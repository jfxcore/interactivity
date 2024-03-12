/*
 * Copyright (c) 2024, JFXcore. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  JFXcore designates this
 * particular file as subject to the "Classpath" exception as provided
 * in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jfxcore.interaction;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class InteractionRequestTest {

    @Nested
    class WhenNoListenerIsPresent {
        @Test
        void testRequest() {
            var interaction = new Interaction<Integer, String>();
            assertThrows(UnhandledInteractionException.class, () -> interaction.request(123));
        }

        @Test
        void testRequestAndWait() {
            var interaction = new Interaction<Integer, String>();
            assertThrows(UnhandledInteractionException.class, () -> interaction.requestAndWait(123));
        }
    }

    @Nested
    class WhenNoListenerResponds {
        @Test
        void testRequest() {
            var interaction = new Interaction<Integer, String>();
            interaction.addListener(request -> false);
            assertThrows(UnhandledInteractionException.class, () -> interaction.request(123));
        }

        @Test
        void testRequestAndWait() {
            var interaction = new Interaction<Integer, String>();
            interaction.addListener(request -> false);
            assertThrows(UnhandledInteractionException.class, () -> interaction.requestAndWait(123));
        }
    }

    @Nested
    class WhenMultipleListenersArePresent {
        Interaction<Integer, String> interaction;
        InteractionListener<Integer, String> asyncListener = request -> {
            scheduleTask(() -> request.complete("async-" + request.getPayload()));
            return true;
        };

        {
            interaction = new Interaction<>();
            interaction.addListener(request -> {
                request.complete("first-" + request.getPayload());
                return true;
            });
            interaction.addListener(request -> {
                request.complete("second-" + request.getPayload());
                return true;
            });
        }

        @Test
        void testRequest_secondListenerResponds() {
            var response = new TestConsumer<String>();
            interaction.request(123).whenCompleted(response);
            assertEquals("second-123", response.value);
        }

        @Test
        void testRequest_newlyAddedListenerResponds() {
            var subscription = interaction.subscribe(request -> {
                request.complete("new-" + request.getPayload());
                return true;
            });

            var response = new TestConsumer<String>();
            interaction.request(123).whenCompleted(response);
            assertEquals("new-123", response.value);

            subscription.unsubscribe();
            interaction.request(123).whenCompleted(response);
            assertEquals("second-123", response.value);
        }

        @Test
        void testRequest_asyncListenerResponds() {
            var response = new AwaitableTestConsumer<String>();
            var subscription = interaction.subscribe(asyncListener);
            interaction.request(123).whenCompleted(response);
            assertEquals("async-123", response.awaitValue());
            subscription.unsubscribe();
        }

        @Test
        void testRequestAndWait_secondListenerResponds() {
            String result = interaction.requestAndWait(123);
            assertEquals("second-123", result);
        }

        @Test
        void testRequestAndWait_newlyAddedListenerResponds() {
            var subscription = interaction.subscribe(request -> {
                request.complete("new-" + request.getPayload());
                return true;
            });

            String result = interaction.requestAndWait(123);
            assertEquals("new-123", result);

            subscription.unsubscribe();
            result = interaction.requestAndWait(123);
            assertEquals("second-123", result);
        }

        @Test
        void testRequestAndWait_asyncListenerResponds() {
            var subscription = interaction.subscribe(asyncListener);
            String result = interaction.requestAndWait(123);
            assertEquals("async-123", result);
            subscription.unsubscribe();
        }
    }

    @Nested
    class WhenListenerDoesNotAcceptRequest {
        Interaction<Integer, String> interaction;

        {
            interaction = new Interaction<>();
            interaction.addListener(request -> {
                request.complete("first-" + request.getPayload());
                return true;
            });
            interaction.addListener(request -> false);
        }

        @Test
        void testRequest_nextListenerResponds() {
            var response = new TestConsumer<String>();
            interaction.request(123).whenCompleted(response);
            assertEquals("first-123", response.value);
        }

        @Test
        void testRequestAndWait_nextListenerResponds() {
            String result = interaction.requestAndWait(123);
            assertEquals("first-123", result);
        }

        @Test
        void testRequest_newlyAddedListenerIsSkipped() {
            var subscription = interaction.subscribe(request -> false);
            var response = new TestConsumer<String>();
            interaction.request(123).whenCompleted(response);
            assertEquals("first-123", response.value);

            subscription.unsubscribe();
            interaction.request(123).whenCompleted(response);
            assertEquals("first-123", response.value);
        }

        @Test
        void testRequestAndWait_newlyAddedListenerIsSkipped() {
            var subscription = interaction.subscribe(request -> false);
            String result = interaction.requestAndWait(123);
            assertEquals("first-123", result);

            subscription.unsubscribe();
            result = interaction.requestAndWait(123);
            assertEquals("first-123", result);
        }
    }

    @Nested
    class WhenRequestIsCancelled {
        @Test
        void testCancellationHandlerIsInvoked() {
            var interaction = new Interaction<Integer, String>();
            interaction.addListener(request -> true);
            var cancelled = new AtomicBoolean();
            var request = interaction.request(123).whenCancelled(() -> cancelled.set(true));
            request.cancel();
            assertTrue(cancelled.get());
        }

        @Test
        void testCancellationHandlerIsInvoked_whenInteractionIsAlreadyCancelled() {
            var interaction = new Interaction<Integer, String>();
            interaction.addListener(request -> true);
            var cancelled = new AtomicBoolean();
            var request = interaction.request(123);
            request.cancel();
            request.whenCancelled(() -> cancelled.set(true));
            assertTrue(cancelled.get());
        }

        @Test
        void testRequestAndWaitThrowsCancellationException_withImmediateCancellation() {
            var interaction = new Interaction<Integer, String>();
            interaction.addListener(request -> {
                request.cancel();
                return true;
            });

            assertThrows(CancellationException.class, () -> interaction.requestAndWait(123));
        }

        @Test
        void testRequestAndWaitThrowsCancellationException_withDelayedCancellation() {
            var interaction = new Interaction<Integer, String>();
            interaction.addListener(request -> {
                scheduleTask(request::cancel);
                return true;
            });

            assertThrows(CancellationException.class, () -> interaction.requestAndWait(123));
        }
    }

    @Nested
    @ExtendWith(ApplicationExtension.class)
    class WhenRequestIsCompleted {
        @Test
        void testCompletionHandlerIsInvoked_withImmediateCompletion() {
            var interaction = new Interaction<Integer, String>();
            interaction.addListener(request -> {
                request.complete("payload-" + request.getPayload());
                return true;
            });

            var response = new TestConsumer<String>();
            interaction.request(123).whenCompleted(response);
            assertEquals("payload-123", response.value);
        }

        @Test
        void testCompletionHandlerIsInvoked_withDelayedCompletion() {
            var interaction = new Interaction<Integer, String>();
            interaction.addListener(request -> {
                scheduleTask(() -> request.complete("payload-" + request.getPayload()));
                return true;
            });

            var response = new AwaitableTestConsumer<String>();
            interaction.request(123).whenCompleted(response);
            assertEquals("payload-123", response.awaitValue());
        }

        @Test
        void testRequestAndWait_withMonitor_withImmediateCompletion() {
            testRequestAndWait_withImmediateCompletion(InteractionRequestBase.AwaitableMonitor.class);
        }

        @Test
        void testRequestAndWait_withEventLoop_withImmediateCompletion(FxRobot robot) {
            robot.interact(() -> testRequestAndWait_withImmediateCompletion(InteractionRequestBase.AwaitableEventLoop.class));
        }

        private void testRequestAndWait_withImmediateCompletion(Class<?> requestClass) {
            var interaction = new Interaction<Integer, String>();
            interaction.addListener(request -> {
                request.complete("payload-" + request.getPayload() + "-" + request.getClass().getName());
                return true;
            });

            String response = interaction.requestAndWait(123);
            assertEquals("payload-123-" + requestClass.getName(), response);
        }

        @Test
        void testRequestAndWait_withMonitor_withDelayedCompletion() {
            testRequestAndWait_withDelayedCompletion(InteractionRequestBase.AwaitableMonitor.class);
        }

        @Test
        void testRequestAndWait_withEventLoop_withDelayedCompletion(FxRobot robot) {
            robot.interact(() -> testRequestAndWait_withDelayedCompletion(InteractionRequestBase.AwaitableEventLoop.class));
        }

        private void testRequestAndWait_withDelayedCompletion(Class<?> requestClass) {
            var interaction = new Interaction<Integer, String>();
            interaction.addListener(request -> {
                scheduleTask(() -> request.complete(
                    "payload-" + request.getPayload() + "-" + request.getClass().getName()));
                return true;
            });

            String response = interaction.requestAndWait(123);
            assertEquals("payload-123-" + requestClass.getName(), response);
        }
    }

    @Nested
    @ExtendWith(ApplicationExtension.class)
    class WhenRequestIsExceptionallyCompleted {
        @Test
        void testExceptionalCompletionHandlerIsInvoked_withImmediateCompletion() {
            var interaction = new Interaction<Integer, String>();
            interaction.addListener(request -> {
                request.completeExceptionally(new RuntimeException("exception-" + request.getPayload()));
                return true;
            });

            var response = new TestConsumer<Throwable>();
            interaction.request(123).whenCompletedExceptionally(response);
            assertInstanceOf(RuntimeException.class, response.value);
            assertEquals("exception-123", response.value.getMessage());
        }

        @Test
        void testExceptionalCompletionHandlerIsInvoked_withDelayedCompletion() {
            var interaction = new Interaction<Integer, String>();
            interaction.addListener(request -> {
                scheduleTask(() -> request.completeExceptionally(
                    new RuntimeException("exception-" + request.getPayload())));
                return true;
            });

            var response = new AwaitableTestConsumer<Throwable>();
            interaction.request(123).whenCompletedExceptionally(response);
            response.awaitValue();
            assertInstanceOf(RuntimeException.class, response.value);
            assertEquals("exception-123", response.value.getMessage());
        }

        @Test
        void testRequestAndWaitThrowsInteractionException_withMonitor_withImmediateCompletion() {
            testRequestAndWaitThrowsInteractionException_withImmediateCompletion(
                InteractionRequestBase.AwaitableMonitor.class);
        }

        @Test
        void testRequestAndWaitThrowsInteractionException_withEventLoop_withImmediateCompletion(FxRobot robot) {
            robot.interact(() -> testRequestAndWaitThrowsInteractionException_withImmediateCompletion(
                InteractionRequestBase.AwaitableEventLoop.class));
        }

        private void testRequestAndWaitThrowsInteractionException_withImmediateCompletion(Class<?> requestClass) {
            var interaction = new Interaction<Integer, String>();
            interaction.addListener(request -> {
                request.completeExceptionally(
                    new RuntimeException("exception-" + request.getPayload() + "-" + request.getClass().getName()));
                return true;
            });

            var exception = assertThrows(InteractionException.class, () -> interaction.requestAndWait(123));
            assertEquals("exception-123-" + requestClass.getName(), exception.getCause().getMessage());
        }

        @Test
        void testRequestAndWaitThrowsInteractionException_withMonitor_withDelayedCompletion() {
            testRequestAndWaitThrowsInteractionException_withDelayedCompletion(
                InteractionRequestBase.AwaitableMonitor.class);
        }

        @Test
        void testRequestAndWaitThrowsInteractionException_withEventLoop_withDelayedCompletion(FxRobot robot) {
            robot.interact(() -> testRequestAndWaitThrowsInteractionException_withDelayedCompletion(
                InteractionRequestBase.AwaitableEventLoop.class));
        }

        private void testRequestAndWaitThrowsInteractionException_withDelayedCompletion(Class<?> requestClass) {
            var interaction = new Interaction<Integer, String>();
            interaction.addListener(request -> {
                scheduleTask(() -> request.completeExceptionally(
                    new RuntimeException("exception-" + request.getPayload() + "-" + request.getClass().getName())));
                return true;
            });

            var exception = assertThrows(InteractionException.class, () -> interaction.requestAndWait(123));
            assertEquals("exception-123-" + requestClass.getName(), exception.getCause().getMessage());
        }
    }

    interface RunnableEx {
        void run() throws Throwable;
    }

    static void scheduleTask(RunnableEx runnable) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }, 100);
    }

    static class TestConsumer<T> implements Consumer<T> {
        T value;

        @Override
        public void accept(T t) {
            value = t;
        }
    }

    static class AwaitableTestConsumer<T> implements Consumer<T> {
        T value;
        CountDownLatch countDownLatch = new CountDownLatch(1);

        @Override
        public void accept(T t) {
            value = t;
            countDownLatch.countDown();
        }

        void await() {
            try {
                assertTrue(countDownLatch.await(3000, TimeUnit.MILLISECONDS), "Timeout");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        T awaitValue() {
            await();
            return value;
        }
    }
}
