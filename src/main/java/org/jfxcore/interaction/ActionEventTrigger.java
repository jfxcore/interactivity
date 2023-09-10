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
import javafx.event.ActionEvent;
import javafx.event.EventTarget;

/**
 * Triggers actions when an {@link ActionEvent} occurs.
 */
public class ActionEventTrigger extends EventTrigger<ActionEvent> {

    /**
     * Initializes a new {@code ActionEventTrigger} instance.
     */
    public ActionEventTrigger() {
        super(ActionEvent.ACTION, false);
    }

    /**
     * Initializes a new {@code ActionEventTrigger} instance.
     *
     * @param eventFilter {@code true} if the trigger should receive events in the capturing phase
     */
    public ActionEventTrigger(@NamedArg("eventFilter") boolean eventFilter) {
        super(ActionEvent.ACTION, eventFilter);
    }

    /**
     * Initializes a new {@code ActionEventTrigger} instance with the specified actions.
     *
     * @param actions the actions
     */
    @SafeVarargs
    public ActionEventTrigger(@NamedArg("actions") TriggerAction<? super EventTarget, ? super ActionEvent>... actions) {
        super(ActionEvent.ACTION, false, actions);
    }

    /**
     * Initializes a new {@code ActionEventTrigger} instance with the specified actions.
     *
     * @param eventFilter {@code true} if the trigger should receive events in the capturing phase
     * @param actions the actions
     */
    @SafeVarargs
    public ActionEventTrigger(@NamedArg("eventFilter") boolean eventFilter,
                              @NamedArg("actions") TriggerAction<? super EventTarget, ? super ActionEvent>... actions) {
        super(ActionEvent.ACTION, eventFilter, actions);
    }

}
