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

import com.lmax.disruptor.EventTranslator;
import java.util.logging.Level;

/**
 * This class is responsible for writing elements that make up a log event into
 * the ringbuffer {@code RingBufferLogEvent}. After this translator populated
 * the ringbuffer event, the disruptor will update the sequence number so that
 * the event can be consumed by another thread.
 */
public class RingBufferLogEventTranslator implements
        EventTranslator<RingBufferLogEvent> {

    private AsyncDirectJDKLog asyncDirectJDKLog;
    public Level level;
    public String message;
    public Throwable thrown;
    private String loggerName;

    private int threadId = (int) Thread.currentThread().getId();

    @Override
    public void translateTo(final RingBufferLogEvent event, final long sequence) {

        event.setValues(asyncDirectJDKLog, loggerName,
                level, message, thrown, threadId);
        clear(); // clear the translator
    }

    /**
     * Release references held by this object to allow objects to be garbage-collected.
     */
    private void clear() {
        setBasicValues(null, null, null, null, null);
    }

    public void setBasicValues(final AsyncDirectJDKLog asyncDirectJDKLog, final String loggerName,
                               final Level level, final String msg, final Throwable throwable) {
        this.asyncDirectJDKLog = asyncDirectJDKLog;
        this.loggerName = loggerName;
        this.level = level;
        this.message = msg;
        this.thrown = throwable;
    }

    public void updateThreadValues() {
        this.threadId = (int) Thread.currentThread().getId();
    }
}
