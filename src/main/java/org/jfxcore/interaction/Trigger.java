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
 * {@code Trigger} is the base class for reusable components that extend JavaFX entities with the
 * capability to run {@link TriggerAction actions} when the trigger is released. This happens in
 * an implementation-defined way; for example, an {@link EventTrigger} releases when an event is
 * received.
 * <p>
 * A trigger is attached to a JavaFX entity by adding it to its triggers list, which can be
 * retrieved with the {@link Interaction#getTriggers} method.
 * <p>
 * Implementations can override the {@link #onAttached} and {@link #onDetached} methods to run
 * custom code, install listeners, or configure the associated entity.
 *
 * @param <T> the target of this trigger, for example a {@link Node}
 * @param <P> the type of the parameter that this trigger provides to its actions
 */
@DefaultProperty("actions")
public abstract non-sealed class Trigger<T, P> extends Attachable<T> {

    private final ActionList<T, P> actions;

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
    protected Trigger(TriggerAction<? super T, ? super P>... actions) {
        this.actions = new ActionList<>(this, List.of(actions));
    }

    /**
     * Gets the associated object.
     *
     * @return the associated object, or {@code null} if this trigger is not associated with an object
     */
    @Override
    public final T getAssociatedObject() {
        return super.getAssociatedObject();
    }

    /**
     * Gets a modifiable list of {@link TriggerAction} instances defined for this trigger.
     *
     * @return an {@code ObservableList} of {@code TriggerAction} instances
     */
    public final ObservableList<TriggerAction<? super T, ? super P>> getActions() {
        return actions;
    }

    private static class ActionList<T, P> extends ModifiableObservableListBase<TriggerAction<? super T, ? super P>> {
        final List<TriggerAction<? super T, ? super P>> backingList;
        final Trigger<T, P> trigger;

        ActionList(Trigger<T, P> trigger, List<TriggerAction<? super T, ? super P>> backingList) {
            this.trigger = trigger;
            this.backingList = backingList;
        }

        @Override
        public TriggerAction<? super T, ? super P> get(int index) {
            return backingList.get(index);
        }

        @Override
        public int size() {
            return backingList.size();
        }

        @Override
        protected void doAdd(int index, TriggerAction<? super T, ? super P> element) {
            checkPreconditions(element);
            backingList.add(index, element);
            element.associatedTrigger = trigger;
            T associatedObject = trigger.associatedObject;

            if (associatedObject != null) {
                try {
                    element.onAttached(associatedObject);
                } catch (Throwable ex) {
                    Thread currentThread = Thread.currentThread();
                    currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
                }
            }
        }

        @Override
        protected TriggerAction<? super T, ? super P> doSet(int index, TriggerAction<? super T, ? super P> element) {
            checkPreconditions(element);
            TriggerAction<? super T, ? super P> oldElement = backingList.set(index, element);
            T associatedObject = trigger.associatedObject;
            if (associatedObject == null) {
                return oldElement;
            }

            Throwable exception = null;

            try {
                oldElement.onDetached(associatedObject);
            } catch (Throwable ex) {
                exception = ex;
            }

            oldElement.associatedTrigger = null;
            element.associatedTrigger = trigger;

            try {
                element.onAttached(associatedObject);
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
        protected TriggerAction<? super T, ? super P> doRemove(int index) {
            TriggerAction<? super T, ? super P> oldAction = backingList.remove(index);
            T associatedObject = trigger.associatedObject;
            try {
                if (associatedObject != null) {
                    oldAction.onDetached(associatedObject);
                }
            } catch (Throwable ex) {
                Thread currentThread = Thread.currentThread();
                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
            } finally {
                oldAction.associatedTrigger = null;
            }

            return oldAction;
        }

        private void checkPreconditions(TriggerAction<?, ?> action) {
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
     * Runs all actions of this trigger with the specified {@code parameter}.
     *
     * @param parameter the parameter supplied to the actions
     */
    protected void runActions(P parameter) {
        for (TriggerAction<? super T, ? super P> action : getActions()) {
            try {
                action.onExecute(parameter);
            } catch (Throwable ex) {
                Thread currentThread = Thread.currentThread();
                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
            }
        }
    }

    /**
     * Occurs when the trigger is attached to an object.
     *
     * @param associatedObject the associated object
     */
    protected void onAttached(T associatedObject) {}

    /**
     * Occurs when the trigger is detached from an object.
     *
     * @param associatedObject the associated object
     */
    protected void onDetached(T associatedObject) {}

    @Override
    final void attach(T associatedObject) {
        onAttached(associatedObject);

        for (TriggerAction<? super T, ? super P> action : actions) {
            try {
                action.onAttached(associatedObject);
            } catch (Throwable ex) {
                Thread currentThread = Thread.currentThread();
                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
            }
        }
    }

    @Override
    final void detach(T associatedObject) {
        onDetached(associatedObject);

        for (TriggerAction<? super T, ? super P> action : actions) {
            try {
                action.onDetached(associatedObject);
            } catch (Throwable ex) {
                Thread currentThread = Thread.currentThread();
                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
            }
        }
    }

}
