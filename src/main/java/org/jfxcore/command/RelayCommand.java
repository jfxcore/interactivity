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

package org.jfxcore.command;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A command implementation that invokes a user-specified operation.
 *
 * @param <T> the parameter type, or {@code Void} if the command is not parameterized
 */
public class RelayCommand<T> extends Command {

    private final Consumer<T> operation;
    private final Consumer<Throwable> exceptionHandler;

    /**
     * Creates a new {@link RelayCommand} instance that executes a parameterless operation.
     *
     * @param operation a parameterless operation
     */
    public RelayCommand(Runnable operation) {
        Objects.requireNonNull(operation, "operation cannot be null");
        this.operation = unused -> operation.run();
        this.exceptionHandler = null;
    }

    /**
     * Creates a new {@link RelayCommand} instance that executes a parameterless operation.
     *
     * @param operation a parameterless operation
     * @param exceptionHandler handler for exceptions thrown by the operation
     */
    public RelayCommand(Runnable operation, Consumer<Throwable> exceptionHandler) {
        Objects.requireNonNull(operation, "operation cannot be null");
        this.operation = unused -> operation.run();
        this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler cannot be null");
    }

    /**
     * Creates a new {@link RelayCommand} instance that executes a parameterized operation.
     *
     * @param operation a parameterized operation
     */
    public RelayCommand(Consumer<T> operation) {
        this.operation = Objects.requireNonNull(operation, "operation cannot be null");
        this.exceptionHandler = null;
    }

    /**
     * Creates a new {@link RelayCommand} instance that executes a parameterized operation.
     *
     * @param operation a parameterized operation
     * @param exceptionHandler handler for exceptions thrown by the operation
     */
    public RelayCommand(Consumer<T> operation, Consumer<Throwable> exceptionHandler) {
        this.operation = Objects.requireNonNull(operation, "operation cannot be null");
        this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler cannot be null");
    }

    private final BooleanProperty executable = new SimpleBooleanProperty(this, "executable", true);

    @Override
    public final BooleanProperty executableProperty() {
        return executable;
    }

    @Override
    public final boolean isExecutable() {
        return executable.get();
    }

    public final void setExecutable(boolean executable) {
        this.executable.set(executable);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onExecute(Object parameter) {
        try {
            operation.accept((T)parameter);
        } catch (Throwable ex) {
            if (exceptionHandler != null) {
                exceptionHandler.accept(ex);
            } else {
                throw ex;
            }
        }
    }

}
