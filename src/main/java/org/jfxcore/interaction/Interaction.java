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
import java.util.Objects;

/**
 * Contains the static {@link #getBehaviors Interaction.behaviors} and
 * {@link #getTriggers Interaction.triggers} properties.
 */
public final class Interaction {

    private Interaction() {}

    /**
     * Gets the {@link Behavior behaviors} that are defined on the specified {@code node}.
     *
     * @param node the node
     * @return a modifiable list of behaviors
     * @param <T> the node type
     */
    @SuppressWarnings("unchecked")
    public static <T extends Node> ObservableList<Behavior<? super T>> getBehaviors(T node) {
        BehaviorList<T, Behavior<? super T>> list =
            (BehaviorList<T, Behavior<? super T>>)Objects.requireNonNull(node, "node cannot be null")
            .getProperties()
            .get(BehaviorList.class);

        if (list == null) {
            list = new BehaviorList<>(node);
            node.getProperties().put(BehaviorList.class, list);
        }

        return list;
    }

    /**
     * Gets the {@link Trigger triggers} that are defined on the specified {@code node}.
     *
     * @param node the node
     * @return a modifiable list of triggers
     * @param <T> the node type
     */
    @SuppressWarnings("unchecked")
    public static <T extends Node> ObservableList<Trigger<? super T>> getTriggers(T node) {
        TriggerList<T, Trigger<? super T>> list =
            (TriggerList<T, Trigger<? super T>>)Objects.requireNonNull(node, "node cannot be null")
                .getProperties()
                .get(TriggerList.class);

        if (list == null) {
            list = new TriggerList<>(node);
            node.getProperties().put(TriggerList.class, list);
        }

        return list;
    }

    private static abstract class AttachableList<T extends Node, U extends Attachable<? super T>>
            extends ModifiableObservableListBase<U> {
        final List<U> backingList;
        final T node;

        AttachableList(T node) {
            this.backingList = new ArrayList<>(2);
            this.node = node;
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
            element.associatedNode = node;

            try {
                element.attach(node);
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
                oldElement.detach(node);
            } catch (Throwable ex) {
                exception = ex;
            }

            oldElement.associatedNode = null;
            element.associatedNode = node;

            try {
                element.attach(node);
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
                oldElement.detach(node);
            } catch (Throwable ex) {
                Thread currentThread = Thread.currentThread();
                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
            } finally {
                oldElement.associatedNode = null;
            }

            return oldElement;
        }

        abstract String elementName();

        private void checkPreconditions(U element) {
            if (element == null) {
                throw new NullPointerException(elementName() + " cannot be null.");
            }

            if (element.associatedNode == node) {
                throw new IllegalStateException(elementName() + " cannot be attached to the same node more than once.");
            }

            if (element.associatedNode != null) {
                throw new IllegalStateException(elementName() + " cannot be attached to multiple nodes.");
            }
        }
    }

    private static class BehaviorList<T extends Node, U extends Behavior<? super T>> extends AttachableList<T, U> {
        BehaviorList(T node) {
            super(node);
        }

        @Override
        String elementName() {
            return "Behavior";
        }

        @Override
        public String toString() {
            return BehaviorList.class.getName();
        }
    }

    private static class TriggerList<T extends Node, U extends Trigger<? super T>> extends AttachableList<T, U> {
        TriggerList(T node) {
            super(node);
        }

        @Override
        String elementName() {
            return "Trigger";
        }

        @Override
        public String toString() {
            return TriggerList.class.getName();
        }
    }

}
