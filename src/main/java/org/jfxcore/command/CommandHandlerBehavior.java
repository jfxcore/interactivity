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

import org.jfxcore.interaction.Behavior;
import javafx.scene.Node;

/**
 * {@code CommandHandlerBehavior} provides an extension mechanism that defines a piece of behavior or configuration
 * logic that is executed when a {@link Command} is attached to, or detached from, a {@link Node}.
 * <p>
 * In this example, a {@code CommandHandlerBehavior} is used to automatically set a {@code Button}'s text to
 * the name of its attached command:
 * <pre>{@code
 *     // Define a behavior to apply the command title to Button controls.
 *     public class MyCommandHandler extends CommandHandlerBehavior<Button> {
 *         @Override
 *         public void onAttached(Button button, Command command) {
 *             if (command instanceof ServiceCommand<?> c) {
 *                 button.textProperty().bind(c.titleProperty());
 *             }
 *         }
 *
 *         @Override
 *         public void onDetached(Button button, Command command) {
 *             button.textProperty().unbind();
 *         }
 *     }
 *
 *     // Wire it up.
 *     var command = new ServiceCommand(myService);
 *     var trigger = new ActionEventTrigger(command);
 *     var handler = new MyCommandHandler();
 *     var button = new Button();
 *     Interaction.getTriggers(button).add(trigger);
 *     Interaction.getBehaviors(button).add(handler);
 * }</pre>
 *
 * Note that the {@link Command} class defines overridable {@link Command#onAttached onAttached} and
 * {@link Command#onDetached onDetached} methods, which can also be used to implement behavior logic.
 * The difference between those methods and {@code CommandHandlerBehavior} is in scope:
 * logic in a {@code Command} subclass generally applies to all nodes that use the command,
 * while {@code CommandHandlerBehavior} only applies to specific nodes on which the handler is set.
 */
public abstract class CommandHandlerBehavior<T extends Node> extends Behavior<T> {

    /**
     * Occurs when the command is attached to a {@link Node}.
     * <p>
     * When the command is bound to multiple events of a single {@code Node}, this method is only invoked once.
     * Note that this method will be invoked once for each {@code Node} to which this command is attached.
     *
     * @param command the command
     */
    protected abstract void onAttached(Command command);

    /**
     * Occurs when the command is detached from a {@link Node}.
     * <p>
     * This happens when the command is removed from its associated {@link EventTrigger}, or if the
     * {@code EventTrigger} is removed from the {@code Node}.
     * When the command is bound to multiple events of a single {@code Node}, the command is only detached
     * after the last binding is removed. Note that this method will be invoked once for each {@code Node}
     * from which this command is detached.
     *
     * @param command the command
     */
    protected abstract void onDetached(Command command);

    @Override
    protected final void onAttached(T node) {
        CommandHandlerBehaviorList.get(node).add(this);
    }

    @Override
    protected final void onDetached(T node) {
        CommandHandlerBehaviorList.get(node).remove(this);
    }

}
