/*
 * Copyright (c) 2023, 2024, JFXcore. All rights reserved.
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
 * {@code Behavior} is the base class for reusable components that can extend the behavior of
 * JavaFX entities.
 * <p>
 * A behavior is attached to a JavaFX entity by adding it to its behaviors list, which can be
 * retrieved with the {@link Interaction#getBehaviors} method. A behavior can only be attached
 * to a single object at any time, but it can be re-used after being detached.
 * <p>
 * Implementations can override the {@link #onAttached} and {@link #onDetached} methods to run
 * custom code, install listeners, or configure the associated entity.
 *
 * @param <T> the target of this behavior, for example a {@link Node}
 */
public abstract non-sealed class Behavior<T> extends Attachable<T> {

    /**
     * Initializes a new {@code Behavior} instance.
     */
    protected Behavior() {}

    /**
     * Gets the associated object.
     *
     * @return the associated object, or {@code null} if this behavior is not associated with an object
     */
    @Override
    public final T getAssociatedObject() {
        return super.getAssociatedObject();
    }

    /**
     * Occurs when the behavior is attached to an object by adding it to the object's
     * {@link Interaction#getBehaviors} list.
     * <p>
     * The behavior can only be attached to a single object at any time; any attempt to attach the behavior
     * to another object will fail with an {@link IllegalStateException}.
     * When the behavior is removed from the object's behavior list, {@link #onDetached} will be invoked.
     *
     * @param associatedObject the associated object
     */
    protected void onAttached(T associatedObject) {}

    /**
     * Occurs when the behavior is detached from an object.
     *
     * @param associatedObject the associated object
     */
    protected void onDetached(T associatedObject) {}

    @Override
    final void attach(T associatedObject) {
        onAttached(associatedObject);
    }

    @Override
    final void detach(T associatedObject) {
        onDetached(associatedObject);
    }

}
