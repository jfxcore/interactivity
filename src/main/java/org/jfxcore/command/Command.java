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

package org.jfxcore.command;

import org.jfxcore.interaction.ActionEventTrigger;
import org.jfxcore.interaction.Interaction;
import org.jfxcore.interaction.Trigger;
import org.jfxcore.interaction.TriggerAction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.event.EventTarget;
import javafx.event.ActionEvent;

/**
 * Represents an operation that can be invoked in various ways, such as by clicking a button,
 * pressing a key, or typing a shortcut.
 *
 * @see RelayCommand
 * @see ServiceCommand
 */
public abstract class Command {

    /**
     * The command that is invoked when the owner receives an {@link ActionEvent}.
     * <p>
     * Setting this property is a shortcut for adding an {@link ActionEventTrigger} with an
     * {@link InvokeCommandAction} to the owner, like in the following example:
     * <pre>{@code
     *     var command = new MyCommand();
     *     var invokeCommandAction = new InvokeCommandAction(command);
     *     var actionEventTrigger = new ActionEventTrigger(invokeCommandAction);
     *
     *     var button = new Button();
     *     Interaction.getTriggers(button).add(actionEventTrigger);
     * }</pre>
     *
     * @param owner the owner of the attached property
     * @return the command property
     */
    public static ObjectProperty<Command> onActionProperty(EventTarget owner) {
        class InvokeCommandActionImpl extends InvokeCommandAction {}
        var triggers = Interaction.getTriggers(owner);

        for (Trigger<?, ?> trigger : triggers) {
            if (trigger instanceof ActionEventTrigger actionEventTrigger) {
                for (TriggerAction<?, ?> action : actionEventTrigger.getActions()) {
                    if (action instanceof InvokeCommandActionImpl invokeCommandAction) {
                        return invokeCommandAction.commandProperty();
                    }
                }
            }
        }

        var invokeCommandAction = new InvokeCommandActionImpl();
        triggers.add(new ActionEventTrigger(invokeCommandAction));
        return invokeCommandAction.commandProperty();
    }

    /**
     * Gets the value of the {@link #onActionProperty(EventTarget) onAction} property.
     *
     * @param owner the owner of the attached property
     * @return the command
     */
    public static Command getOnAction(EventTarget owner) {
        return onActionProperty(owner).get();
    }

    /**
     * Sets the value of the {@link #onActionProperty(EventTarget) onAction} property.
     *
     * @param owner the owner of the attached property
     * @param command the command
     */
    public static void setOnAction(EventTarget owner, Command command) {
        onActionProperty(owner).set(command);
    }

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
     * Occurs when the command is attached to an object.
     * <p>
     * If the command is added to multiple {@link InvokeCommandAction} instances on a single object, this
     * method is only invoked once. Note that a command can be attached to multiple different objects;
     * this method will be invoked once for each object to which this command is attached.
     * <p>
     * Derived classes can override this method to implement logic that the command uses to configure the
     * controls to which it is attached. For example, a command implementation could set a control's
     * {@code Labeled.textProperty()} to a user-defined value.
     * Implementing the {@code onAttached} and {@code onDetached} methods is an alternative to using a
     * {@link CommandHandlerBehavior}. The major difference is that {@code CommandHandlerBehavior} only
     * applies to specific objects on which the {@code CommandHandlerBehavior} is set, while overriding
     * {@code onAttached} and {@code onDetached} applies to all objects to which this command is attached.
     *
     * @param associatedObject the object to which this command is attached
     */
    protected void onAttached(Object associatedObject) {}

    /**
     * Occurs when the command is detached from an object.
     * <p>
     * If the command was added to multiple {@link InvokeCommandAction} instances on a single object, this
     * method is only invoked after it is removed from all {@code InvokeCommandAction} instances.
     * Note that a command can be attached to multiple different objects; this method will be invoked once
     * for each object from which this command is detached.
     * <p>
     * Derived classes can override this method to roll back changes that were established
     * by the {@link #onAttached onAttached} method.
     *
     * @param associatedObject the object to which this command was attached
     */
    protected void onDetached(Object associatedObject) {}

}
