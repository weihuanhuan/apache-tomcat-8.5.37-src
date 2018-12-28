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

    public static final Factory FACTORY = new Factory();

    private static final long serialVersionUID = 8462119088943934758L;
    private static final LogRecord EMPTY = new LogRecord(Level.INFO, Strings.EMPTY);


    public RingBufferLogEvent() {
    }

    private static class Factory implements EventFactory<RingBufferLogEvent> {

        @Override
        public RingBufferLogEvent newInstance() {
            final RingBufferLogEvent result = new RingBufferLogEvent();
            return result;
        }
    }

    private int threadPriority;
    private long threadId;
    private boolean endOfBatch = false;
    private Level level;
    private String threadName;
    private String loggerName;
    private LogRecord message;
    private transient Throwable thrown;
    private String fqcn;

    private transient AsyncDirectJDKLog asyncDirectJDKLog;

    public void setValues(final AsyncDirectJDKLog anAsyncDirectJDKLog, final String aLoggerName,
                          final String theFqcn, final Level aLevel, final LogRecord msg, final Throwable aThrowable,
                          final long threadId, final String threadName, final int threadPriority) {
        this.threadPriority = threadPriority;
        this.threadId = threadId;
        this.level = aLevel;
        this.threadName = threadName;
        this.loggerName = aLoggerName;
        setMessage(msg);
        this.thrown = aThrowable;
        this.fqcn = theFqcn;
        this.asyncDirectJDKLog = anAsyncDirectJDKLog;
    }


    @Override
    public String getLoggerFqcn() {
        return fqcn;
    }

    private void setMessage(final LogRecord msg) {
        this.message = msg;
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
     * Release references held by ring buffer to allow objects to be garbage-collected.
     */
    public void clear() {
        this.asyncDirectJDKLog = null;
        this.loggerName = null;
        this.fqcn = null;
        this.level = null;
        this.message = null;
        this.thrown = null;
    }

}
