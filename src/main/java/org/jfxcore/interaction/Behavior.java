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
 * {@code Behavior} is the base class for reusable components that can extend the behavior of scene graph nodes.
 * <p>
 * A behavior is attached to a scene graph node by adding it to the node's {@link Interaction#getBehaviors}.
 * <p>
 * Implementations can override the {@link #onAttached} and {@link #onDetached} methods to run custom
 * code, install listeners, or configure the associated node.
 *
 * @param <T> the node type
 */
public abstract non-sealed class Behavior<T extends Node> extends Attachable<T> {

    /**
     * Initializes a new {@code Behavior} instance.
     */
    protected Behavior() {}

    /**
     * Gets the associated node.
     *
     * @return the associated node, or {@code null} if this behavior is not associated with a node
     */
    @Override
    public final T getAssociatedNode() {
        return super.getAssociatedNode();
    }

    /**
     * Occurs when the behavior is attached to a node.
     *
     * @param node the node
     */
    protected void onAttached(T node) {}

    /**
     * Occurs when the behavior is detached from a node.
     *
     * @param node the node
     */
    protected void onDetached(T node) {}

    @Override
    final void attach(T node) {
        onAttached(node);
    }

    @Override
    final void detach(T node) {
        onDetached(node);
    }

}
