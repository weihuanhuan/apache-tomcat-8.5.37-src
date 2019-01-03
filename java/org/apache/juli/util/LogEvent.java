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
     * Gets the Level.
     *
     * @return Level.
     */
    Level getLevel();

    /**
     * Gets the message.
     *
     * @return message.
     */
    String getMessage();

    /**
     * Gets the Thrown
     *
     * @return Thrown.
     */

    Throwable getThrown();

    /**
     * Gets the thread ID.
     *
     * @return thread ID.
     */
    int getThreadId();

    /**
     * Gets the Logger Name
     *
     * @return Logger Name.
     */
    String getLoggerName();

}
