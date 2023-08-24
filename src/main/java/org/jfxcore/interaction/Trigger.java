/*
 * Copyright (c) 2023, JFXcore. All rights reserved.
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

import javafx.beans.DefaultProperty;
import javafx.collections.ModifiableObservableListBase;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code Trigger} is the base class for reusable components that extend scene graph nodes with the
 * capability to run {@link TriggerAction actions} when a condition is met or an event is received.
 * <p>
 * A trigger is attached to a scene graph node by adding it to the node's {@link Interaction#getTriggers}.
 * <p>
 * Implementations can override the {@link #onAttached} and {@link #onDetached} methods to run custom
 * code, install listeners, or configure the associated node.
 *
 * @param <T> the node type
 */
@DefaultProperty("actions")
public abstract non-sealed class Trigger<T extends Node> extends Attachable<T> {

    private final ActionList<T> actions;

    /**
     * Initializes a new {@code Trigger} instance.
     */
    protected Trigger() {
        this.actions = new ActionList<>(this, new ArrayList<>(2));
    }

    /**
     * Initializes a new {@code Trigger} instance.
     *
     * @param actions the actions
     */
    @SafeVarargs
    protected Trigger(TriggerAction<? super T>... actions) {
        this.actions = new ActionList<>(this, List.of(actions));
    }

    /**
     * Gets the associated node.
     *
     * @return the associated node, or {@code null} if this trigger is not associated with a node
     */
    @Override
    public final T getAssociatedNode() {
        return super.getAssociatedNode();
    }

    /**
     * Gets a modifiable list of {@link TriggerAction} instances defined for this trigger.
     *
     * @return an {@code ObservableList} of {@code TriggerAction} instances
     */
    public final ObservableList<TriggerAction<? super T>> getActions() {
        return actions;
    }

    private static class ActionList<T extends Node> extends ModifiableObservableListBase<TriggerAction<? super T>> {
        final List<TriggerAction<? super T>> backingList;
        final Trigger<T> trigger;

        ActionList(Trigger<T> trigger, List<TriggerAction<? super T>> backingList) {
            this.trigger = trigger;
            this.backingList = backingList;
        }

        @Override
        public TriggerAction<? super T> get(int index) {
            return backingList.get(index);
        }

        @Override
        public int size() {
            return backingList.size();
        }

        @Override
        protected void doAdd(int index, TriggerAction<? super T> element) {
            checkPreconditions(element);
            backingList.add(index, element);
            element.associatedTrigger = trigger;
            T node = trigger.associatedNode;

            if (node != null) {
                try {
                    element.onAttached(node);
                } catch (Throwable ex) {
                    Thread currentThread = Thread.currentThread();
                    currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
                }
            }
        }

        @Override
        protected TriggerAction<? super T> doSet(int index, TriggerAction<? super T> element) {
            checkPreconditions(element);
            TriggerAction<? super T> oldElement = backingList.set(index, element);
            T node = trigger.associatedNode;
            if (node == null) {
                return oldElement;
            }

            Throwable exception = null;

            try {
                oldElement.onDetached(node);
            } catch (Throwable ex) {
                exception = ex;
            }

            oldElement.associatedTrigger = null;
            element.associatedTrigger = trigger;

            try {
                element.onAttached(node);
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
        protected TriggerAction<? super T> doRemove(int index) {
            TriggerAction<? super T> oldAction = backingList.remove(index);
            T node = trigger.associatedNode;
            try {
                if (node != null) {
                    oldAction.onDetached(node);
                }
            } catch (Throwable ex) {
                Thread currentThread = Thread.currentThread();
                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
            } finally {
                oldAction.associatedTrigger = null;
            }

            return oldAction;
        }

        private void checkPreconditions(TriggerAction<?> action) {
            if (action == null) {
                throw new NullPointerException(
                    TriggerAction.class.getSimpleName() + " cannot be null.");
            }

            if (action.associatedTrigger == trigger) {
                throw new IllegalStateException(
                    TriggerAction.class.getSimpleName() + "  cannot be added to the same trigger more than once.");
            }

            if (action.associatedTrigger != null) {
                throw new IllegalStateException(
                    TriggerAction.class.getSimpleName() + " cannot be added to multiple triggers.");
            }
        }
    }

    /**
     * Occurs when the trigger is attached to a node.
     *
     * @param node the node
     */
    protected void onAttached(T node) {}

    /**
     * Occurs when the trigger is detached from a node.
     *
     * @param node the node
     */
    protected void onDetached(T node) {}

    @Override
    final void attach(T node) {
        onAttached(node);

        for (TriggerAction<? super T> action : actions) {
            try {
                action.onAttached(node);
            } catch (Throwable ex) {
                Thread currentThread = Thread.currentThread();
                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
            }
        }
    }

    @Override
    final void detach(T node) {
        onDetached(node);

        for (TriggerAction<? super T> action : actions) {
            try {
                action.onDetached(node);
            } catch (Throwable ex) {
                Thread currentThread = Thread.currentThread();
                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
            }
        }
    }

}
