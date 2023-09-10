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

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import java.util.Objects;

/**
 * A trigger that reacts to the occurrence of an {@link Event}.
 *
 * @param <E> the event type provided to this trigger's actions
 * @see ActionEventTrigger
 * @see KeyEventTrigger
 * @see MouseEventTrigger
 * @see TouchEventTrigger
 */
public class EventTrigger<E extends Event> extends Trigger<EventTarget, E> {

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
     */
    public EventTrigger(@NamedArg("eventType") EventType<E> eventType) {
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.eventFilter = false;
    }

    /**
     * Initializes a new {@code EventTrigger} instance.
     *
     * @param eventType the event type
     * @param eventFilter {@code true} if the trigger should receive events in the capturing phase
     */
    public EventTrigger(@NamedArg("eventType") EventType<E> eventType,
                        @NamedArg("eventFilter") boolean eventFilter) {
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.eventFilter = eventFilter;
    }

    /**
     * Initializes a new {@code EventTrigger} instance.
     *
     * @param eventType the event type
     * @param actions the actions
     */
    @SafeVarargs
    public EventTrigger(@NamedArg("eventType") EventType<E> eventType,
                        @NamedArg("actions") TriggerAction<? super EventTarget, ? super E>... actions) {
        super(actions);
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.eventFilter = false;
    }

    /**
     * Initializes a new {@code EventTrigger} instance.
     *
     * @param eventType the event type
     * @param eventFilter {@code true} if the trigger should receive events in the capturing phase
     * @param actions the actions
     */
    @SafeVarargs
    public EventTrigger(@NamedArg("eventType") EventType<E> eventType,
                        @NamedArg("eventFilter") boolean eventFilter,
                        @NamedArg("actions") TriggerAction<? super EventTarget, ? super E>... actions) {
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
    protected final void onAttached(EventTarget associatedObject) {
        if (eventFilter) {
            associatedObject.addEventFilter(eventType, handler);
        } else {
            associatedObject.addEventHandler(eventType, handler);
        }
    }

    @Override
    protected final void onDetached(EventTarget associatedObject) {
        if (eventFilter) {
            associatedObject.removeEventFilter(eventType, handler);
        } else {
            associatedObject.removeEventHandler(eventType, handler);
        }
    }

    private void runActions(E event) {
        for (TriggerAction<? super EventTarget, ? super E> action : getActions()) {
            try {
                action.onExecute(event);
            } catch (Throwable ex) {
                Thread currentThread = Thread.currentThread();
                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
            }
        }
    }

}
