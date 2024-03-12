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

import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

abstract sealed class InteractionRequestBase<P, R>
        implements InteractionRequest<P, R>
        permits InteractionRequestBase.Default,
                InteractionRequestBase.AwaitableEventLoop,
                InteractionRequestBase.AwaitableMonitor {
    enum State {
        RUNNING,
        COMPLETED,
        COMPLETED_EXCEPTIONALLY,
        CANCELLED
    }

    private final P payload;
    private final Interaction<P, R> interaction;
    private List<Runnable> cancellationActions;
    private List<Consumer<R>> completionActions;
    private List<Consumer<Throwable>> exceptionalCompletionActions;
    private State state = State.RUNNING;
    private R response;
    private Throwable exception;

    InteractionRequestBase(Interaction<P, R> interaction, P payload) {
        this.interaction = interaction;
        this.payload = payload;
    }

    @Override
    public final Interaction<P, R> getInteraction() {
        return interaction;
    }

    @Override
    public final P getPayload() {
        return payload;
    }

    final R getResponse() {
        return response;
    }

    final Throwable getException() {
        return exception;
    }

    @Override
    public final synchronized boolean isDone() {
        return state != State.RUNNING;
    }

    @Override
    public final synchronized boolean isCompleted() {
        return state == State.COMPLETED;
    }

    final synchronized boolean setCompleted(R value) {
        if (state != State.RUNNING) {
            return false;
        }

        state = State.COMPLETED;
        response = value;

        if (completionActions != null) {
            completionActions.forEach(action -> action.accept(response));
        }

        return true;
    }

    @Override
    public final synchronized boolean isCompletedExceptionally() {
        return state == State.COMPLETED_EXCEPTIONALLY;
    }

    final synchronized boolean setCompletedExceptionally(Throwable ex) {
        if (state != State.RUNNING) {
            return false;
        }

        state = State.COMPLETED_EXCEPTIONALLY;
        exception = ex;

        if (exceptionalCompletionActions != null) {
            exceptionalCompletionActions.forEach(action -> action.accept(exception));
        }

        return true;
    }

    @Override
    public void cancel() {
        setCancelled();
    }

    @Override
    public final synchronized boolean isCancelled() {
        return state == State.CANCELLED;
    }

    final synchronized boolean setCancelled() {
        if (state != State.RUNNING) {
            return false;
        }

        state = State.CANCELLED;

        if (cancellationActions != null) {
            cancellationActions.forEach(Runnable::run);
        }

        return true;
    }

    @Override
    public final synchronized InteractionRequest<P, R> whenDone(Runnable action) {
        return whenCancelled(action)
            .whenCompleted(result -> action.run())
            .whenCompletedExceptionally(exception -> action.run());
    }

    @Override
    public final synchronized InteractionRequest<P, R> whenCancelled(Runnable action) {
        if (state == State.CANCELLED) {
            action.run();
        } else if (state == State.RUNNING) {
            if (cancellationActions == null) {
                cancellationActions = new ArrayList<>(2);
            }

            cancellationActions.add(action);
        }

        return this;
    }

    @Override
    public final synchronized InteractionRequest<P, R> whenCompleted(Consumer<R> action) {
        if (state == State.COMPLETED) {
            action.accept(response);
        } else if (state == State.RUNNING) {
            if (completionActions == null) {
                completionActions = new ArrayList<>(2);
            }

            completionActions.add(action);
        }

        return this;
    }

    @Override
    public final synchronized InteractionRequest<P, R> whenCompletedExceptionally(Consumer<Throwable> action) {
        if (state == State.COMPLETED_EXCEPTIONALLY) {
            action.accept(exception);
        } else if (state == State.RUNNING) {
            if (exceptionalCompletionActions == null) {
                exceptionalCompletionActions = new ArrayList<>(2);
            }

            exceptionalCompletionActions.add(action);
        }

        return this;
    }

    void await() {}

    static final class Default<P, R> extends InteractionRequestBase<P, R> {
        Default(Interaction<P, R> interaction, P payload) {
            super(interaction, payload);
        }

        @Override
        public void complete(R response) {
            setCompleted(response);
        }

        @Override
        public void completeExceptionally(Throwable cause) {
            setCompletedExceptionally(cause);
        }
    }

    static final class AwaitableEventLoop<P, R> extends InteractionRequestBase<P, R> {
        private boolean nestedEventLoopStarted;

        AwaitableEventLoop(Interaction<P, R> interaction, P payload) {
            super(interaction, payload);
        }

        @Override
        void await() {
            if (!isDone()) {
                nestedEventLoopStarted = true;
                Platform.enterNestedEventLoop(this);
            }
        }

        @Override
        public void complete(R response) {
            if (setCompleted(response)) {
                if (Platform.isFxApplicationThread()) {
                    completeImpl();
                } else {
                    Platform.runLater(this::completeImpl);
                }
            }
        }

        @Override
        public void completeExceptionally(Throwable cause) {
            if (setCompletedExceptionally(cause)) {
                if (Platform.isFxApplicationThread()) {
                    completeImpl();
                } else {
                    Platform.runLater(this::completeImpl);
                }
            }
        }

        private void completeImpl() {
            if (nestedEventLoopStarted) {
                Platform.exitNestedEventLoop(this, null);
            }
        }
    }

    static final class AwaitableMonitor<P, R> extends InteractionRequestBase<P, R> {
        AwaitableMonitor(Interaction<P, R> interaction, P payload) {
            super(interaction, payload);
        }

        @Override
        synchronized void await() {
            if (isDone()) {
                return;
            }

            while (!isDone()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new InteractionException("Interaction was interrupted", e, this);
                }
            }
        }

        @Override
        public synchronized void cancel() {
            if (setCancelled()) {
                notifyAll();
            }
        }

        @Override
        public synchronized void complete(R response) {
            if (setCompleted(response)) {
                notifyAll();
            }
        }

        @Override
        public synchronized void completeExceptionally(Throwable cause) {
            if (setCompletedExceptionally(cause)) {
                notifyAll();
            }
        }
    }
}