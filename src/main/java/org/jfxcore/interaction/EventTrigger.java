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

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import java.util.Objects;

/**
 * Base class for triggers that react to the occurrence of an {@link Event}.
 *
 * @param <T> the node type
 * @param <E> the event type
 * @see ActionEventTrigger
 * @see KeyEventTrigger
 * @see MouseEventTrigger
 * @see TouchEventTrigger
 */
public abstract class EventTrigger<T extends Node, E extends Event> extends Trigger<T> {

    private final EventType<E> eventType;
    private final boolean eventFilter;

    private final EventHandler<E> handler = event -> {
        if (handleEvent(event)) {
            runActions(event);
            event.consume();
        }
    };

    /**
     * Initializes a new {@code EventTrigger} instance.
     *
     * @param eventType the event type
     * @param eventFilter {@code true} if the trigger should receive events in the capturing phase
     */
    protected EventTrigger(EventType<E> eventType, boolean eventFilter) {
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.eventFilter = eventFilter;
    }

    /**
     * Initializes a new {@code EventTrigger} instance.
     *
     * @param eventType the event type
     * @param eventFilter {@code true} if the trigger should receive events in the capturing phase
     * @param actions the actions
     */
    @SafeVarargs
    protected EventTrigger(EventType<E> eventType, boolean eventFilter, TriggerAction<? super T>... actions) {
        super(actions);
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.eventFilter = eventFilter;
    }

    /**
     * Determines whether this {@code EventTrigger} will handle the specified event.
     * <p>
     * Derived classes should override this method to inspect the event and return {@code true}
     * if the event should be handled by this {@code EventTrigger}.
     *
     * @param event the event
     * @return {@code true} if the event should be handled, {@code false} otherwise
     */
    protected boolean handleEvent(E event) {
        return true;
    }

    @Override
    protected final void onAttached(T node) {
        if (eventFilter) {
            node.addEventFilter(eventType, handler);
        } else {
            node.addEventHandler(eventType, handler);
        }
    }

    @Override
    protected final void onDetached(T node) {
        if (eventFilter) {
            node.removeEventFilter(eventType, handler);
        } else {
            node.removeEventHandler(eventType, handler);
        }
    }

    private void runActions(E event) {
        for (TriggerAction<? super T> action : getActions()) {
            try {
                action.onExecute(event);
            } catch (Throwable ex) {
                Thread currentThread = Thread.currentThread();
                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
            }
        }
    }

}
