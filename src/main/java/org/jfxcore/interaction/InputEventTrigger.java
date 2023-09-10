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
import javafx.beans.property.ObjectProperty;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.input.InputEvent;
import java.util.Objects;

/**
 * A triggers that react to the occurrence of an {@link InputEvent}.
 *
 * @param <E> the event type
 */
public class InputEventTrigger<E extends InputEvent> extends EventTrigger<E> {

    /**
     * Initializes a new {@code InputEventTrigger} instance.
     *
     * @param eventType the event type
     */
    public InputEventTrigger(@NamedArg("eventType") EventType<E> eventType) {
        super(eventType);
    }

    /**
     * Initializes a new {@code InputEventTrigger} instance.
     *
     * @param eventType the event type
     * @param eventFilter {@code true} if the trigger should receive events in the capturing phase
     */
    public InputEventTrigger(@NamedArg("eventType") EventType<E> eventType,
                             @NamedArg("eventFilter") boolean eventFilter) {
        super(eventType, eventFilter);
    }

    /**
     * Initializes a new {@code InputEventTrigger} instance with the specified actions.
     *
     * @param eventType the event type
     * @param actions the actions
     */
    @SafeVarargs
    public InputEventTrigger(@NamedArg("eventType") EventType<E> eventType,
                             @NamedArg("actions") TriggerAction<? super EventTarget, ? super E>... actions) {
        super(eventType, actions);
    }

    /**
     * Initializes a new {@code InputEventTrigger} instance with the specified actions.
     *
     * @param eventType the event type
     * @param eventFilter {@code true} if the trigger should receive events in the capturing phase
     * @param actions the actions
     */
    @SafeVarargs
    public InputEventTrigger(@NamedArg("eventType") EventType<E> eventType,
                             @NamedArg("eventFilter") boolean eventFilter,
                             @NamedArg("actions") TriggerAction<? super EventTarget, ? super E>... actions) {
        super(eventType, eventFilter, actions);
    }

    static <T> boolean matches(ObjectProperty<T> property, T value) {
        return property == null || Objects.equals(property.get(), value);
    }

}
