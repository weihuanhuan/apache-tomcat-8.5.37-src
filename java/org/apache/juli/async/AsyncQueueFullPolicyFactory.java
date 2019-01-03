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
import org.apache.juli.util.LoaderUtil;

/**
 * Creates {@link AsyncQueueFullPolicy} instances based on user-specified system properties. The {@code AsyncQueueFullPolicy}
 * created by this factory is used in AsyncDirectJDKLog, AsyncLoggerConfig and AsyncAppender
 * to control if events are logged in the current thread, the background thread, or discarded.
 * <p>
 * Property {@code "log4j2.AsyncQueueFullPolicy"} controls the routing behaviour. If this property is not specified or has
 * value {@code "Default"}, this factory creates {@link DefaultAsyncQueueFullPolicy} objects.
 * </p> <p>
 * If this property has value {@code "Discard"}, this factory creates {@link DiscardingAsyncQueueFullPolicy} objects.
 * By default, this router discards events of level {@code INFO}, {@code DEBUG} and {@code TRACE} if the queue is full.
 * This can be adjusted with property {@code "log4j2.DiscardThreshold"} (name of the level at which to start
 * discarding).
 * </p> <p>
 * For any other value, this
 * factory interprets the value as the fully qualified name of a class implementing the {@link AsyncQueueFullPolicy}
 * interface. The class must have a default constructor.
 * </p>
 *
 * @since 2.6
 */
public class AsyncQueueFullPolicyFactory {
    static final String PROPERTY_NAME_ASYNC_EVENT_ROUTER = "log4j2.AsyncQueueFullPolicy";
    static final String PROPERTY_VALUE_DEFAULT_ASYNC_EVENT_ROUTER = "Default";
    static final String PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER = "Discard";
    static final String PROPERTY_NAME_DISCARDING_THRESHOLD_LEVEL = "log4j2.DiscardThreshold";


    /**
     * Creates and returns {@link AsyncQueueFullPolicy} instances based on user-specified system properties.
     * <p>
     * Property {@code "log4j2.AsyncQueueFullPolicy"} controls the routing behaviour. If this property is not specified or
     * has value {@code "Default"}, this method returns {@link DefaultAsyncQueueFullPolicy} objects.
     * </p> <p>
     * If this property has value {@code "Discard"}, this method returns {@link DiscardingAsyncQueueFullPolicy} objects.
     * </p> <p>
     * For any other value, this method interprets the value as the fully qualified name of a class implementing the
     * {@link AsyncQueueFullPolicy} interface. The class must have a default constructor.
     * </p>
     *
     * @return a new AsyncQueueFullPolicy
     */
    public static AsyncQueueFullPolicy create() {
        final String router = System.getProperty(PROPERTY_NAME_ASYNC_EVENT_ROUTER);
        if (router == null || PROPERTY_VALUE_DEFAULT_ASYNC_EVENT_ROUTER.equals(router)
                || DefaultAsyncQueueFullPolicy.class.getSimpleName().equals(router)
                || DefaultAsyncQueueFullPolicy.class.getName().equals(router)) {
            return new DefaultAsyncQueueFullPolicy();
        }
        if (PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER.equals(router)
                || DiscardingAsyncQueueFullPolicy.class.getSimpleName().equals(router)
                || DiscardingAsyncQueueFullPolicy.class.getName().equals(router)) {
            return createDiscardingAsyncQueueFullPolicy();
        }
        return createCustomRouter(router);
    }

    private static AsyncQueueFullPolicy createCustomRouter(final String router) {
        return new DefaultAsyncQueueFullPolicy();
    }

    private static AsyncQueueFullPolicy createDiscardingAsyncQueueFullPolicy() {
        final String level = System.getProperty(PROPERTY_NAME_DISCARDING_THRESHOLD_LEVEL, Level.INFO.getName());
        final Level thresholdLevel = Level.parse(level);
        return new DiscardingAsyncQueueFullPolicy(thresholdLevel);
    }
}
