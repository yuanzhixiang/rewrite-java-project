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
package org.agrona;

import java.util.concurrent.ThreadLocalRandom;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Miscellaneous useful functions for dealing with low level bits and bytes.
 */
public final class BitUtil {
    /**
     * Size of a byte in bytes
     */
    public static final int SIZE_OF_BYTE = 1;

    /**
     * Size of a boolean in bytes
     */
    public static final int SIZE_OF_BOOLEAN = 1;

    /**
     * Size of a char in bytes
     */
    public static final int SIZE_OF_CHAR = 2;

    /**
     * Size of a short in bytes
     */
    public static final int SIZE_OF_SHORT = 2;

    /**
     * Size of an int in bytes
     */
    public static final int SIZE_OF_INT = 4;

    /**
     * Size of a float in bytes
     */
    public static final int SIZE_OF_FLOAT = 4;

    /**
     * Size of a long in bytes
     */
    public static final int SIZE_OF_LONG = 8;

    /**
     * Size of a double in bytes
     */
    public static final int SIZE_OF_DOUBLE = 8;

    /**
     * Length of the data blocks used by the CPU cache sub-system in bytes.
     */
    public static final int CACHE_LINE_LENGTH = 64;

    private static final byte[] HEX_DIGIT_TABLE = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private static final byte[] FROM_HEX_DIGIT_TABLE;

    static {
        FROM_HEX_DIGIT_TABLE = new byte[128];

        FROM_HEX_DIGIT_TABLE['0'] = 0x00;
        FROM_HEX_DIGIT_TABLE['1'] = 0x01;
        FROM_HEX_DIGIT_TABLE['2'] = 0x02;
        FROM_HEX_DIGIT_TABLE['3'] = 0x03;
        FROM_HEX_DIGIT_TABLE['4'] = 0x04;
        FROM_HEX_DIGIT_TABLE['5'] = 0x05;
        FROM_HEX_DIGIT_TABLE['6'] = 0x06;
        FROM_HEX_DIGIT_TABLE['7'] = 0x07;
        FROM_HEX_DIGIT_TABLE['8'] = 0x08;
        FROM_HEX_DIGIT_TABLE['9'] = 0x09;
        FROM_HEX_DIGIT_TABLE['a'] = 0x0a;
        FROM_HEX_DIGIT_TABLE['A'] = 0x0a;
        FROM_HEX_DIGIT_TABLE['b'] = 0x0b;
        FROM_HEX_DIGIT_TABLE['B'] = 0x0b;
        FROM_HEX_DIGIT_TABLE['c'] = 0x0c;
        FROM_HEX_DIGIT_TABLE['C'] = 0x0c;
        FROM_HEX_DIGIT_TABLE['d'] = 0x0d;
        FROM_HEX_DIGIT_TABLE['D'] = 0x0d;
        FROM_HEX_DIGIT_TABLE['e'] = 0x0e;
        FROM_HEX_DIGIT_TABLE['E'] = 0x0e;
        FROM_HEX_DIGIT_TABLE['f'] = 0x0f;
        FROM_HEX_DIGIT_TABLE['F'] = 0x0f;
    }

    private static final int LAST_DIGIT_MASK = 0b1;

    private BitUtil() {
    }

    /**
     * Fast method of finding the next power of 2 greater than or equal to the supplied value.
     * <p>
     * If the value is &lt;= 0 then 1 will be returned.
     * <p>
     * This method is not suitable for {@link Integer#MIN_VALUE} or numbers greater than 2^30. When provided
     * then {@link Integer#MIN_VALUE} will be returned.
     *
     * @param value from which to search for next power of 2.
     * @return The next power of 2 or the value itself if it is a power of 2.
     */
    public static int findNextPositivePowerOfTwo(final int value) {
        return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(value - 1));
    }

    /**
     * Fast method of finding the next power of 2 greater than or equal to the supplied value.
     * <p>
     * If the value is &lt;= 0 then 1 will be returned.
     * <p>
     * This method is not suitable for {@link Long#MIN_VALUE} or numbers greater than 2^62. When provided
     * then {@link Long#MIN_VALUE} will be returned.
     *
     * @param value from which to search for next power of 2.
     * @return The next power of 2 or the value itself if it is a power of 2.
     */
    public static long findNextPositivePowerOfTwo(final long value) {
        return 1L << (Long.SIZE - Long.numberOfLeadingZeros(value - 1));
    }

    /**
     * Align a value to the next multiple up of alignment.
     * If the value equals an alignment multiple then it is returned unchanged.
     * <p>
     * This method executes without branching. This code is designed to be use in the fast path and should not
     * be used with negative numbers. Negative numbers will result in undefined behaviour.
     *
     * @param value     to be aligned up.
     * @param alignment to be used.
     * @return the value aligned to the next boundary.
     */
    public static int align(final int value, final int alignment) {
        return (value + (alignment - 1)) & -alignment;
    }

    /**
     * Generate a byte array from the hex representation of the given byte array.
     *
     * @param buffer to convert from a hex representation (in Big Endian).
     * @return new byte array that is decimal representation of the passed array.
     */
    public static byte[] fromHexByteArray(final byte[] buffer) {
        final byte[] outputBuffer = new byte[buffer.length >> 1];

        for (int i = 0; i < buffer.length; i += 2) {
            final int hi = FROM_HEX_DIGIT_TABLE[buffer[i]] << 4;
            final int lo = FROM_HEX_DIGIT_TABLE[buffer[i + 1]]; // lgtm[java/index-out-of-bounds]
            outputBuffer[i >> 1] = (byte) (hi | lo);
        }

        return outputBuffer;
    }

    /**
     * Generate a byte array that is a hex representation of a given byte array.
     *
     * @param buffer to convert to a hex representation.
     * @return new byte array that is hex representation (in Big Endian) of the passed array.
     */
    public static byte[] toHexByteArray(final byte[] buffer) {
        return toHexByteArray(buffer, 0, buffer.length);
    }

    /**
     * Generate a byte array that is a hex representation of a given byte array.
     *
     * @param buffer to convert to a hex representation.
     * @param offset the offset into the buffer.
     * @param length the number of bytes to convert.
     * @return new byte array that is hex representation (in Big Endian) of the passed array.
     */
    public static byte[] toHexByteArray(final byte[] buffer, final int offset, final int length) {
        final byte[] outputBuffer = new byte[length << 1];

        for (int i = 0; i < (length << 1); i += 2) {
            final byte b = buffer[offset + (i >> 1)];

            outputBuffer[i] = HEX_DIGIT_TABLE[(b >> 4) & 0x0F];
            outputBuffer[i + 1] = HEX_DIGIT_TABLE[b & 0x0F];
        }

        return outputBuffer;
    }

    /**
     * Generate a byte array that is a hex representation of a given byte array.
     *
     * @param charSequence to convert to a hex representation.
     * @param offset       the offset into the buffer.
     * @param length       the number of bytes to convert.
     * @return new byte array that is hex representation (in Big Endian) of the passed array.
     */
    public static byte[] toHexByteArray(final CharSequence charSequence, final int offset, final int length) {
        final byte[] outputBuffer = new byte[length << 1];

        for (int i = 0; i < (length << 1); i += 2) {
            final byte b = (byte) charSequence.charAt(offset + (i >> 1));

            outputBuffer[i] = HEX_DIGIT_TABLE[(b >> 4) & 0x0F];
            outputBuffer[i + 1] = HEX_DIGIT_TABLE[b & 0x0F];
        }

        return outputBuffer;
    }

    /**
     * Generate a byte array from a string that is the hex representation of the given byte array.
     *
     * @param string to convert from a hex representation (in Big Endian).
     * @return new byte array holding the decimal representation of the passed array.
     */
    public static byte[] fromHex(final String string) {
        final int length = string.length();
        final byte[] bytes = new byte[length];

        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) string.charAt(i);
        }

        return fromHexByteArray(bytes);
    }

    /**
     * Generate a string that is the hex representation of a given byte array.
     *
     * @param buffer to convert to a hex representation.
     * @param offset the offset into the buffer.
     * @param length the number of bytes to convert.
     * @return new String holding the hex representation (in Big Endian) of the passed array.
     */
    public static String toHex(final byte[] buffer, final int offset, final int length) {
        return new String(toHexByteArray(buffer, offset, length), US_ASCII);
    }

    /**
     * Generate a string that is the hex representation of a given byte array.
     *
     * @param buffer to convert to a hex representation.
     * @return new String holding the hex representation (in Big Endian) of the passed array.
     */
    public static String toHex(final byte[] buffer) {
        return new String(toHexByteArray(buffer), US_ASCII);
    }

    /**
     * Is a int value even.
     *
     * @param value to check.
     * @return true if the number is even otherwise false.
     */
    public static boolean isEven(final int value) {
        return (value & LAST_DIGIT_MASK) == 0;
    }

    /**
     * Is a long value even.
     *
     * @param value to check.
     * @return true if the number is even otherwise false.
     */
    public static boolean isEven(final long value) {
        return (value & LAST_DIGIT_MASK) == 0;
    }

    /**
     * Is a value a positive power of 2.
     *
     * @param value to be checked.
     * @return true if the number is a positive power of 2, otherwise false.
     */
    public static boolean isPowerOfTwo(final int value) {
        return value > 0 && ((value & (~value + 1)) == value);
    }

    /**
     * Is a value a positive power of 2.
     *
     * @param value to be checked.
     * @return true if the number is a positive power of 2, otherwise false.
     */
    public static boolean isPowerOfTwo(final long value) {
        return value > 0 && ((value & (~value + 1)) == value);
    }

    /**
     * Cycles indices of an array one at a time in a forward fashion.
     *
     * @param current value to be incremented.
     * @param max     value for the cycle.
     * @return the next value, or zero if max is reached.
     */
    public static int next(final int current, final int max) {
        int next = current + 1;
        if (next == max) {
            next = 0;
        }

        return next;
    }

    /**
     * Cycles indices of an array one at a time in a backwards fashion.
     *
     * @param current value to be decremented.
     * @param max     value of the cycle.
     * @return the next value, or max - 1 if current is zero.
     */
    public static int previous(final int current, final int max) {
        if (0 == current) {
            return max - 1;
        }

        return current - 1;
    }

    /**
     * Calculate the shift value to scale a number based on how refs are compressed or not.
     *
     * @param scale of the number reported by Unsafe.
     * @return how many times the number needs to be shifted to the left.
     */
    public static int calculateShiftForScale(final int scale) {
        if (4 == scale) {
            return 2;
        } else if (8 == scale) {
            return 3;
        }

        throw new IllegalArgumentException("unknown pointer size for scale=" + scale);
    }

    /**
     * Generate a randomised integer over [{@link Integer#MIN_VALUE}, {@link Integer#MAX_VALUE}].
     *
     * @return randomised integer suitable as an Id.
     */
    public static int generateRandomisedId() {
        return ThreadLocalRandom.current().nextInt();
    }

    /**
     * Is an address aligned on a boundary.
     *
     * @param address   to be tested.
     * @param alignment boundary the address is tested against.
     * @return true if the address is on the aligned boundary otherwise false.
     * @throws IllegalArgumentException if the alignment is not a power of 2.
     */
    public static boolean isAligned(final long address, final int alignment) {
        if (!BitUtil.isPowerOfTwo(alignment)) {
            throw new IllegalArgumentException("alignment must be a power of 2: alignment=" + alignment);
        }

        return (address & (alignment - 1)) == 0;
    }
}
