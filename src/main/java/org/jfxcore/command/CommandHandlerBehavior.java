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

/**
 * {@code CommandHandlerBehavior} provides an extension mechanism that defines a piece of behavior or configuration
 * logic that is executed when a {@link Command} is attached to, or detached from, an object.
 * <p>
 * In this example, a {@code CommandHandlerBehavior} is used to automatically set a {@code Button}'s text to
 * the name of its attached command:
 * <pre>{@code
 *     // Define a behavior to apply the command title to Button controls.
 *     public class MyCommandHandler extends CommandHandlerBehavior<Button> {
 *         @Override
 *         public void onAttached(Command command) {
 *             if (command instanceof ServiceCommand c) {
 *                 getAssociatedObject().textProperty().bind(c.titleProperty());
 *             }
 *         }
 *
 *         @Override
 *         public void onDetached(Command command) {
 *             getAssociatedObject().textProperty().unbind();
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
 * {@link Command#onDetached onDetached} methods, which can also be used to implement custom behavior.
 * The difference between those methods and {@code CommandHandlerBehavior} is in scope:
 * logic in a {@code Command} subclass generally applies to all objects that use the command, while
 * {@code CommandHandlerBehavior} only applies to specific objects on which the handler is set.
 *
 * @param <T> the target of this behavior
 */
public abstract class CommandHandlerBehavior<T> extends Behavior<T> {

    /**
     * Initializes a new {@code CommandHandlerBehavior} instance.
     */
    protected CommandHandlerBehavior() {}

    /**
     * Occurs when the command is attached to an object.
     * <p>
     * If the command is added to multiple {@link InvokeCommandAction} instances on a single object,
     * this method is only invoked once.
     *
     * @param command the command
     */
    protected abstract void onAttached(Command command);

    /**
     * Occurs when the command is detached from an object.
     * <p>
     * If the command was added to multiple {@link InvokeCommandAction} instances on a single object,
     * this method is only invoked after it is removed from all {@code InvokeCommandAction} instances.
     *
     * @param command the command
     */
    protected abstract void onDetached(Command command);

    @Override
    protected final void onAttached(T associatedObject) {
        CommandHandlerBehaviorList.get(associatedObject).add(this);
    }

    @Override
    protected final void onDetached(T associatedObject) {
        CommandHandlerBehaviorList.get(associatedObject).remove(this);
    }

}
