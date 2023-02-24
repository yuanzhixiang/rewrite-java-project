package io.aeron;

import io.aeron.exceptions.AeronException;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static java.util.concurrent.atomic.AtomicIntegerFieldUpdater.newUpdater;

/**
 * Aeron entry point for communicating to the Media Driver for creating {@link Publication}s and {@link Subscription}s.
 * Use an {@link Aeron.Context} to configure the Aeron object.
 * <p>
 * A client application requires only one Aeron object per Media Driver.
 * <p>
 * <b>Note:</b> If {@link Aeron.Context#errorHandler(ErrorHandler)} is not set and a {@link DriverTimeoutException}
 * occurs then the process will face the wrath of {@link System#exit(int)}.
 * See {@link Aeron.Configuration#DEFAULT_ERROR_HANDLER}.
 */
public class Aeron implements AutoCloseable {
    /**
     * Used to represent a null value for when some value is not yet set.
     */
    public static final int NULL_VALUE = -1;

    /**
     * Using an integer because there is no support for boolean. 1 is closed, 0 is not closed.
     */
    private static final AtomicIntegerFieldUpdater<Aeron> IS_CLOSED_UPDATER = newUpdater(Aeron.class, "isClosed");

    @Override
    public void close() throws Exception {
        throw new UnsupportedOperationException();
    }

    static void sleep(final long durationMs) {
        try {
            Thread.sleep(durationMs);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AeronException("unexpected interrupt", ex);
        }
    }
}
