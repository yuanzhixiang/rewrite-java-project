/*
 * Copyright 2014-2022 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.jupiter.api.Assertions.*;

class Int2ObjectHashMapTest {
    final Int2ObjectHashMap<String> intToObjectMap;

    Int2ObjectHashMapTest() {
        intToObjectMap = newMap(Hashing.DEFAULT_LOAD_FACTOR, Int2ObjectHashMap.MIN_CAPACITY);
    }

    Int2ObjectHashMap<String> newMap(final float loadFactor, final int initialCapacity) {
        return new Int2ObjectHashMap<>(initialCapacity, loadFactor);
    }

    @Test
    void shouldDoPutAndThenGet() {
        final String value = "Seven";
        intToObjectMap.put(7, value);

        assertThat(intToObjectMap.get(7), is(value));
    }

    @Test
    void shouldReplaceExistingValueForTheSameKey() {
        final int key = 7;
        final String value = "Seven";
        intToObjectMap.put(key, value);

        final String newValue = "New Seven";
        final String oldValue = intToObjectMap.put(key, newValue);

        assertThat(intToObjectMap.get(key), is(newValue));
        assertThat(oldValue, is(value));
        assertThat(intToObjectMap.size(), is(1));
    }

    @Test
    void shouldGrowWhenThresholdExceeded() {
        final float loadFactor = 0.5f;
        final int initialCapacity = 32;
        final Int2ObjectHashMap<String> map = newMap(loadFactor, initialCapacity);
        for (int i = 0; i < 16; i++) {
            map.put(i, Integer.toString(i));
        }

        assertThat(map.resizeThreshold(), is(16));
        assertThat(map.capacity(), is(initialCapacity));
        assertThat(map.size(), is(16));

        map.put(16, "16");

        assertThat(map.resizeThreshold(), is(initialCapacity));
        assertThat(map.capacity(), is(64));
        assertThat(map.size(), is(17));

        assertThat(map.get(16), equalTo("16"));
        assertThat((double) loadFactor, closeTo(map.loadFactor(), 0.0f));
    }

    @Test
    void shouldHandleCollisionAndThenLinearProbe() {
        final float loadFactor = 0.5f;
        final int initialCapacity = 32;
        final Int2ObjectHashMap<String> map = newMap(loadFactor, initialCapacity);
        final int key = 7;
        final String value = "Seven";
        map.put(key, value);

        final int collisionKey = key + map.capacity();
        final String collisionValue = Integer.toString(collisionKey);
        map.put(collisionKey, collisionValue);

        assertThat(map.get(key), is(value));
        assertThat(map.get(collisionKey), is(collisionValue));
        assertThat((double) loadFactor, closeTo(map.loadFactor(), 0.0f));
    }

    @Test
    void shouldClearCollection() {
        for (int i = 0; i < 15; i++) {
            intToObjectMap.put(i, Integer.toString(i));
        }

        assertThat(intToObjectMap.size(), is(15));
        assertThat(intToObjectMap.get(1), is("1"));

        intToObjectMap.clear();

        assertThat(intToObjectMap.size(), is(0));
        assertNull(intToObjectMap.get(1));
    }

    @Test
    void shouldCompactCollection() {
        final int totalItems = 50;
        for (int i = 0; i < totalItems; i++) {
            intToObjectMap.put(i, Integer.toString(i));
        }

        for (int i = 0, limit = totalItems - 4; i < limit; i++) {
            intToObjectMap.remove(i);
        }

        final int capacityBeforeCompaction = intToObjectMap.capacity();
        intToObjectMap.compact();

        assertThat(intToObjectMap.capacity(), lessThan(capacityBeforeCompaction));
    }

    @Test
    void shouldCompute() {
        final int testKey = 7;
        final String testValue = "Seven";
        final String testValue2 = "7";

        assertThat(intToObjectMap.get(testKey), nullValue());
        assertThat(intToObjectMap.compute(testKey, (k, v) -> testValue), is(testValue));
        assertThat(intToObjectMap.get(testKey), is(testValue));

        assertThat(intToObjectMap.compute(testKey, (k, v) -> testValue2), is(testValue2));
        assertThat(intToObjectMap.get(testKey), is(testValue2));
    }

    @Test
    void shouldComputeBoxed() {
        final Map<Integer, String> intToObjectMap = this.intToObjectMap;

        final int testKey = 7;
        final String testValue = "Seven";
        final String testValue2 = "7";

        assertThat(intToObjectMap.compute(testKey, (k, v) -> testValue), is(testValue));
        assertThat(intToObjectMap.get(testKey), is(testValue));

        assertThat(intToObjectMap.compute(testKey, (k, v) -> testValue2), is(testValue2));
        assertThat(intToObjectMap.get(testKey), is(testValue2));
    }

    @Test
    void shouldComputeIfAbsent() {
        final int testKey = 7;
        final String testValue = "Seven";
        final String testValue2 = "7";

        assertThat(intToObjectMap.get(testKey), nullValue());

        assertThat(intToObjectMap.computeIfAbsent(testKey, (i) -> testValue), is(testValue));
        assertThat(intToObjectMap.get(testKey), is(testValue));

        assertThat(intToObjectMap.computeIfAbsent(testKey, (i) -> testValue2), is(testValue));
        assertThat(intToObjectMap.get(testKey), is(testValue));
    }

    @Test
    void shouldComputeIfAbsentBoxed() {
        final Map<Integer, String> intToObjectMap = this.intToObjectMap;

        final int testKey = 7;
        final String testValue = "Seven";
        final String testValue2 = "7";

        assertThat(intToObjectMap.computeIfAbsent(testKey, (i) -> testValue), is(testValue));
        assertThat(intToObjectMap.get(testKey), is(testValue));

        assertThat(intToObjectMap.computeIfAbsent(testKey, (i) -> testValue2), is(testValue));
        assertThat(intToObjectMap.get(testKey), is(testValue));
    }

    @Test
    void shouldComputeIfPresent() {
        final int testKey = 7;
        final String testValue = "Seven";
        final String testValue2 = "7";

        assertThat(intToObjectMap.computeIfPresent(testKey, (k, v) -> testValue), nullValue());
        assertThat(intToObjectMap.get(testKey), nullValue());

        intToObjectMap.put(testKey, testValue);
        assertThat(intToObjectMap.computeIfPresent(testKey, (k, v) -> testValue2), is(testValue2));
        assertThat(intToObjectMap.get(testKey), is(testValue2));
    }

    @Test
    void computeIfPresentShouldDeleteExistingEntryIfFunctionReturnsNull() {
        intToObjectMap.put(1, "one");
        final int key = 3;
        final String value = "three";
        intToObjectMap.put(key, value);
        final IntObjectToObjectFunction<String, String> function = (k, v) ->
        {
            assertEquals(key, k);
            assertEquals(value, v);
            return null;
        };

        assertNull(intToObjectMap.computeIfPresent(key, function));

        assertEquals(1, intToObjectMap.size());
        assertEquals("one", intToObjectMap.get(1));
        assertFalse(intToObjectMap.containsKey(key));
        assertFalse(intToObjectMap.containsValue(value));
    }

    @Test
    void shouldComputeIfPresentBoxed() {
        final Map<Integer, String> intToObjectMap = this.intToObjectMap;

        final int testKey = 7;
        final String testValue = "Seven";
        final String testValue2 = "7";

        assertThat(intToObjectMap.computeIfPresent(testKey, (k, v) -> testValue), nullValue());
        assertThat(intToObjectMap.get(testKey), nullValue());

        intToObjectMap.put(testKey, testValue);
        assertThat(intToObjectMap.computeIfPresent(testKey, (k, v) -> testValue2), is(testValue2));
        assertThat(intToObjectMap.get(testKey), is(testValue2));
    }

    @Test
    void shouldContainValue() {
        final int key = 7;
        final String value = "Seven";

        intToObjectMap.put(key, value);

        assertTrue(intToObjectMap.containsValue(value));
        assertFalse(intToObjectMap.containsValue("NoKey"));
    }

    @Test
    void shouldContainKey() {
        final int key = 7;
        final String value = "Seven";

        intToObjectMap.put(key, value);

        assertTrue(intToObjectMap.containsKey(key));
        assertFalse(intToObjectMap.containsKey(0));
    }

    @Test
    void shouldRemoveEntry() {
        final int key = 7;
        final String value = "Seven";

        intToObjectMap.put(key, value);

        assertTrue(intToObjectMap.containsKey(key));

        intToObjectMap.remove(key);

        assertFalse(intToObjectMap.containsKey(key));
    }

    @Test
    void shouldRemoveEntryAndCompactCollisionChain() {
        final int key = 12;
        final String value = "12";

        intToObjectMap.put(key, value);
        intToObjectMap.put(13, "13");

        final int collisionKey = key + intToObjectMap.capacity();
        final String collisionValue = Integer.toString(collisionKey);

        intToObjectMap.put(collisionKey, collisionValue);
        intToObjectMap.put(14, "14");

        assertThat(intToObjectMap.remove(key), is(value));
    }

    @Test
    void shouldIterateValues() {
        final Collection<String> initialSet = new ArrayList<>();

        for (int i = 0; i < 11; i++) {
            final String value = Integer.toString(i);
            intToObjectMap.put(i, value);
            initialSet.add(value);
        }

        final Collection<String> copyToSetOne = new ArrayList<>();
        for (final String s : intToObjectMap.values()) {
            //noinspection UseBulkOperation
            copyToSetOne.add(s);
        }

        final Collection<String> copyToSetTwo = new ArrayList<>();
        for (final String s : intToObjectMap.values()) {
            //noinspection UseBulkOperation
            copyToSetTwo.add(s);
        }

        assertEquals(initialSet.size(), copyToSetOne.size());
        assertTrue(initialSet.containsAll(copyToSetOne));

        assertEquals(initialSet.size(), copyToSetTwo.size());
        assertTrue(initialSet.containsAll(copyToSetTwo));
    }

    @Test
    void shouldForEachValues() {
        final Collection<String> expected = new HashSet<>();
        for (int i = 0; i < 11; i++) {
            final String value = Integer.toString(i);
            intToObjectMap.put(i, value);
            expected.add(value);
        }

        final Collection<String> copySet = new HashSet<>();
        //noinspection UseBulkOperation
        intToObjectMap.values().forEach(copySet::add);

        assertEquals(expected, copySet);
    }

    @Test
    void shouldIterateKeysGettingIntAsPrimitive() {
        final Collection<Integer> initialSet = new HashSet<>();

        for (int i = 0; i < 11; i++) {
            final String value = Integer.toString(i);
            intToObjectMap.put(i, value);
            initialSet.add(i);
        }

        final Collection<Integer> copyToSet = new HashSet<>();

        for (final Int2ObjectHashMap<String>.KeyIterator iter = intToObjectMap.keySet().iterator(); iter.hasNext(); ) {
            copyToSet.add(iter.nextInt());
        }

        assertEquals(initialSet, copyToSet);
    }

    @Test
    void shouldIterateKeys() {
        final Collection<Integer> initialSet = new HashSet<>();

        for (int i = 0; i < 11; i++) {
            final String value = Integer.toString(i);
            intToObjectMap.put(i, value);
            initialSet.add(i);
        }

        assertIterateKeys(initialSet);
        assertIterateKeys(initialSet);
        assertIterateKeys(initialSet);
    }

    private void assertIterateKeys(final Collection<Integer> initialSet) {
        final Collection<Integer> copyToSet = new HashSet<>();
        for (final Integer aInteger : intToObjectMap.keySet()) {
            //noinspection UseBulkOperation
            copyToSet.add(aInteger);
        }

        assertEquals(initialSet, copyToSet);
    }

    @Test
    void shouldIterateAndHandleRemove() {
        final Collection<Integer> initialSet = new HashSet<>();

        final int count = 11;
        for (int i = 0; i < count; i++) {
            final String value = Integer.toString(i);
            intToObjectMap.put(i, value);
            initialSet.add(i);
        }

        final Collection<Integer> copyOfSet = new HashSet<>();

        int i = 0;
        for (final Iterator<Integer> iter = intToObjectMap.keySet().iterator(); iter.hasNext(); ) {
            final Integer item = iter.next();
            if (i++ == 7) {
                iter.remove();
            } else {
                copyOfSet.add(item);
            }
        }

        final int reducedSetSize = count - 1;
        assertEquals(count, initialSet.size());
        assertEquals(reducedSetSize, intToObjectMap.size());
        assertEquals(reducedSetSize, copyOfSet.size());
    }

    @Test
    void shouldIterateEntries() {
        final int count = 11;
        for (int i = 0; i < count; i++) {
            final String value = Integer.toString(i);
            intToObjectMap.put(i, value);
        }

        iterateEntries();
        iterateEntries();
        iterateEntries();

        final String testValue = "Wibble";
        for (final Map.Entry<Integer, String> entry : intToObjectMap.entrySet()) {
            assertThat(String.valueOf(entry.getKey()), equalTo(entry.getValue()));

            if (entry.getKey() == 7) {
                entry.setValue(testValue);
            }
        }

        assertEquals(testValue, intToObjectMap.get(7));
    }

    private void iterateEntries() {
        for (final Map.Entry<Integer, String> entry : intToObjectMap.entrySet()) {
            assertThat(String.valueOf(entry.getKey()), equalTo(entry.getValue()));
        }
    }

    @Test
    void shouldIterateForEach() {
        final int count = 11;
        for (int i = 0; i < count; i++) {
            final String value = Integer.toString(i);
            intToObjectMap.put(i, value);
        }

        final Collection<Integer> copyToSet = new HashSet<>();
        intToObjectMap.forEachInt(
                (key, value) ->
                {
                    assertEquals(value, String.valueOf(key));

                    // not copying values, because they match keys
                    copyToSet.add(key);
                });
        assertEquals(copyToSet, intToObjectMap.keySet());
    }

    @Test
    void shouldGenerateStringRepresentation() {
        final int[] testEntries = {3, 1, 19, 7, 11, 12, 7};

        for (final int testEntry : testEntries) {
            intToObjectMap.put(testEntry, String.valueOf(testEntry));
        }

        final String mapAsAString = "{19=19, 1=1, 11=11, 7=7, 3=3, 12=12}";
        assertThat(intToObjectMap.toString(), equalTo(mapAsAString));
    }

    @Test
    void shouldCopyConstructAndBeEqual() {
        final int[] testEntries = {3, 1, 19, 7, 11, 12, 7};

        for (final int testEntry : testEntries) {
            intToObjectMap.put(testEntry, String.valueOf(testEntry));
        }

        final Int2ObjectHashMap<String> mapCopy = new Int2ObjectHashMap<>(intToObjectMap);
        assertThat(mapCopy, is(intToObjectMap));
    }

    @Test
    void shouldAllowNullValuesWithNullMapping() {
        final Int2ObjectHashMap<String> map = new Int2ObjectHashMap<String>() {
            private final Object nullRef = new Object();

            protected Object mapNullValue(final Object value) {
                return value == null ? nullRef : value;
            }

            protected String unmapNullValue(final Object value) {
                return value == nullRef ? null : (String) value;
            }
        };

        map.put(0, null);
        map.put(1, "one");

        assertThat(map.get(0), nullValue());
        assertThat(map.get(1), is("one"));
        assertThat(map.get(-1), nullValue());

        assertThat(map.containsKey(0), is(true));
        assertThat(map.containsKey(1), is(true));
        assertThat(map.containsKey(-1), is(false));

        assertThat(map.values(), containsInAnyOrder(null, "one"));
        assertThat(map.keySet(), containsInAnyOrder(0, 1));

        assertThat(map.size(), is(2));

        map.remove(0);

        assertThat(map.get(0), nullValue());
        assertThat(map.get(1), is("one"));
        assertThat(map.get(-1), nullValue());

        assertThat(map.containsKey(0), is(false));
        assertThat(map.containsKey(1), is(true));
        assertThat(map.containsKey(-1), is(false));

        assertThat(map.size(), is(1));
    }

    @Test
    void shouldToArray() {
        final Int2ObjectHashMap<String> map = new Int2ObjectHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        final Object[] array = map.entrySet().toArray();
        for (final Object entry : array) {
            map.remove(((Map.Entry<?, ?>) entry).getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void shouldToArrayTyped() {
        final Int2ObjectHashMap<String> map = new Int2ObjectHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        final Map.Entry<?, ?>[] type = new Map.Entry[1];
        final Map.Entry<?, ?>[] array = map.entrySet().toArray(type);
        for (final Map.Entry<?, ?> entry : array) {
            map.remove(entry.getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    void shouldToArrayWithArrayListConstructor() {
        final Int2ObjectHashMap<String> map = new Int2ObjectHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        final List<Map.Entry<Integer, String>> list = new ArrayList<>(map.entrySet());
        for (final Map.Entry<Integer, String> entry : list) {
            map.remove(entry.getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    void containsValueShouldPerformEqualityCheckBasedOnTheValueStoredInTheMap() {
        final Int2ObjectHashMap<CharSequence> map = new Int2ObjectHashMap<>();
        map.put(11, new CharSequenceKey("abc"));
        final CharSequenceKey xyzKey = new CharSequenceKey("xyz");
        map.put(42, xyzKey);

        assertTrue(map.containsValue("abc"));
        assertTrue(map.containsValue(new CharSequenceKey("abc")));
        assertTrue(map.containsValue(xyzKey));
        assertTrue(map.containsValue(new CharSequenceKey("xyz")));

        final Int2ObjectHashMap<CharSequence>.ValueCollection values = map.values();
        assertTrue(values.contains("abc"));
        assertTrue(values.contains(new CharSequenceKey("abc")));
        assertTrue(values.contains(xyzKey));

        assertFalse(map.containsValue(null));
        assertFalse(map.containsValue("null"));
        assertFalse(map.containsValue(new CharSequenceKey("test")));
    }

    @Test
    void getOrDefaultShouldReturnDefaultValueIfNoMappingExistsForAGivenKey() {
        final int key = 121;
        final String defaultValue = "fallback";

        assertEquals(defaultValue, intToObjectMap.getOrDefault(key, defaultValue));
    }

    @Test
    void getOrDefaultShouldReturnValueForAnExistingKey() {
        final int key = 121;
        final String value = "found";
        final String defaultValue = "fallback";
        intToObjectMap.put(key, value);

        assertEquals(value, intToObjectMap.getOrDefault(key, defaultValue));
    }

    @Test
    void removeIsANoOpIfValueIsNull() {
        final int key = 42;
        final String value = "nine";
        intToObjectMap.put(key, value);

        assertFalse(intToObjectMap.remove(key, null));

        assertEquals(value, intToObjectMap.get(key));
    }

    @Test
    void removeIsANoOpIfValueDoesNotMatch() {
        final int key = 42;
        final String value = "nine";
        intToObjectMap.put(key, value);

        assertFalse(intToObjectMap.remove(key, "ten"));

        assertEquals(value, intToObjectMap.get(key));
    }

    @Test
    void removeShouldDeleteKeyMappingIfValueMatches() {
        final int key = -100;
        final Int2ObjectHashMap<CharSequence> map = new Int2ObjectHashMap<>();
        map.put(key, new CharSequenceKey("abc"));
        map.put(2, "two");

        assertTrue(map.remove(key, "abc"));

        assertEquals(1, map.size());
        assertEquals("two", map.get(2));
    }

    @Test
    void replaceThrowsNullPointerExceptionIfValueIsNull() {
        final NullPointerException exception =
                assertThrowsExactly(NullPointerException.class, () -> intToObjectMap.replace(42, null));
        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    void replaceReturnsNullForAnUnknownKey() {
        intToObjectMap.put(1, "one");

        assertNull(intToObjectMap.replace(2, "three"));

        assertEquals("one", intToObjectMap.get(1));
    }

    @Test
    void replaceReturnsPreviousValueAfterSettingTheNewOne() {
        final int key = 1;
        final String oldValue = "one";
        final String newValue = "three";
        intToObjectMap.put(key, oldValue);

        assertEquals(oldValue, intToObjectMap.replace(key, newValue));

        assertEquals(newValue, intToObjectMap.get(key));
    }

    @Test
    void replaceThrowsNullPointerExceptionIfNewValueIsNull() {
        final NullPointerException exception =
                assertThrowsExactly(NullPointerException.class, () -> intToObjectMap.replace(42, "abc", null));
        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    void replaceReturnsFalseForAnUnknownKey() {
        intToObjectMap.put(1, "one");

        assertFalse(intToObjectMap.replace(2, "a", "b"));

        assertEquals("one", intToObjectMap.get(1));
    }

    @Test
    void replaceReturnsFalseIfTheOldValueDoesNotMatch() {
        final int key = 1;
        final String value = "one";
        intToObjectMap.put(key, value);

        assertFalse(intToObjectMap.replace(key, "wrong!", "new one"));

        assertEquals(value, intToObjectMap.get(key));
    }

    @Test
    void replaceReturnsTrueAfterUpdatingTheNewValue() {
        final Int2ObjectHashMap<CharSequence> map = new Int2ObjectHashMap<>();
        final int key = 1;
        final String newValue = "two";
        map.put(key, new CharSequenceKey("one"));

        assertTrue(map.replace(key, "one", newValue));

        assertEquals(newValue, map.get(key));
    }

    @Test
    void replaceAllIntThrowsNullPointerExceptionIfTheNewValueIsNull() {
        final IntObjectToObjectFunction<String, String> nullFunction = (key, value) -> null;
        intToObjectMap.put(1, "one");

        final NullPointerException exception =
                assertThrowsExactly(NullPointerException.class, () -> intToObjectMap.replaceAllInt(nullFunction));
        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    void replaceAllIntUpdatesEveryExistingValue() {
        final IntObjectToObjectFunction<String, String> updateFunction = (key, value) -> key + "_" + value;
        intToObjectMap.put(1, "one");
        intToObjectMap.put(2, "two");
        intToObjectMap.put(-100, "null");

        intToObjectMap.replaceAllInt(updateFunction);

        assertEquals(3, intToObjectMap.size());
        assertEquals("1_one", intToObjectMap.get(1));
        assertEquals("2_two", intToObjectMap.get(2));
        assertEquals("-100_null", intToObjectMap.get(-100));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "val 1", "你好"})
    void putIfAbsentShouldReturnAnExistingValueForAnExistingKey(final String value) {
        final int key = 42;
        final String newValue = " this is something new";
        intToObjectMap.put(key, value);

        assertEquals(value, intToObjectMap.putIfAbsent(key, newValue));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "val 1", "你好"})
    void putIfAbsentShouldReturnNullAfterPuttingANewValue(final String newValue) {
        final int key = 42;
        intToObjectMap.put(3, "three");

        assertNull(intToObjectMap.putIfAbsent(key, newValue));

        assertEquals(newValue, intToObjectMap.get(key));
        assertEquals("three", intToObjectMap.get(3));
    }

    @Test
    void putIfAbsentThrowsNullPointerExceptionIfValueIsNull() {
        final NullPointerException exception =
                assertThrowsExactly(NullPointerException.class, () -> intToObjectMap.putIfAbsent(42, null));
        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    void putAllCopiesAllOfTheValuesFromTheSourceMap() {
        intToObjectMap.put(42, "forty two");
        intToObjectMap.put(0, "zero");
        final Int2ObjectHashMap<String> otherMap = new Int2ObjectHashMap<>();
        otherMap.put(1, "1");
        otherMap.put(2, "2");
        otherMap.put(3, "3");
        otherMap.put(42, "42");

        intToObjectMap.putAll(otherMap);

        assertEquals(5, intToObjectMap.size());
        assertEquals("zero", intToObjectMap.get(0));
        assertEquals("1", intToObjectMap.get(1));
        assertEquals("2", intToObjectMap.get(2));
        assertEquals("3", intToObjectMap.get(3));
        assertEquals("42", intToObjectMap.get(42));
    }

    @Test
    void putAllThrowsNullPointerExceptionIfOtherMapContainsNull() {
        intToObjectMap.put(0, "zero");
        final Int2NullableObjectHashMap<String> otherMap = new Int2NullableObjectHashMap<>();
        otherMap.put(1, null);

        final NullPointerException exception =
                assertThrowsExactly(NullPointerException.class, () -> intToObjectMap.putAll(otherMap));
        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    void removeIfIntOnKeySet() {
        final IntPredicate filter = (key) -> (key & 1) == 0;
        intToObjectMap.put(1, "one");
        intToObjectMap.put(2, "two");
        intToObjectMap.put(3, "three");

        assertTrue(intToObjectMap.keySet().removeIfInt(filter));

        assertEquals(2, intToObjectMap.size());
        assertEquals("one", intToObjectMap.get(1));
        assertEquals("three", intToObjectMap.get(3));

        assertFalse(intToObjectMap.keySet().removeIfInt(filter));
        assertEquals(2, intToObjectMap.size());
    }

    @Test
    void removeIfOnValuesCollection() {
        final Predicate<String> filter = (value) -> value.contains("e");
        intToObjectMap.put(1, "one");
        intToObjectMap.put(2, "two");
        intToObjectMap.put(3, "three");

        assertTrue(intToObjectMap.values().removeIf(filter));

        assertEquals(1, intToObjectMap.size());
        assertEquals("two", intToObjectMap.get(2));

        assertFalse(intToObjectMap.values().removeIf(filter));
        assertEquals(1, intToObjectMap.size());
    }

    @Test
    void removeIfIntOnEntrySet() {
        final IntObjPredicate<String> filter = (key, value) -> (key & 1) == 0 && value.startsWith("t");
        intToObjectMap.put(1, "one");
        intToObjectMap.put(2, "two");
        intToObjectMap.put(3, "three");
        intToObjectMap.put(4, "four");

        assertTrue(intToObjectMap.entrySet().removeIfInt(filter));

        assertEquals(3, intToObjectMap.size());
        assertEquals("one", intToObjectMap.get(1));
        assertEquals("three", intToObjectMap.get(3));
        assertEquals("four", intToObjectMap.get(4));

        assertFalse(intToObjectMap.entrySet().removeIfInt(filter));
        assertEquals(3, intToObjectMap.size());
    }
}
