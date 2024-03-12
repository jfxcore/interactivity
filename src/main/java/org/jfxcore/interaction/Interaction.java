/*
 * Copyright (c) 2023, 2024, JFXcore. All rights reserved.
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
import javafx.collections.ModifiableObservableListBase;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.util.Subscription;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;

/**
 * {@code Interaction} provides a way for software components to initiate an interaction with other
 * software components, which may involve presenting a dialog window or control to the user.
 * <p>
 * An interaction is requested by calling {@link #request} or {@link #requestAndWait}, after which an
 * {@link InteractionListener} can accept the request and complete it by returning a response or an
 * exception. In most cases, the interaction will be initiated in a controller or view model component,
 * while the response will be handled in a view component.
 * <p>
 * For example, a view model component may want to confirm the deletion of a file with the user:
 * <pre>{@code
 * import java.nio.file.Files;
 * import java.nio.file.Path;
 * import javafx.scene.control.ButtonType;
 * import javafx.scene.control.Dialog;
 * import javafx.scene.layout.StackPane;
 *
 * public class ViewModel {
 *     public final Interaction<Path, Boolean> confirmDelete = new Interaction<>();
 *
 *     // This method might be called when the user clicks on a 'delete' button.
 *     // An application can use the Command class to model user-initiated commands.
 *     public void deleteFile(Path file) {
 *         if (confirmDelete.requestAndWait(file)) {
 *             Files.delete(file);
 *         }
 *     }
 * }
 *
 * public class View extends StackPane {
 *     private final ViewModel vm = new ViewModel();
 *
 *     public View() {
 *         vm.confirmDelete.addListener(request -> {
 *             var dialog = new Dialog<>();
 *             dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
 *             dialog.setContentText("Do you want to delete the following file: " + request.getPayload());
 *
 *             // Show the dialog and get the result
 *             var response = dialog.showAndWait().orElse(ButtonType.CANCEL);
 *
 *             // Complete the request
 *             request.complete(response == ButtonType.OK);
 *
 *             // We need to return true, since we already completed the request
 *             return true;
 *         });
 *     }
 * }
 * }</pre>
 * <p>
 * Interaction listeners will be invoked in reverse order, starting with the most recently added listener.
 * Once a listener accepts the request, subsequent listeners will not be invoked.
 *
 * @param <P> the payload type
 * @param <R> the response type
 */
public final class Interaction<P, R> {

    private final List<InteractionListener<P, R>> listeners = new ArrayList<>(2);
    private List<InteractionListener<P, R>> toBeAdded;
    private List<InteractionListener<P, R>> toBeRemoved;
    private int lockCount;

    /**
     * Creates a new {@code Interaction} instance.
     */
    public Interaction() {}

    /**
     * Requests an interaction with the specified payload.
     *
     * @param payload the request payload
     * @throws UnhandledInteractionException if no listener accepted the request
     * @return the {@code InteractionRequest}
     */
    public InteractionRequest<P, R> request(P payload) {
        var request = new InteractionRequestBase.Default<>(this, payload);
        handleRequest(request);
        return request;
    }

    /**
     * Requests an interaction with the specified payload, and waits for the response.
     * <p>
     * If this method is called on the JavaFX application thread, a nested event loop may be
     * started if the interaction is not immediately completed. This allows the application
     * to remain responsive while waiting for the interaction to be completed or cancelled.
     * <p>
     * If this method is called on any other thread, the current thread will be blocked until
     * the interaction is completed or cancelled.
     *
     * @param payload the request payload
     * @throws UnhandledInteractionException if no listener accepted the request
     * @throws InteractionException if the request is completed with an exception
     * @throws CancellationException if the request was cancelled
     * @return the response
     */
    public R requestAndWait(P payload) {
        InteractionRequestBase<P, R> request = Platform.isFxApplicationThread() ?
            new InteractionRequestBase.AwaitableEventLoop<>(this, payload) :
            new InteractionRequestBase.AwaitableMonitor<>(this, payload);

        handleRequest(request);
        request.await();

        if (request.isCancelled()) {
            throw new CancellationException();
        } else if (request.getException() != null) {
            throw new InteractionException(
                "The interaction request was completed with an exception.",
                request.getException(),
                request);
        }

        return request.getResponse();
    }

    private void handleRequest(InteractionRequestBase<P, R> request) {
        synchronized (listeners) {
            try {
                lockCount++;

                for (int i = listeners.size() - 1; i >= 0; --i) {
                    if (listeners.get(i).accept(request) || request.isDone()) {
                        return;
                    }
                }

                throw new UnhandledInteractionException("No listener accepted the interaction request.", request);
            } finally {
                lockCount--;

                if (toBeRemoved != null) {
                    for (var listener : toBeRemoved) {
                        removeListenerFromList(listeners, listener);
                    }

                    toBeRemoved = null;
                }

                if (toBeAdded != null) {
                    for (var listener : toBeAdded) {
                        removeListenerFromList(listeners, listener);
                    }

                    toBeAdded = null;
                }
            }
        }
    }

    /**
     * Subscribes a listener to this interaction.
     *
     * @param listener the {@code InteractionListener}
     * @return the {@code Subscription} that can be used to unsubscribe the listener
     */
    public Subscription subscribe(InteractionListener<P, R> listener) {
        addListener(listener);
        return () -> removeListener(listener);
    }

    /**
     * Adds a listener to this interaction.
     *
     * @param listener the {@code InteractionListener}
     */
    public void addListener(InteractionListener<P, R> listener) {
        Objects.requireNonNull(listener, "listener cannot be null");

        synchronized (listeners) {
            if (lockCount > 0) {
                if (toBeRemoved == null || !removeListenerFromList(toBeRemoved, listener)) {
                    if (toBeAdded == null) {
                        toBeAdded = new ArrayList<>(2);
                    }

                    toBeAdded.add(listener);
                }
            } else {
                listeners.add(listener);
            }
        }
    }

    /**
     * Removes a listener from this interaction.
     * If the specified listener was not added to this interaction, calling this method has no effect.
     *
     * @param listener the {@code InteractionListener}
     */
    public void removeListener(InteractionListener<P, R> listener) {
        Objects.requireNonNull(listener, "listener cannot be null");

        synchronized (listeners) {
            if (lockCount > 0) {
                if (toBeAdded == null || !removeListenerFromList(toBeAdded, listener)) {
                    if (toBeRemoved == null) {
                        toBeRemoved = new ArrayList<>(2);
                    }

                    toBeRemoved.add(listener);
                }
            } else {
                removeListenerFromList(listeners, listener);
            }
        }
    }

    private boolean removeListenerFromList(List<InteractionListener<P, R>> list,
                                           InteractionListener<P, R> listener) {
        for (int i = list.size() - 1; i >= 0; --i) {
            if (listener.equals(list.get(i))) {
                list.remove(i);
                return true;
            }
        }

        return false;
    }

    /**
     * Stores behavior lists for non-{@code Node} owners.
     */
    private static final Map<Object, ObservableList<?>> behaviorLists = new WeakHashMap<>();

    /**
     * Gets the {@link Behavior behaviors} of the specified {@code owner}.
     *
     * @param owner the owner of the behaviors
     * @return a modifiable list of behaviors
     * @param <T> the owner type
     */
    @SuppressWarnings("unchecked")
    public static <T> ObservableList<Behavior<? super T>> getBehaviors(T owner) {
        Objects.requireNonNull(owner, "owner cannot be null");

        if (owner instanceof Node node) {
            BehaviorList<T, Behavior<? super T>> list =
                (BehaviorList<T, Behavior<? super T>>)node.getProperties().get(BehaviorList.class);

            if (list == null) {
                list = new BehaviorList<>((T)node);
                node.getProperties().put(BehaviorList.class, list);
            }

            return list;
        }

        synchronized (behaviorLists) {
            return (ObservableList<Behavior<? super T>>) behaviorLists
                    .computeIfAbsent(owner, key -> new BehaviorList<>(owner));
        }
    }

    /**
     * Stores trigger lists for non-{@code Node} owners.
     */
    private static final Map<Object, ObservableList<?>> triggerLists = new WeakHashMap<>();

    /**
     * Gets the {@link Trigger triggers} of the specified {@code owner}.
     *
     * @param owner the owner of the triggers
     * @return a modifiable list of triggers
     * @param <T> the owner type
     */
    @SuppressWarnings("unchecked")
    public static <T> ObservableList<Trigger<? super T, ?>> getTriggers(T owner) {
        Objects.requireNonNull(owner, "owner cannot be null");

        if (owner instanceof Node node) {
            TriggerList<T, Trigger<? super T, ?>> list =
                (TriggerList<T, Trigger<? super T, ?>>)node.getProperties().get(TriggerList.class);

            if (list == null) {
                list = new TriggerList<>((T)node);
                node.getProperties().put(TriggerList.class, list);
            }

            return list;
        }

        synchronized (triggerLists) {
            return (ObservableList<Trigger<? super T, ?>>) triggerLists
                    .computeIfAbsent(owner, key -> new TriggerList<>(owner));
        }
    }

    private static abstract class AttachableList<T, U extends Attachable<? super T>>
            extends ModifiableObservableListBase<U> {
        final List<U> backingList;
        final T owner;

        AttachableList(T owner) {
            this.backingList = new ArrayList<>(2);
            this.owner = owner;
        }

        @Override
        public U get(int index) {
            return backingList.get(index);
        }

        @Override
        public int size() {
            return backingList.size();
        }

        @Override
        protected void doAdd(int index, U element) {
            checkPreconditions(element);
            backingList.add(index, element);
            element.associatedObject = owner;

            try {
                element.attach(owner);
            } catch (Throwable ex) {
                Thread currentThread = Thread.currentThread();
                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
            }
        }

        @Override
        protected U doSet(int index, U element) {
            checkPreconditions(element);
            U oldElement = backingList.set(index, element);
            Throwable exception = null;

            try {
                oldElement.detach(owner);
            } catch (Throwable ex) {
                exception = ex;
            }

            oldElement.associatedObject = null;
            element.associatedObject = owner;

            try {
                element.attach(owner);
            } catch (Throwable ex) {
                if (exception != null) {
                    ex.addSuppressed(exception);
                }

                exception = ex;
            }

            if (exception != null) {
                Thread currentThread = Thread.currentThread();
                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, exception);
            }

            return oldElement;
        }

        @Override
        protected U doRemove(int index) {
            U oldElement = backingList.remove(index);

            try {
                oldElement.detach(owner);
            } catch (Throwable ex) {
                Thread currentThread = Thread.currentThread();
                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
            } finally {
                oldElement.associatedObject = null;
            }

            return oldElement;
        }

        abstract String elementName();

        private void checkPreconditions(U element) {
            if (element == null) {
                throw new NullPointerException(
                    elementName() + " cannot be null.");
            }

            if (element.associatedObject == owner) {
                throw new IllegalStateException(
                    elementName() + " cannot be attached to the same object more than once.");
            }

            if (element.associatedObject != null) {
                throw new IllegalStateException(
                    elementName() + " cannot be attached to multiple objects.");
            }
        }
    }

    private static class BehaviorList<T, U extends Behavior<? super T>> extends AttachableList<T, U> {
        BehaviorList(T owner) {
            super(owner);
        }

        @Override
        String elementName() {
            return Behavior.class.getSimpleName();
        }

        @Override
        public String toString() {
            return BehaviorList.class.getName();
        }
    }

    private static class TriggerList<T, U extends Trigger<? super T, ?>> extends AttachableList<T, U> {
        TriggerList(T owner) {
            super(owner);
        }

        @Override
        String elementName() {
            return Trigger.class.getSimpleName();
        }

        @Override
        public String toString() {
            return TriggerList.class.getName();
        }
    }

}
