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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public class DiscardingAsyncQueueFullPolicy extends DefaultAsyncQueueFullPolicy {

    private final Level thresholdLevel;
    private final AtomicLong discardCount = new AtomicLong();

    public DiscardingAsyncQueueFullPolicy(final Level thresholdLevel) {
        this.thresholdLevel = Objects.requireNonNull(thresholdLevel, "thresholdLevel");
    }

    @Override
    public EventRoute getRoute(final long backgroundThreadId, final Level level) {
        if (level.intValue() < thresholdLevel.intValue()) {
            if (discardCount.getAndIncrement() == 0) {
            }
            return EventRoute.DISCARD;
        }
        return super.getRoute(backgroundThreadId, level);
    }

    public static long getDiscardCount(final AsyncQueueFullPolicy router) {
        if (router instanceof DiscardingAsyncQueueFullPolicy) {
            return ((DiscardingAsyncQueueFullPolicy) router).discardCount.get();
        }
        return 0;
    }

    public Level getThresholdLevel() {
        return thresholdLevel;
    }
}
