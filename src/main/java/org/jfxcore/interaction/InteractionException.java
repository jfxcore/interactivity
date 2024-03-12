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

import java.util.Objects;

/**
 * Thrown when an interaction request was completed with an exception.
 */
public class InteractionException extends RuntimeException {

    private final InteractionRequest<?, ?> request;

    /**
     * Creates a new instance of {@code InteractionException}.
     *
     * @param request the interaction request
     */
    public InteractionException(InteractionRequest<?, ?> request) {
        this.request = Objects.requireNonNull(request, "request cannot be null");
    }

    /**
     * Creates a new instance of {@code InteractionException}.
     *
     * @param cause the exception
     * @param request the interaction request
     */
    public InteractionException(Throwable cause, InteractionRequest<?, ?> request) {
        super(cause);
        this.request = Objects.requireNonNull(request, "request cannot be null");
    }

    /**
     * Creates a new instance of {@code InteractionException}.
     *
     * @param message the message
     * @param request the interaction request
     */
    public InteractionException(String message, InteractionRequest<?, ?> request) {
        super(message);
        this.request = Objects.requireNonNull(request, "request cannot be null");
    }

    /**
     * Creates a new instance of {@code InteractionException}.
     *
     * @param message the message
     * @param cause the exception
     * @param request the interaction request
     */
    public InteractionException(String message, Throwable cause, InteractionRequest<?, ?> request) {
        super(message, cause);
        this.request = Objects.requireNonNull(request, "request cannot be null");
    }

    /**
     * Gets the associated interaction request.
     *
     * @return the {@code InteractionRequest}
     */
    public final InteractionRequest<?, ?> getRequest() {
        return request;
    }
}
