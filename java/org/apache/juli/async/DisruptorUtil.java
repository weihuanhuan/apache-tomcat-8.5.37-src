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

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.apache.juli.util.Integers;
import org.apache.juli.util.LoaderUtil;

/**
 * Utility methods for getting Disruptor related configuration.
 */
final class DisruptorUtil {
    private static final int RINGBUFFER_MIN_SIZE = 128;
    private static final int RINGBUFFER_DEFAULT_SIZE = 256 * 1024;

    private DisruptorUtil() {
    }

    static long getTimeout(final String propertyName, final long defaultTimeout) {
        return Long.parseLong(System.getProperty(propertyName, String.valueOf(defaultTimeout)));
    }

    static WaitStrategy createWaitStrategy(final String propertyName) {
        final String key = propertyName.startsWith("AsyncDirectJDKLog.") ? "AsyncDirectJDKLog.Timeout" : "AsyncDefaultLog";
        final long timeoutMillis = DisruptorUtil.getTimeout(key, 10L);
        return createWaitStrategy(propertyName, timeoutMillis);
    }

    static WaitStrategy createWaitStrategy(final String propertyName, final long timeoutMillis) {
        final String strategy = System.getProperty(propertyName, "TIMEOUT");
        final String strategyUp = strategy.toUpperCase(Locale.ROOT); // TODO Refactor into Strings.toRootUpperCase(String)
        switch (strategyUp) { // TODO Define a DisruptorWaitStrategy enum?
            case "SLEEP":
                return new SleepingWaitStrategy();
            case "YIELD":
                return new YieldingWaitStrategy();
            case "BLOCK":
                return new BlockingWaitStrategy();
            case "BUSYSPIN":
                return new BusySpinWaitStrategy();
            case "TIMEOUT":
                return new TimeoutBlockingWaitStrategy(timeoutMillis, TimeUnit.MILLISECONDS);
            default:
                return new TimeoutBlockingWaitStrategy(timeoutMillis, TimeUnit.MILLISECONDS);
        }
    }

    static int calculateRingBufferSize(final String propertyName) {
        int ringBufferSize = RINGBUFFER_DEFAULT_SIZE;
        final String userPreferredRBSize = System.getProperty(propertyName,
                String.valueOf(ringBufferSize));
        try {
            int size = Integer.parseInt(userPreferredRBSize);
            if (size < RINGBUFFER_MIN_SIZE) {
                size = RINGBUFFER_MIN_SIZE;
            }
            ringBufferSize = size;
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
        return Integers.ceilingNextPowerOfTwo(ringBufferSize);
    }

    static ExceptionHandler<RingBufferLogEvent> getAsyncLoggerExceptionHandler() {
        final String cls = System.getProperty("AsyncDirectJDKLog.ExceptionHandler");
        if (cls == null) {
            return new AsyncLoggerDefaultExceptionHandler();
        }
        try {
            @SuppressWarnings("unchecked") final Class<? extends ExceptionHandler<RingBufferLogEvent>> klass =
                    (Class<? extends ExceptionHandler<RingBufferLogEvent>>) LoaderUtil.loadClass(cls);
            return klass.newInstance();
        } catch (final Exception ignored) {
            return new AsyncLoggerDefaultExceptionHandler();
        }
    }

}
