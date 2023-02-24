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

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Obtain access the {@link Unsafe} class for direct memory operations.
 */
public final class UnsafeAccess {
    /**
     * Reference to the {@link Unsafe} instance.
     */
    public static final Unsafe UNSAFE;

    /**
     * Byte array base offset.
     */
    public static final int ARRAY_BYTE_BASE_OFFSET;

    /**
     * Indicates that a special sequence of instructions must be used instead of simply calling
     * {@link Unsafe#setMemory(Object, long, long, byte)} in order to trick the JIT into calling the {@code memset}
     * function.
     */
    public static final boolean MEMSET_HACK_REQUIRED;

    /**
     * A minimal size in bytes for the {@link Unsafe#setMemory(Object, long, long, byte)} after which JIT could decide
     * to use the {@code memset}.
     *
     * @see #MEMSET_HACK_REQUIRED
     */
    public static final int MEMSET_HACK_THRESHOLD;

    static {
        Unsafe unsafe = null;
        try {
            unsafe = Unsafe.getUnsafe();
        } catch (final Exception ex) {
            try {
                final Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);

                unsafe = (Unsafe) f.get(null);
            } catch (final Exception ex2) {
                LangUtil.rethrowUnchecked(ex);
            }
        }

        UNSAFE = unsafe;
        ARRAY_BYTE_BASE_OFFSET = unsafe.arrayBaseOffset(byte[].class);

        boolean memsetHackRequired;
        try {
            Class.forName("java.lang.Runtime$Version"); // since JDK 9
            memsetHackRequired = false;
        } catch (final ClassNotFoundException ex) {
            memsetHackRequired = true;
        }
        MEMSET_HACK_REQUIRED = memsetHackRequired;
        MEMSET_HACK_THRESHOLD = 64;
    }

    private UnsafeAccess() {
    }
}
