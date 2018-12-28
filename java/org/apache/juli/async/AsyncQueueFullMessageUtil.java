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


/**
 * <b>Consider this class private.</b>
 * <p>
 * </p>
 */
public final class AsyncQueueFullMessageUtil {
    private AsyncQueueFullMessageUtil() {
        // Utility Class
    }

    /**
     */
    public static void logWarningToStatusLogger() {
//        StatusLogger.getLogger()
//                .warn("LOG4J2-2031: Log4j2 logged an event out of order to prevent deadlock caused by domain " +
//                        "objects logging from their toString method when the async queue is full");
    }
}
