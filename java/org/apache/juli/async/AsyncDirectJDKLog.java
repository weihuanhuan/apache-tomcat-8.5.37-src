/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.juli.async;

import com.lmax.disruptor.EventTranslatorVararg;
import com.lmax.disruptor.dsl.Disruptor;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.juli.logging.DirectJDKLog;
import org.apache.juli.logging.Log;

public class AsyncDirectJDKLog extends DirectJDKLog implements EventTranslatorVararg<RingBufferLogEvent> {

    private static final ThreadNameCachingStrategy THREAD_NAME_CACHING_STRATEGY = ThreadNameCachingStrategy.create();
    private static final AsyncLoggerDisruptor loggerDisruptor = new AsyncLoggerDisruptor();

    private final ThreadLocal<RingBufferLogEventTranslator> threadLocalTranslator = new ThreadLocal<>();

    private static final String FQCN = "Fully Quality Class Name";

    public AsyncDirectJDKLog(final String name) {
        super(name);
    }

    private RingBufferLogEventTranslator getCachedTranslator() {
        RingBufferLogEventTranslator result = threadLocalTranslator.get();
        if (result == null) {
            result = new RingBufferLogEventTranslator();
            threadLocalTranslator.set(result);
        }
        return result;
    }

    public void logMessage(final String fqcn, final Level level, final String message,
                           final Throwable thrown) {

        LogRecord lr = new LogRecord(level, message);
        if (loggerDisruptor.isUseThreadLocals()) {
            logWithThreadLocalTranslator(fqcn, level, lr, thrown);
        } else {
            // LOG4J2-1172: avoid storing non-JDK classes in ThreadLocals to avoid memory leaks in web apps
            logWithVarargTranslator(fqcn, level, lr, thrown);
        }
    }

    /**
     * Enqueues the specified log event data for logging in a background thread.
     * <p>
     * This re-uses a {@code RingBufferLogEventTranslator} instance cached in a {@code ThreadLocal} to avoid creating
     * unnecessary objects with each event.
     *
     * @param fqcn    fully qualified name of the caller
     * @param level   level at which the caller wants to log the message
     * @param message the log message
     * @param thrown  a {@code Throwable} or {@code null}
     */
    private void logWithThreadLocalTranslator(final String fqcn, final Level level,
                                              final LogRecord message, final Throwable thrown) {
        // Implementation note: this method is tuned for performance. MODIFY WITH CARE!

        final RingBufferLogEventTranslator translator = getCachedTranslator();
        initTranslator(translator, fqcn, level, message, thrown);
        initTranslatorThreadValues(translator);
        publish(translator);
    }

    private void publish(final RingBufferLogEventTranslator translator) {
        if (!loggerDisruptor.tryPublish(translator)) {
            handleRingBufferFull(translator);
        }
    }

    private void handleRingBufferFull(final RingBufferLogEventTranslator translator) {
        final EventRoute eventRoute = loggerDisruptor.getEventRoute(translator.level);
        switch (eventRoute) {
            case ENQUEUE:
                loggerDisruptor.enqueueLogMessageInfo(translator);
                break;
            case SYNCHRONOUS:
                logMessageInCurrentThread(translator.fqcn, translator.level, translator.message,
                        translator.thrown);
                break;
            case DISCARD:
                break;
            default:
                throw new IllegalStateException("Unknown EventRoute " + eventRoute);
        }
    }

    private void initTranslator(final RingBufferLogEventTranslator translator, final String fqcn,
                                final Level level, final LogRecord message, final Throwable thrown) {

        translator.setBasicValues(this, this.getClass().getSimpleName(), fqcn, level, message, thrown);
    }

    private void initTranslatorThreadValues(final RingBufferLogEventTranslator translator) {
        // constant check should be optimized out when using default (CACHED)
        if (THREAD_NAME_CACHING_STRATEGY == ThreadNameCachingStrategy.UNCACHED) {
            translator.updateThreadValues();
        }
    }


    /**
     * Enqueues the specified log event data for logging in a background thread.
     * <p>
     * This creates a new varargs Object array for each invocation, but does not store any non-JDK classes in a
     * {@code ThreadLocal} to avoid memory leaks in web applications (see LOG4J2-1172).
     *
     * @param fqcn    fully qualified name of the caller
     * @param level   level at which the caller wants to log the message
     * @param message the log message
     * @param thrown  a {@code Throwable} or {@code null}
     */
    private void logWithVarargTranslator(final String fqcn, final Level level,
                                         final LogRecord message, final Throwable thrown) {
        // Implementation note: candidate for optimization: exceeds 35 bytecodes.

        final Disruptor<RingBufferLogEvent> disruptor = loggerDisruptor.getDisruptor();
        if (disruptor == null) {
//            LOGGER.error("Ignoring log event after Log4j has been shut down.");
            return;
        }
        // calls the translateTo method on this AsyncDirectJDKLog
        if (!disruptor.getRingBuffer().tryPublishEvent(this,
                this, // asyncLogger: 0

                fqcn, // 2
                level, // 3// 4
                message, // 5
                thrown)) { // 6
            handleRingBufferFull(fqcn, level, message, thrown);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.lmax.disruptor.EventTranslatorVararg#translateTo(java.lang.Object, long, java.lang.Object[])
     */
    @Override
    public void translateTo(final RingBufferLogEvent event, final long sequence, final Object... args) {
        // Implementation note: candidate for optimization: exceeds 35 bytecodes.
        final AsyncDirectJDKLog asyncDirectJDKLog = (AsyncDirectJDKLog) args[0];
        final StackTraceElement location = (StackTraceElement) args[1];
        final String fqcn = (String) args[2];
        final Level level = (Level) args[3];
        final LogRecord message = (LogRecord) args[5];
        final Throwable thrown = (Throwable) args[6];


        final Thread currentThread = Thread.currentThread();
        final String threadName = THREAD_NAME_CACHING_STRATEGY.getThreadName();
        event.setValues(asyncDirectJDKLog, FQCN, fqcn, level, message, thrown,
                currentThread.getId(), threadName, currentThread.getPriority());
    }

    /**
     * LOG4J2-471: prevent deadlock when RingBuffer is full and object being logged calls Logger.log() from its
     * toString() method
     *
     * @param fqcn    fully qualified caller name
     * @param level   log level
     * @param message log message
     * @param thrown  optional exception
     */
    void logMessageInCurrentThread(final String fqcn, final Level level,
                                   final LogRecord message, final Throwable thrown) {
        // bypass RingBuffer and invoke Appender directly
        logger.log(message);
    }

    private void handleRingBufferFull(final String fqcn,
                                      final Level level,
                                      final LogRecord msg,
                                      final Throwable thrown) {
        final EventRoute eventRoute = loggerDisruptor.getEventRoute(level);
        switch (eventRoute) {
            case ENQUEUE:
                loggerDisruptor.getDisruptor().getRingBuffer().publishEvent(this, this, fqcn, level, msg, thrown);
                break;
            case SYNCHRONOUS:
                logMessageInCurrentThread(fqcn, level, msg, thrown);
                break;
            case DISCARD:
                break;
            default:
                throw new IllegalStateException("Unknown EventRoute " + eventRoute);
        }
    }

    /**
     * This method is called by the EventHandler that processes the RingBufferLogEvent in a separate thread.
     * Merges the contents of the configuration map into the contextData, after replacing any variables in the property
     * values with the StrSubstitutor-supplied actual values.
     *
     * @param event the event to log
     */
    public void actualAsyncLog(final RingBufferLogEvent event) {

        logger.log(event.getMessage());
    }

    @Override
    public final void debug(Object message) {
        logMessage(FQCN, Level.FINE, String.valueOf(message), null);
    }

    @Override
    public final void debug(Object message, Throwable t) {
        logMessage(FQCN, Level.FINE, String.valueOf(message), t);
    }

    @Override
    public final void trace(Object message) {
        logMessage(FQCN, Level.FINER, String.valueOf(message), null);
    }

    @Override
    public final void trace(Object message, Throwable t) {
        logMessage(FQCN, Level.FINER, String.valueOf(message), t);
    }

    @Override
    public final void info(Object message) {
        logMessage(FQCN, Level.INFO, String.valueOf(message), null);
    }

    @Override
    public final void info(Object message, Throwable t) {
        logMessage(FQCN, Level.INFO, String.valueOf(message), t);
    }

    @Override
    public final void warn(Object message) {
        logMessage(FQCN, Level.WARNING, String.valueOf(message), null);
    }

    @Override
    public final void warn(Object message, Throwable t) {
        logMessage(FQCN, Level.WARNING, String.valueOf(message), t);
    }

    @Override
    public final void error(Object message) {
        logMessage(FQCN, Level.SEVERE, String.valueOf(message), null);
    }

    @Override
    public final void error(Object message, Throwable t) {
        logMessage(FQCN, Level.SEVERE, String.valueOf(message), t);
    }

    @Override
    public final void fatal(Object message) {
        logMessage(FQCN, Level.SEVERE, String.valueOf(message), null);
    }

    @Override
    public final void fatal(Object message, Throwable t) {
        logMessage(FQCN, Level.SEVERE, String.valueOf(message), t);
    }

}
