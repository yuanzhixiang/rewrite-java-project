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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Utilities for inspecting the system.
 */
public final class SystemUtil {
    /**
     * PID value if a process id could not be determined. This value should be equal to a kernel only process
     * id for the platform so that it does not indicate a real process id.
     */
    public static final long PID_NOT_FOUND = 0;

    /**
     * Value a {@link System#getProperties()} can be set to so that {@code null} will be returned as if the property
     * was not set.
     *
     * @see #getProperty(String)
     */
    public static final String NULL_PROPERTY_VALUE = "@null";

    private static final String SUN_PID_PROP_NAME = "sun.java.launcher.pid";
    private static final long MAX_G_VALUE = 8589934591L;
    private static final long MAX_M_VALUE = 8796093022207L;
    private static final long MAX_K_VALUE = 9007199254739968L;

    private static final String OS_NAME;
    private static final String OS_ARCH;
    private static final long PID;

    static {
        OS_NAME = System.getProperty("os.name").toLowerCase();
        OS_ARCH = System.getProperty("os.arch", "unknown");

        long pid = PID_NOT_FOUND;
        try {
            final Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
            final Method currentMethod = processHandleClass.getMethod("current");
            final Object processHandle = currentMethod.invoke(null);
            final Method pidMethod = processHandleClass.getMethod("pid");
            pid = (Long) pidMethod.invoke(processHandle);
        } catch (final Throwable ignore) {
            try {
                final String pidPropertyValue = System.getProperty(SUN_PID_PROP_NAME);
                if (null != pidPropertyValue) {
                    pid = Long.parseLong(pidPropertyValue);
                } else {
                    final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                    pid = Long.parseLong(jvmName.split("@")[0]);
                }
            } catch (final Throwable ignore2) {
            }
        }

        PID = pid;
    }

    private SystemUtil() {
    }

    /**
     * Get the name of the operating system as a lower case String.
     * <p>
     * This is what is returned from {@code System.getProperty("os.name").toLowerCase()}.
     *
     * @return the name of the operating system as a lower case String.
     */
    public static String osName() {
        return OS_NAME;
    }

    /**
     * Returns the name of the operating system architecture.
     * <p>
     * This is the same a calling the {@code System.getProperty("os.arch", "unknown")}.
     *
     * @return name of the operating system architecture or {@code unknown}.
     */
    public static String osArch() {
        return OS_ARCH;
    }

    /**
     * Return the current process id from the OS.
     *
     * @return current process id or {@link #PID_NOT_FOUND} if PID was not able to be found.
     * @see #PID_NOT_FOUND
     */
    public static long getPid() {
        return PID;
    }

    /**
     * Is the operating system likely to be Windows based on {@link #osName()}.
     *
     * @return {@code true} if the operating system is likely to be Windows based on {@link #osName()}.
     */
    public static boolean isWindows() {
        return OS_NAME.startsWith("win");
    }

    /**
     * Is the operating system likely to be Linux based on {@link #osName()}.
     *
     * @return {@code true} if the operating system is likely to be Linux based on {@link #osName()}.
     */
    public static boolean isLinux() {
        return OS_NAME.contains("linux");
    }

    /**
     * Is the operating system architecture ({@link #osArch()}) represents an x86-based system.
     *
     * @return {@code true} if the operating system architecture represents an x86-based system.
     */
    public static boolean isX64Arch() {
        return isX64Arch(OS_ARCH);
    }

    /**
     * Is a debugger attached to the JVM?
     *
     * @return {@code true} if attached otherwise false.
     */
    public static boolean isDebuggerAttached() {
        final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

        for (final String arg : runtimeMXBean.getInputArguments()) {
            if (arg.contains("-agentlib:jdwp")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return the system property for java.io.tmpdir ensuring a {@link File#separator} is at the end.
     *
     * @return tmp directory for the runtime.
     */
    public static String tmpDirName() {
        String tmpDirName = System.getProperty("java.io.tmpdir");
        if (!tmpDirName.endsWith(File.separator)) {
            tmpDirName += File.separator;
        }

        return tmpDirName;
    }

    /**
     * Get a formatted dump of all threads with associated state and stack traces.
     *
     * @return a formatted dump of all threads with associated state and stack traces.
     */
    public static String threadDump() {
        final StringBuilder sb = new StringBuilder();
        threadDump(sb);

        return sb.toString();
    }

    /**
     * Write a formatted dump of all threads with associated state and stack traces to a provided {@link StringBuilder}.
     *
     * @param sb to write the thread dump to.
     */
    public static void threadDump(final StringBuilder sb) {
        final ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();

        for (final ThreadInfo threadInfo : mxBean.getThreadInfo(mxBean.getAllThreadIds(), Integer.MAX_VALUE)) {
            if (null != threadInfo) {
                sb.append('"').append(threadInfo.getThreadName()).append("\": ").append(threadInfo.getThreadState());

                for (final StackTraceElement stackTraceElement : threadInfo.getStackTrace()) {
                    sb.append("\n    at ").append(stackTraceElement.toString());
                }

                sb.append("\n\n");
            }
        }
    }

    /**
     * Load system properties from a given filename or url with default to {@link PropertyAction#REPLACE}.
     * <p>
     * File is first searched for in resources using the system {@link ClassLoader},
     * then file system, then URL. All are loaded if multiples found.
     *
     * @param filenameOrUrl that holds properties.
     */
    public static void loadPropertiesFile(final String filenameOrUrl) {
        loadPropertiesFile(PropertyAction.REPLACE, filenameOrUrl);
    }

    /**
     * Load system properties from a given filename or url.
     * <p>
     * File is first searched for in resources using the system {@link ClassLoader},
     * then file system, then URL. All are loaded if multiples found.
     *
     * @param propertyAction to take with each loaded property.
     * @param filenameOrUrl  that holds properties.
     */
    public static void loadPropertiesFile(final PropertyAction propertyAction, final String filenameOrUrl) {
        final URL resource = ClassLoader.getSystemClassLoader().getResource(filenameOrUrl);
        if (null != resource) {
            try (InputStream in = resource.openStream()) {
                loadProperties(propertyAction, in);
            } catch (final Exception ignore) {
            }
        }

        final File file = new File(filenameOrUrl);
        if (file.exists()) {
            try (InputStream in = Files.newInputStream(file.toPath())) {
                loadProperties(propertyAction, in);
            } catch (final Exception ignore) {
            }
        }

        try (InputStream in = new URL(filenameOrUrl).openStream()) {
            loadProperties(propertyAction, in);
        } catch (final Exception ignore) {
        }
    }

    /**
     * Load system properties from a given set of filenames or URLs with default to {@link PropertyAction#REPLACE}.
     *
     * @param filenamesOrUrls that holds properties.
     * @see #loadPropertiesFile(String)
     */
    public static void loadPropertiesFiles(final String... filenamesOrUrls) {
        loadPropertiesFiles(PropertyAction.REPLACE, filenamesOrUrls);
    }

    /**
     * Load system properties from a given set of filenames or URLs.
     *
     * @param propertyAction  to take with each loaded property.
     * @param filenamesOrUrls that holds properties.
     * @see #loadPropertiesFile(String)
     */
    public static void loadPropertiesFiles(final PropertyAction propertyAction, final String... filenamesOrUrls) {
        for (final String filenameOrUrl : filenamesOrUrls) {
            loadPropertiesFile(propertyAction, filenameOrUrl);
        }
    }

    /**
     * Get the value of a {@link System#getProperty(String)} with the exception that if the value is
     * {@link #NULL_PROPERTY_VALUE} then return {@code null}.
     *
     * @param propertyName to get the value for.
     * @return the value of a {@link System#getProperty(String)} with the exception that if the value is
     * {@link #NULL_PROPERTY_VALUE} then return {@code null}.
     */
    public static String getProperty(final String propertyName) {
        final String propertyValue = System.getProperty(propertyName);

        return NULL_PROPERTY_VALUE.equals(propertyValue) ? null : propertyValue;
    }

    /**
     * Get the value of a {@link System#getProperty(String, String)} with the exception that if the value is
     * {@link #NULL_PROPERTY_VALUE} then return {@code null}, otherwise if the value is not set then return the default
     * value.
     *
     * @param propertyName to get the value for.
     * @param defaultValue to use if the property is not set.
     * @return the value of a {@link System#getProperty(String, String)} with the exception that if the value is
     * {@link #NULL_PROPERTY_VALUE} then return {@code null}, otherwise if the value is not set then return the default
     * value.
     */
    public static String getProperty(final String propertyName, final String defaultValue) {
        final String propertyValue = System.getProperty(propertyName);
        if (NULL_PROPERTY_VALUE.equals(propertyValue)) {
            return null;
        }

        return null == propertyValue ? defaultValue : propertyValue;
    }

    /**
     * Get a size value as an int from a system property. Supports a 'g', 'm', and 'k' suffix to indicate
     * gigabytes, megabytes, or kilobytes respectively.
     *
     * @param propertyName to lookup.
     * @param defaultValue to be applied if the system property is not set.
     * @return the int value.
     * @throws NumberFormatException if the value is out of range or mal-formatted.
     */
    public static int getSizeAsInt(final String propertyName, final int defaultValue) {
        final String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null) {
            final long value = parseSize(propertyName, propertyValue);
            if (value < 0 || value > Integer.MAX_VALUE) {
                throw new NumberFormatException(
                        propertyName + " must positive and less than Integer.MAX_VALUE: " + value);
            }

            return (int) value;
        }

        return defaultValue;
    }

    /**
     * Get a size value as a long from a system property. Supports a 'g', 'm', and 'k' suffix to indicate
     * gigabytes, megabytes, or kilobytes respectively.
     *
     * @param propertyName to lookup.
     * @param defaultValue to be applied if the system property is not set.
     * @return the long value.
     * @throws NumberFormatException if the value is out of range or mal-formatted.
     */
    public static long getSizeAsLong(final String propertyName, final long defaultValue) {
        final String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null) {
            final long value = parseSize(propertyName, propertyValue);
            if (value < 0) {
                throw new NumberFormatException(propertyName + " must be positive: " + value);
            }

            return value;
        }

        return defaultValue;
    }

    /**
     * Parse a string representation of a value with optional suffix of 'g', 'm', and 'k' suffix to indicate
     * gigabytes, megabytes, or kilobytes respectively.
     *
     * @param propertyName  that associated with the size value.
     * @param propertyValue to be parsed.
     * @return the long value.
     * @throws NumberFormatException if the value is out of range or mal-formatted.
     */
    public static long parseSize(final String propertyName, final String propertyValue) {
        final int lengthMinusSuffix = propertyValue.length() - 1;
        final char lastCharacter = propertyValue.charAt(lengthMinusSuffix);
        if (Character.isDigit(lastCharacter)) {
            return Long.parseLong(propertyValue);
        }

        final long value = AsciiEncoding.parseLongAscii(propertyValue, 0, lengthMinusSuffix);

        switch (lastCharacter) {
            case 'k':
            case 'K':
                if (value > MAX_K_VALUE) {
                    throw new NumberFormatException(propertyName + " would overflow a long: " + propertyValue);
                }
                return value * 1024;

            case 'm':
            case 'M':
                if (value > MAX_M_VALUE) {
                    throw new NumberFormatException(propertyName + " would overflow a long: " + propertyValue);
                }
                return value * 1024 * 1024;

            case 'g':
            case 'G':
                if (value > MAX_G_VALUE) {
                    throw new NumberFormatException(propertyName + " would overflow a long: " + propertyValue);
                }
                return value * 1024 * 1024 * 1024;

            default:
                throw new NumberFormatException(
                        propertyName + ": " + propertyValue + " should end with: k, m, or g.");
        }
    }

    /**
     * Get a string representation of a time duration with an optional suffix of 's', 'ms', 'us', or 'ns' suffix to
     * indicate seconds, milliseconds, microseconds, or nanoseconds respectively.
     * <p>
     * If the resulting duration is greater than {@link Long#MAX_VALUE} then {@link Long#MAX_VALUE} is used.
     *
     * @param propertyName associated with the duration value.
     * @param defaultValue to be used if the property is not present.
     * @return the long value.
     * @throws NumberFormatException if the value is negative or malformed.
     */
    public static long getDurationInNanos(final String propertyName, final long defaultValue) {
        final String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null) {
            final long value = parseDuration(propertyName, propertyValue);
            if (value < 0) {
                throw new NumberFormatException(propertyName + " must be positive: " + value);
            }

            return value;
        }

        return defaultValue;
    }

    /**
     * Parse a string representation of a time duration with an optional suffix of 's', 'ms', 'us', or 'ns' to
     * indicate seconds, milliseconds, microseconds, or nanoseconds respectively.
     * <p>
     * If the resulting duration is greater than {@link Long#MAX_VALUE} then {@link Long#MAX_VALUE} is used.
     *
     * @param propertyName  associated with the duration value.
     * @param propertyValue to be parsed.
     * @return the long value.
     * @throws NumberFormatException if the value is negative or malformed.
     */
    public static long parseDuration(final String propertyName, final String propertyValue) {
        final char lastCharacter = propertyValue.charAt(propertyValue.length() - 1);
        if (Character.isDigit(lastCharacter)) {
            return Long.parseLong(propertyValue);
        }

        if (lastCharacter != 's' && lastCharacter != 'S') {
            throw new NumberFormatException(
                    propertyName + ": " + propertyValue + " should end with: s, ms, us, or ns.");
        }

        final char secondLastCharacter = propertyValue.charAt(propertyValue.length() - 2);
        if (Character.isDigit(secondLastCharacter)) {
            final long value = AsciiEncoding.parseLongAscii(propertyValue, 0, propertyValue.length() - 1);
            return TimeUnit.SECONDS.toNanos(value);
        }

        final long value = AsciiEncoding.parseLongAscii(propertyValue, 0, propertyValue.length() - 2);

        switch (secondLastCharacter) {
            case 'n':
            case 'N':
                return value;

            case 'u':
            case 'U':
                return TimeUnit.MICROSECONDS.toNanos(value);

            case 'm':
            case 'M':
                return TimeUnit.MILLISECONDS.toNanos(value);

            default:
                throw new NumberFormatException(
                        propertyName + ": " + propertyValue + " should end with: s, ms, us, or ns.");
        }
    }

    static boolean isX64Arch(final String arch) {
        return arch.equals("amd64") || arch.equals("x86_64") || arch.equals("x64");
    }

    private static void loadProperties(final PropertyAction propertyAction, final InputStream in) throws IOException {
        final Properties systemProperties = System.getProperties();
        final Properties properties = new Properties();

        properties.load(in);
        properties.forEach(
                (k, v) ->
                {
                    switch (propertyAction) {
                        case PRESERVE:
                            if (!systemProperties.containsKey(k)) {
                                systemProperties.setProperty((String) k, (String) v);
                            }
                            break;

                        default:
                        case REPLACE:
                            systemProperties.setProperty((String) k, (String) v);
                            break;
                    }
                });
    }
}
