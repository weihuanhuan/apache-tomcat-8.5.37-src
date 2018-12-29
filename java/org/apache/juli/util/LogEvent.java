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

package org.apache.juli.util;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Provides contextual information about a logged message. A LogEvent must be {@link Serializable} so that it
 * may be transmitted over a network connection, output in a
 * and deserialized properly without causing problems if the exception class is not available on the other end.
 * <p>
 * can carry both {@code ThreadContext} data as well as other context data supplied by the
 * </p>
 */
public interface LogEvent extends Serializable {

    /**
     * Gets the logger name.
     *
     * @return logger name, may be {@code null}.
     */
    String getLoggerName();

    Level getLevel();

    /**
     * Gets the message associated with the event.
     *
     * @return message.
     */
    LogRecord getMessage();

    /**
     * Gets the thread ID.
     *
     * @return thread ID.
     * @since 2.6
     */
    long getThreadId();

    /**
     * Gets the thread name.
     *
     * @return thread name, may be null.
     * TODO guess this could go into a thread context object too. (RG) Why?
     */
    String getThreadName();

    /**
     * Gets the thread priority.
     *
     * @return thread priority.
     * @since 2.6
     */
    int getThreadPriority();

    /**
     * Returns {@code true} if this event is the last one in a batch, {@code false} otherwise. Used by asynchronous
     * Loggers and Appenders to signal to buffered downstream components when to flush to disk, as a more efficient
     * alternative to the {@code immediateFlush=true} configuration.
     *
     * @return whether this event is the last one in a batch.
     */
    boolean isEndOfBatch();

    /**
     * Sets whether this event is the last one in a batch. Used by asynchronous Loggers and Appenders to signal to
     * buffered downstream components when to flush to disk, as a more efficient alternative to the
     * {@code immediateFlush=true} configuration.
     *
     * @param endOfBatch {@code true} if this event is the last one in a batch, {@code false} otherwise.
     */
    void setEndOfBatch(boolean endOfBatch);

}
