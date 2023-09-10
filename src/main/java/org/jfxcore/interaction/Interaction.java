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

import javafx.collections.ModifiableObservableListBase;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Contains the static {@link #getBehaviors Interaction.behaviors} and
 * {@link #getTriggers Interaction.triggers} properties.
 */
public final class Interaction {

    private Interaction() {}

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
