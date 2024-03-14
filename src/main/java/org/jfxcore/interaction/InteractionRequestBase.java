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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

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

    private static abstract class CompletionAction<R> {
        final Executor executor;

        private CompletionAction(Executor executor) {
            this.executor = executor;
        }

        abstract void complete(R value, Throwable exception);
        abstract void cancel();
    }

    private static class BiResult<R1, R2> {
        R1 value1;
        R2 value2;
        Throwable exception1;
        Throwable exception2;
    }

    private static class MutableInt {
        int value;
    }

    private final P payload;
    private final Interaction<P, R> interaction;
    private List<CompletionAction<R>> completionActions;
    private State state = State.RUNNING;
    private R response;
    private Throwable exception;

    InteractionRequestBase(Interaction<P, R> interaction, P payload) {
        this.interaction = interaction;
        this.payload = payload;
    }

    Executor defaultExecutor() {
        return ForkJoinPool.commonPool();
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
        runCompletionActions();
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
        runCompletionActions();
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
        runCompletionActions();
        return true;
    }

    @Override
    public <U> CompletionStage<U> thenApply(Function<? super R, ? extends U> fn) {
        return thenApplyImpl(fn, null);
    }

    @Override
    public <U> CompletionStage<U> thenApplyAsync(Function<? super R, ? extends U> fn) {
        return thenApplyImpl(fn, defaultExecutor());
    }

    @Override
    public <U> CompletionStage<U> thenApplyAsync(Function<? super R, ? extends U> fn, Executor executor) {
        return thenApplyImpl(fn, Objects.requireNonNull(executor, "executor cannot be null"));
    }

    private <U> CompletionStage<U> thenApplyImpl(Function<? super R, ? extends U> fn, Executor executor) {
        var newStage = new CompletableFuture<U>();

        addCompletionAction(new CompletionAction<>(executor) {
            @Override
            void complete(R value, Throwable exception) {
                if (exception != null) {
                    newStage.completeExceptionally(exception);
                } else {
                    try {
                        newStage.complete(fn.apply(value));
                    } catch (Throwable ex) {
                        newStage.completeExceptionally(ex);
                    }
                }
            }

            @Override
            void cancel() {
                newStage.cancel(false);
            }
        });

        return newStage;
    }

    @Override
    public CompletionStage<Void> thenAccept(Consumer<? super R> action) {
        return thenAcceptImpl(action, null);
    }

    @Override
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super R> action) {
        return thenAcceptImpl(action, defaultExecutor());
    }

    @Override
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super R> action, Executor executor) {
        return thenAcceptImpl(action, Objects.requireNonNull(executor, "executor cannot be null"));
    }

    private synchronized CompletionStage<Void> thenAcceptImpl(Consumer<? super R> action, Executor executor) {
        var newStage = new CompletableFuture<Void>();

        addCompletionAction(new CompletionAction<>(executor) {
            @Override
            void complete(R value, Throwable exception) {
                if (exception != null) {
                    newStage.completeExceptionally(exception);
                } else {
                    try {
                        action.accept(value);
                        newStage.complete(null);
                    } catch (Throwable ex) {
                        newStage.completeExceptionally(ex);
                    }
                }
            }

            @Override
            void cancel() {
                newStage.cancel(false);
            }
        });

        return newStage;
    }

    @Override
    public CompletionStage<Void> thenRun(Runnable action) {
        return thenRunImpl(action, null);
    }

    @Override
    public CompletionStage<Void> thenRunAsync(Runnable action) {
        return thenRunImpl(action, defaultExecutor());
    }

    @Override
    public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
        return thenRunImpl(action, Objects.requireNonNull(executor, "executor cannot be null"));
    }

    private synchronized CompletionStage<Void> thenRunImpl(Runnable action, Executor executor) {
        var newStage = new CompletableFuture<Void>();

        addCompletionAction(new CompletionAction<>(executor) {
            @Override
            void complete(R value, Throwable exception) {
                if (exception != null) {
                    newStage.completeExceptionally(exception);
                } else {
                    try {
                        action.run();
                        newStage.complete(null);
                    } catch (Throwable ex) {
                        newStage.completeExceptionally(ex);
                    }
                }
            }

            @Override
            void cancel() {
                newStage.cancel(false);
            }
        });

        return newStage;
    }

    @Override
    public <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
                                                 BiFunction<? super R, ? super U, ? extends V> fn) {
        return thenCombineImpl(other, fn, null);
    }

    @Override
    public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                      BiFunction<? super R, ? super U, ? extends V> fn) {
        return thenCombineImpl(other, fn, defaultExecutor());
    }

    @Override
    public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                      BiFunction<? super R, ? super U, ? extends V> fn,
                                                      Executor executor) {
        return thenCombineImpl(other, fn, Objects.requireNonNull(executor, "executor cannot be null"));
    }

    private synchronized <U, V> CompletionStage<V> thenCombineImpl(CompletionStage<? extends U> other,
                                                                   BiFunction<? super R, ? super U, ? extends V> fn,
                                                                   Executor executor) {
        var newStage = new CompletableFuture<V>();
        var result = new BiResult<R, U>();
        var count = new MutableInt();

        other.whenCompleteAsync((value, ex) -> {
            synchronized (result) {
                result.value2 = value;
                result.exception2 = ex;

                if (++count.value == 2) {
                    thenCombineImplResult(newStage, fn, result);
                }
            }
        }, Objects.requireNonNullElse(executor, Runnable::run));

        addCompletionAction(new CompletionAction<>(executor) {
            @Override
            void complete(R value, Throwable exception) {
                synchronized (result) {
                    result.value1 = value;
                    result.exception1 = exception;

                    if (++count.value == 2) {
                        thenCombineImplResult(newStage, fn, result);
                    }
                }
            }

            @Override
            void cancel() {
                newStage.cancel(false);
            }
        });

        return newStage;
    }

    private <V, R1, R2> void thenCombineImplResult(CompletableFuture<V> newStage,
                                                   BiFunction<? super R1, ? super R2, ? extends V> fn,
                                                   BiResult<R1, R2> result) {
        if (result.exception1 != null) {
            newStage.completeExceptionally(result.exception1);
        } else if (result.exception2 != null) {
            newStage.completeExceptionally(result.exception2);
        } else {
            try {
                newStage.complete(fn.apply(result.value1, result.value2));
            } catch (Throwable ex) {
                newStage.completeExceptionally(ex);
            }
        }
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
                                                    BiConsumer<? super R, ? super U> action) {
        return thenAcceptBothImpl(other, action, null);
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                                                         BiConsumer<? super R, ? super U> action) {
        return thenAcceptBothImpl(other, action, defaultExecutor());
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                                                         BiConsumer<? super R, ? super U> action,
                                                         Executor executor) {
        return thenAcceptBothImpl(other, action, Objects.requireNonNull(executor, "executor cannot be null"));
    }

    private synchronized <U> CompletionStage<Void> thenAcceptBothImpl(CompletionStage<? extends U> other,
                                                                      BiConsumer<? super R, ? super U> action,
                                                                      Executor executor) {
        var newStage = new CompletableFuture<Void>();
        var result = new BiResult<R, U>();
        var count = new MutableInt();

        other.whenCompleteAsync((value, ex) -> {
            synchronized (result) {
                result.value2 = value;
                result.exception2 = ex;

                if (++count.value == 2) {
                    thenAcceptBothImplResult(newStage, action, result);
                }
            }
        }, Objects.requireNonNullElse(executor, Runnable::run));

        addCompletionAction(new CompletionAction<>(executor) {
            @Override
            void complete(R value, Throwable exception) {
                synchronized (result) {
                    result.value1 = value;
                    result.exception1 = exception;

                    if (++count.value == 2) {
                        thenAcceptBothImplResult(newStage, action, result);
                    }
                }
            }

            @Override
            void cancel() {
                newStage.cancel(false);
            }
        });

        return newStage;
    }

    private <U> void thenAcceptBothImplResult(CompletableFuture<Void> newStage,
                                              BiConsumer<? super R, ? super U> action,
                                              BiResult<R, U> result) {
        if (result.exception1 != null) {
            newStage.completeExceptionally(result.exception1);
        } else if (result.exception2 != null) {
            newStage.completeExceptionally(result.exception2);
        } else {
            try {
                action.accept(result.value1, result.value2);
                newStage.complete(null);
            } catch (Throwable ex) {
                newStage.completeExceptionally(ex);
            }
        }
    }

    @Override
    public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return runAfterBothImpl(other, action, null);
    }

    @Override
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return runAfterBothImpl(other, action, defaultExecutor());
    }

    @Override
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return runAfterBothImpl(other, action, Objects.requireNonNull(executor, "executor cannot be null"));
    }

    private synchronized CompletionStage<Void> runAfterBothImpl(CompletionStage<?> other,
                                                                Runnable action,
                                                                Executor executor) {
        var newStage = new CompletableFuture<Void>();
        var result = new BiResult<>();
        var count = new MutableInt();

        other.whenCompleteAsync((value, ex) -> {
            synchronized (result) {
                result.exception2 = ex;

                if (++count.value == 2) {
                    runAfterBothImplResult(newStage, action, result);
                }
            }
        }, Objects.requireNonNullElse(executor, Runnable::run));

        addCompletionAction(new CompletionAction<>(executor) {
            @Override
            void complete(R value, Throwable exception) {
                synchronized (result) {
                    result.exception1 = exception;

                    if (++count.value == 2) {
                        runAfterBothImplResult(newStage, action, result);
                    }
                }
            }

            @Override
            void cancel() {
                newStage.cancel(false);
            }
        });

        return newStage;
    }

    private void runAfterBothImplResult(CompletableFuture<Void> newStage,
                                        Runnable action,
                                        BiResult<Object, Object> result) {
        if (result.exception1 != null) {
            newStage.completeExceptionally(result.exception1);
        } else if (result.exception2 != null) {
            newStage.completeExceptionally(result.exception2);
        } else {
            try {
                action.run();
                newStage.complete(null);
            } catch (Throwable ex) {
                newStage.completeExceptionally(ex);
            }
        }
    }

    @Override
    public <U> CompletionStage<U> applyToEither(CompletionStage<? extends R> other, Function<? super R, U> fn) {
        return applyToEitherImpl(other, fn, null);
    }

    @Override
    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends R> other, Function<? super R, U> fn) {
        return applyToEitherImpl(other, fn, defaultExecutor());
    }

    @Override
    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends R> other,
                                                     Function<? super R, U> fn,
                                                     Executor executor) {
        return applyToEitherImpl(other, fn, Objects.requireNonNull(executor, "executor cannot be null"));
    }

    private synchronized <U> CompletionStage<U> applyToEitherImpl(CompletionStage<? extends R> other,
                                                                  Function<? super R, U> fn,
                                                                  Executor executor) {
        var newStage = new CompletableFuture<U>();
        var result = new BiResult<R, R>();
        var count = new MutableInt();

        other.whenCompleteAsync((value, ex) -> {
            synchronized (result) {
                result.value2 = value;
                result.exception2 = ex;

                if (result.exception1 != null || ++count.value == 2) {
                    applyToEitherImplResult(newStage, fn, result);
                }
            }
        }, Objects.requireNonNullElse(executor, Runnable::run));

        addCompletionAction(new CompletionAction<>(executor) {
            @Override
            void complete(R value, Throwable exception) {
                synchronized (result) {
                    result.value1 = value;
                    result.exception1 = exception;

                    if (result.exception2 != null || ++count.value == 2) {
                        applyToEitherImplResult(newStage, fn, result);
                    }
                }
            }

            @Override
            void cancel() {
                newStage.cancel(false);
            }
        });

        return newStage;
    }

    private <U> void applyToEitherImplResult(CompletableFuture<U> newStage,
                                             Function<? super R, U> fn,
                                             BiResult<R, R> result) {
        if (result.exception1 != null && result.exception2 != null) {
            newStage.completeExceptionally(result.exception1);
        } else {
            try {
                newStage.complete(fn.apply(result.exception1 != null ? result.value2 : result.value1));
            } catch (Throwable ex) {
                newStage.completeExceptionally(ex);
            }
        }
    }

    @Override
    public CompletionStage<Void> acceptEither(CompletionStage<? extends R> other, Consumer<? super R> action) {
        return acceptEitherImpl(other, action, null);
    }

    @Override
    public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends R> other, Consumer<? super R> action) {
        return acceptEitherImpl(other, action, defaultExecutor());
    }

    @Override
    public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends R> other,
                                                   Consumer<? super R> action,
                                                   Executor executor) {
        return acceptEitherImpl(other, action, Objects.requireNonNull(executor, "executor cannot be null"));
    }

    private synchronized CompletionStage<Void> acceptEitherImpl(CompletionStage<? extends R> other,
                                                                Consumer<? super R> action,
                                                                Executor executor) {
        var newStage = new CompletableFuture<Void>();
        var result = new BiResult<R, R>();
        var count = new MutableInt();

        other.whenCompleteAsync((value, ex) -> {
            synchronized (result) {
                result.value2 = value;
                result.exception2 = ex;

                if (result.exception1 != null || ++count.value == 2) {
                    acceptEitherImplResult(newStage, action, result);
                }
            }
        }, Objects.requireNonNullElse(executor, Runnable::run));

        addCompletionAction(new CompletionAction<>(executor) {
            @Override
            void complete(R value, Throwable exception) {
                synchronized (result) {
                    result.value1 = value;
                    result.exception1 = exception;

                    if (result.exception2 != null || ++count.value == 2) {
                        acceptEitherImplResult(newStage, action, result);
                    }
                }
            }

            @Override
            void cancel() {
                newStage.cancel(false);
            }
        });

        return newStage;
    }

    private void acceptEitherImplResult(CompletableFuture<Void> newStage,
                                        Consumer<? super R> action,
                                        BiResult<R, R> result) {
        if (result.exception1 != null && result.exception2 != null) {
            newStage.completeExceptionally(result.exception1);
        } else {
            try {
                action.accept(result.exception1 != null ? result.value2 : result.value1);
                newStage.complete(null);
            } catch (Throwable ex) {
                newStage.completeExceptionally(ex);
            }
        }
    }

    @Override
    public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return runAfterEitherImpl(other, action, null);
    }

    @Override
    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return runAfterEitherImpl(other, action, defaultExecutor());
    }

    @Override
    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return runAfterEitherImpl(other, action, Objects.requireNonNull(executor, "executor cannot be null"));
    }

    private synchronized CompletionStage<Void> runAfterEitherImpl(CompletionStage<?> other,
                                                                  Runnable action,
                                                                  Executor executor) {
        var newStage = new CompletableFuture<Void>();
        var result = new BiResult<>();
        var count = new MutableInt();

        other.whenCompleteAsync((value, ex) -> {
            synchronized (result) {
                result.exception2 = ex;

                if (result.exception1 != null || ++count.value == 2) {
                    runAfterEitherImplResult(newStage, action, result);
                }
            }
        }, Objects.requireNonNullElse(executor, Runnable::run));

        addCompletionAction(new CompletionAction<>(executor) {
            @Override
            void complete(R value, Throwable exception) {
                synchronized (result) {
                    result.exception1 = exception;

                    if (result.exception2 != null || ++count.value == 2) {
                        runAfterEitherImplResult(newStage, action, result);
                    }
                }
            }

            @Override
            void cancel() {
                newStage.cancel(false);
            }
        });

        return newStage;
    }

    private void runAfterEitherImplResult(CompletableFuture<Void> newStage,
                                          Runnable action,
                                          BiResult<Object, Object> result) {
        if (result.exception1 != null && result.exception2 != null) {
            newStage.completeExceptionally(result.exception1);
        } else {
            try {
                action.run();
                newStage.complete(null);
            } catch (Throwable ex) {
                newStage.completeExceptionally(ex);
            }
        }
    }

    @Override
    public <U> CompletionStage<U> thenCompose(Function<? super R, ? extends CompletionStage<U>> fn) {
        return thenComposeImpl(fn, null);
    }

    @Override
    public <U> CompletionStage<U> thenComposeAsync(Function<? super R, ? extends CompletionStage<U>> fn) {
        return thenComposeImpl(fn, defaultExecutor());
    }

    @Override
    public <U> CompletionStage<U> thenComposeAsync(Function<? super R, ? extends CompletionStage<U>> fn, Executor executor) {
        return thenComposeImpl(fn, Objects.requireNonNull(executor, "executor cannot be null"));
    }

    private synchronized <U> CompletionStage<U> thenComposeImpl(Function<? super R, ? extends CompletionStage<U>> fn,
                                                                Executor executor) {
        var newStage = new CompletableFuture<U>();
        var composeExecutor = executor;

        addCompletionAction(new CompletionAction<>(null) {
            @Override
            void complete(R value, Throwable exception) {
                if (exception != null) {
                    newStage.completeExceptionally(exception);
                } else {
                    try {
                        fn.apply(value).whenCompleteAsync((composeValue, composeException) -> {
                            if (composeException != null) {
                                newStage.completeExceptionally(composeException);
                            } else {
                                newStage.complete(composeValue);
                            }
                        }, Objects.requireNonNullElse(composeExecutor, Runnable::run));
                    } catch (Throwable ex) {
                        newStage.completeExceptionally(ex);
                    }
                }
            }

            @Override
            void cancel() {
                newStage.cancel(false);
            }
        });

        return newStage;
    }

    @Override
    public synchronized <U> CompletionStage<U> handle(BiFunction<? super R, Throwable, ? extends U> fn) {
        return handleImpl(fn, null);
    }

    @Override
    public <U> CompletionStage<U> handleAsync(BiFunction<? super R, Throwable, ? extends U> fn) {
        return handleImpl(fn, defaultExecutor());
    }

    @Override
    public <U> CompletionStage<U> handleAsync(BiFunction<? super R, Throwable, ? extends U> fn, Executor executor) {
        return handleImpl(fn, Objects.requireNonNull(executor, "executor cannot be null"));
    }

    private synchronized <U> CompletionStage<U> handleImpl(BiFunction<? super R, Throwable, ? extends U> fn,
                                                           Executor executor) {
        var newStage = new CompletableFuture<U>();

        addCompletionAction(new CompletionAction<>(executor) {
            @Override
            void complete(R value, Throwable exception) {
                if (isCompleted() || isCompletedExceptionally()) {
                    try {
                        newStage.complete(fn.apply(value, exception));
                    } catch (Throwable ex) {
                        newStage.completeExceptionally(ex);
                    }
                } else {
                    newStage.cancel(false);
                }
            }

            @Override
            void cancel() {
                newStage.cancel(false);
            }
        });

        return newStage;
    }

    @Override
    public CompletionStage<R> whenComplete(BiConsumer<? super R, ? super Throwable> action) {
        return whenCompleteImpl(action, null);
    }

    @Override
    public CompletionStage<R> whenCompleteAsync(BiConsumer<? super R, ? super Throwable> action) {
        return whenCompleteImpl(action, defaultExecutor());
    }

    @Override
    public CompletionStage<R> whenCompleteAsync(BiConsumer<? super R, ? super Throwable> action, Executor executor) {
        return whenCompleteImpl(action, Objects.requireNonNull(executor, "executor cannot be null"));
    }

    private synchronized CompletionStage<R> whenCompleteImpl(BiConsumer<? super R, ? super Throwable> action,
                                                             Executor executor) {
        var newStage = new CompletableFuture<R>();

        addCompletionAction(new CompletionAction<>(executor) {
            @Override
            void complete(R value, Throwable exception) {
                if (isCompleted()) {
                    try {
                        action.accept(value, exception);
                        newStage.complete(value);
                    } catch (Throwable ex) {
                        newStage.completeExceptionally(ex); // complete with action exception
                    }
                } else if (isCompletedExceptionally()) {
                    try {
                        action.accept(value, exception);
                        newStage.completeExceptionally(exception);
                    } catch (Throwable ex) {
                        newStage.completeExceptionally(exception); // complete with stage exception
                    }
                } else {
                    newStage.cancel(false);
                }
            }

            @Override
            void cancel() {
                newStage.cancel(false);
            }
        });

        return newStage;
    }

    @Override
    public synchronized CompletionStage<R> exceptionally(Function<Throwable, ? extends R> fn) {
        var newStage = new CompletableFuture<R>();

        addCompletionAction(new CompletionAction<>(null) {
            @Override
            void cancel() {
                newStage.cancel(false);
            }

            @Override
            void complete(R value, Throwable exception) {
                if (exception != null) {
                    try {
                        newStage.complete(fn.apply(exception));
                    } catch (Throwable ex) {
                        newStage.completeExceptionally(exception);
                    }
                } else {
                    newStage.complete(value);
                }
            }
        });

        return newStage;
    }

    @Override
    public CompletableFuture<R> toCompletableFuture() {
        return (CompletableFuture<R>)thenApply(x -> x);
    }

    void await() {}

    private void addCompletionAction(CompletionAction<R> action) {
        if (state == State.RUNNING) {
            if (completionActions == null) {
                completionActions = new ArrayList<>(2);
            }

            completionActions.add(action);
        } else if (state == State.CANCELLED) {
            action.cancel();
        } else {
            if (action.executor != null) {
                action.executor.execute(() -> action.complete(response, exception));
            } else {
                action.complete(response, exception);
            }
        }
    }

    private void runCompletionActions() {
        if (completionActions == null) {
            return;
        }

        if (state == State.CANCELLED) {
            for (CompletionAction<R> action : completionActions) {
                action.cancel();
            }
        } else {
            for (CompletionAction<R> action : completionActions) {
                if (action.executor != null) {
                    action.executor.execute(() -> action.complete(response, exception));
                } else {
                    action.complete(response, exception);
                }
            }
        }
    }

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
                    Thread.interrupted(); // clear the interrupted status
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
