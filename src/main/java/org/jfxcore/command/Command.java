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

import org.jfxcore.interaction.Trigger;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.Node;

/**
 * Represents an operation that can be invoked in various ways, such as by clicking a button,
 * pressing a key, or typing a shortcut.
 *
 * @see RelayCommand
 * @see ServiceCommand
 */
public abstract class Command {

    /**
     * Initializes a new {@code Command} instance.
     */
    protected Command() {}

    /**
     * Gets a property that indicates whether the command is currently executable.
     *
     * @return the {@code executable} property
     */
    public abstract ReadOnlyBooleanProperty executableProperty();

    /**
     * Indicates whether the command is currently executable.
     *
     * @return {@code true} if the command is executable, {@code false} otherwise
     */
    public boolean isExecutable() {
        return executableProperty().get();
    }

    /**
     * Executes the command.
     *
     * @param parameter the parameter that is passed to the command, or {@code null}
     * @throws IllegalStateException if the command is not {@link #executableProperty() executable}
     */
    public final void execute(Object parameter) {
        if (!isExecutable()) {
            throw new IllegalStateException("Command is not executable.");
        }

        onExecute(parameter);
    }

    /**
     * Occurs when the command is executed.
     *
     * @param parameter the parameter that is passed to the command, or {@code null}
     */
    protected abstract void onExecute(Object parameter);

    /**
     * Occurs when the command is attached to a {@link Node} by binding it to one of the {@code Node}'s events.
     * <p>
     * When the command is bound to multiple events of a single {@code Node}, this method is only invoked once.
     * Note that this method will be invoked once for each {@code Node} to which this command is attached.
     * <p>
     * Derived classes can override this method to implement logic that the command uses to configure the
     * controls to which it is attached. For example, a command implementation could set a control's
     * {@code Labeled.textProperty()} to a user-defined value.
     * Implementing the {@code onAttached} and {@code onDetached} methods is an alternative to using a
     * {@link CommandHandlerBehavior}. The major difference is that {@code CommandHandlerBehavior} only applies to specific
     * nodes on which the {@code CommandHandlerBehavior} is set, while overriding {@code onAttached} and
     * {@code onDetached} applies to all nodes to which this command is attached.
     *
     * @param node the node to which this command is attached
     */
    protected void onAttached(Node node) {}

    /**
     * Occurs when the command is detached from a {@link Node}.
     * <p>
     * This happens when the command is removed from its associated {@link InvokeCommandAction}, if the
     * {@code InvokeCommandAction} is removed from its associated {@link Trigger}, or if the {@code Trigger}
     * is removed from the {@link Node}.
     * When the command is bound to multiple events of a single {@code Node}, the command is only detached
     * after the last binding is removed. Note that this method will be invoked once for each {@code Node}
     * from which this command is detached.
     * <p>
     * Derived classes can override this method to roll back changes that were established
     * by the {@link #onAttached onAttached} method.
     *
     * @param node the node to which this command was attached
     */
    protected void onDetached(Node node) {}

}
