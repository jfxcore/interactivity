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

/**
 * A listener that can respond to an {@link InteractionRequest}.
 *
 * @param <P> the payload type
 * @param <R> the response type
 * @see WeakInteractionListener
 */
@FunctionalInterface
public interface InteractionListener<P, R> {

    /**
     * Accepts an interaction request and returns whether the listener will respond to the request.
     *
     * @implSpec Listener implementations can respond to the interaction request immediately, or they
     *           can choose to respond to the interaction request at a later point in time.
     *           If an implementation responds to the request or intends to respond to the request later,
     *           it must return {@code true} from this method. An implementation must never complete the
     *           interaction request if it returned {@code false} from this method.
     *
     * @param request the interaction request
     * @return {@code true} if the interaction request is handled, {@code false} otherwise
     */
    boolean accept(InteractionRequest<P, R> request);
}
