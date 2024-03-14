/*
 * Copyright (c) 2024, JFXcore. All rights reserved.
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

import javafx.beans.WeakListener;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * A weakly-reachable wrapper for {@link InteractionListener}, which can be used to prevent an
 * {@link Interaction} from retaining a strong reference to the {@code InteractionListener}.
 *
 * @param <P> the payload type
 * @param <R> the response type
 */
public final class WeakInteractionListener<P, R> implements InteractionListener<P, R>, WeakListener {

    private final WeakReference<InteractionListener<P, R>> wref;

    /**
     * Creates a new instance of {@code WeakInteractionListener}.
     *
     * @param listener the wrapped {@code InteractionListener}
     */
    public WeakInteractionListener(InteractionListener<P, R> listener) {
        this.wref = new WeakReference<>(Objects.requireNonNull(listener, "listener cannot be null"));
    }

    @Override
    public boolean wasGarbageCollected() {
        return false;
    }

    @Override
    public boolean accept(InteractionRequest<P, R> request) {
        InteractionListener<P, R> ref = wref.get();
        if (ref == null) {
            request.getInteraction().removeListener(this);
            return false;
        }

        return ref.accept(request);
    }

}
