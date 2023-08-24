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

import javafx.scene.Node;

/**
 * Encapsulates an action that is invoked by a {@link Trigger}.
 *
 * @param <T> the node type
 */
public abstract class TriggerAction<T extends Node> {

    Trigger<? extends T> associatedTrigger;

    /**
     * Initializes a new {@code TriggerAction} instance.
     */
    protected TriggerAction() {}

    /**
     * Gets the node that is associated with this {@code TriggerAction}.
     *
     * @return the node, or {@code null} if this action is not associated with a node
     */
    public final T getAssociatedNode() {
        return associatedTrigger != null ? associatedTrigger.getAssociatedNode() : null;
    }

    /**
     * Occurs when the action is executed.
     *
     * @param parameter the parameter that is passed to the action, or {@code null}
     */
    protected abstract void onExecute(Object parameter);

    /**
     * Occurs when the {@code TriggerAction} is attached to a node.
     *
     * @param node the node
     */
    protected void onAttached(T node) {}

    /**
     * Occurs when the {@code TriggerAction} is detached from a node.
     *
     * @param node the node
     */
    protected void onDetached(T node) {}

}
