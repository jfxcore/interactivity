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

import java.util.concurrent.CompletionStage;

/**
 * Provides information about an interaction request, as well as methods to complete or
 * cancel the interaction request.
 *
 * @param <P> the payload type
 * @param <R> the response type
 * @see Interaction#request
 * @see Interaction#requestAndWait
 */
public sealed interface InteractionRequest<P, R> extends CompletionStage<R> permits InteractionRequestBase {

    /**
     * Gets the associated interaction.
     *
     * @return the {@code Interaction}
     */
    Interaction<P, R> getInteraction();

    /**
     * Gets the payload of this interaction request.
     *
     * @return the payload, or {@code null} if no payload was supplied
     */
    P getPayload();

    /**
     * Indicates whether this interaction request has transitioned into one of the tree completion states:
     * <ul>
     *     <li>{@link #isCompleted()}
     *     <li>{@link #isCompletedExceptionally()}
     *     <li>{@link #isCancelled()}
     * </ul>
     *
     * @return {@code true} if this interaction request is done, {@code false} otherwise
     */
    boolean isDone();

    /**
     * Completes this interaction request with the specified response, unless it is already completed or cancelled.
     *
     * @param response the interaction response
     */
    void complete(R response);

    /**
     * Indicates whether this interaction request completed with a response.
     *
     * @return {@code true} if this interaction request completed with a response, {@code false} otherwise
     */
    boolean isCompleted();

    /**
     * Completes this interaction request with the specified exception, unless it is already completed or cancelled.
     *
     * @param cause the cause of exceptional completion
     */
    void completeExceptionally(Throwable cause);

    /**
     * Indicates whether this interaction request completed with an exception.
     *
     * @return {@code true} if this interaction request completed with an exception, {@code false} otherwise
     */
    boolean isCompletedExceptionally();

    /**
     * Cancels this interaction request, unless it is already completed.
     */
    void cancel();

    /**
     * Indicates whether this interaction request was cancelled.
     *
     * @return {@code true} if this interaction request was cancelled, {@code false} otherwise
     */
    boolean isCancelled();
}
