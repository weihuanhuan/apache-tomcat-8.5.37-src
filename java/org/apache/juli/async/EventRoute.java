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


import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Enumeration over the different destinations where a log event can be sent.
 *
 * @see AsyncQueueFullPolicy
 * @see AsyncQueueFullPolicyFactory
 * @see DefaultAsyncQueueFullPolicy
 * @see DiscardingAsyncQueueFullPolicy
 * @since 2.6
 */
public enum EventRoute {
    /**
     * Enqueues the event for asynchronous logging in the background thread.
     */
    ENQUEUE {
        @Override
        public void logMessage(final AsyncDirectJDKLog asyncDirectJDKLog, final String fqcn, final Level level,
                               final LogRecord message, final Throwable thrown) {
        }

    },
    /**
     * Logs the event synchronously: sends the event directly to the appender (in the current thread).
     */
    SYNCHRONOUS {
        @Override
        public void logMessage(final AsyncDirectJDKLog asyncDirectJDKLog, final String fqcn, final Level level,
                               final LogRecord message, final Throwable thrown) {
        }


    },
    /**
     * Discards the event (so it is not logged at all).
     */
    DISCARD {
        @Override
        public void logMessage(final AsyncDirectJDKLog asyncDirectJDKLog, final String fqcn, final Level level,
                               final LogRecord message, final Throwable thrown) {
            // do nothing: drop the event
        }


    };

    public abstract void logMessage(final AsyncDirectJDKLog asyncDirectJDKLog, final String fqcn, final Level level,
                                    final LogRecord message, final Throwable thrown);


}
