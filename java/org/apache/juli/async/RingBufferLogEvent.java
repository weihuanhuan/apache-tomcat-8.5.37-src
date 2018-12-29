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

import com.lmax.disruptor.EventFactory;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.juli.util.LogEvent;
import org.apache.juli.util.Strings;

public class RingBufferLogEvent implements LogEvent {

    private static final long serialVersionUID = 8462119088943934758L;

    public static final Factory FACTORY = new Factory();
    private static final LogRecord EMPTY = new LogRecord(Level.INFO, Strings.EMPTY);

    private transient AsyncDirectJDKLog asyncDirectJDKLog;
    private String loggerName;

    private Level level;
    private LogRecord message;
    private transient Throwable thrown;

    private long threadId;
    private String threadName;
    private int threadPriority;

    private boolean endOfBatch = false;

    public RingBufferLogEvent() {
    }


    public void setValues(final AsyncDirectJDKLog asyncDirectJDKLog, final String loggerName,
                          final Level level, final LogRecord msg, final Throwable throwable,
                          final long threadId, final String threadName, final int threadPriority) {

        this.asyncDirectJDKLog = asyncDirectJDKLog;
        this.loggerName = loggerName;

        this.level = level;
        this.message = msg;
        this.thrown = throwable;

        this.threadPriority = threadPriority;
        this.threadId = threadId;
        this.threadName = threadName;
    }

    /**
     * Event processor that reads the event from the ringbuffer can call this method.
     *
     * @param endOfBatch flag to indicate if this is the last event in a batch from the RingBuffer
     */
    public void execute(final boolean endOfBatch) {
        this.endOfBatch = endOfBatch;
        asyncDirectJDKLog.actualAsyncLog(this);
    }

    @Override
    public String getLoggerName() {
        return loggerName;
    }

    @Override
    public Level getLevel() {
        if (level == null) {
            level = Level.OFF; // LOG4J2-462, LOG4J2-465
        }
        return level;
    }

    @Override
    public LogRecord getMessage() {
        if (message == null) {
            return message == null ? EMPTY : message;
        }
        return message;
    }

    public Throwable getThrown() {
        return thrown;
    }


    @Override
    public long getThreadId() {
        return threadId;
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    @Override
    public int getThreadPriority() {
        return threadPriority;
    }

    /**
     * Returns {@code true} if this event is the end of a batch, {@code false} otherwise.
     *
     * @return {@code true} if this event is the end of a batch, {@code false} otherwise
     */
    @Override
    public boolean isEndOfBatch() {
        return endOfBatch;
    }

    @Override
    public void setEndOfBatch(final boolean endOfBatch) {
        this.endOfBatch = endOfBatch;
    }

    /**
     * Release references held by ring buffer to allow objects to be garbage-collected.
     */
    public void clear() {
        this.asyncDirectJDKLog = null;
        this.loggerName = null;
        this.level = null;
        this.message = null;
        this.thrown = null;
    }

    private static class Factory implements EventFactory<RingBufferLogEvent> {

        @Override
        public RingBufferLogEvent newInstance() {
            final RingBufferLogEvent result = new RingBufferLogEvent();
            return result;
        }
    }

}
