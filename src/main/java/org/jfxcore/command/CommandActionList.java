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

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.ModifiableObservableListBase;
import javafx.scene.Node;
import java.util.ArrayList;
import java.util.List;

final class CommandActionList extends ModifiableObservableListBase<InvokeCommandAction> {

    public static CommandActionList get(Node node) {
        return (CommandActionList)node.getProperties().computeIfAbsent(
            CommandActionList.class, key -> new CommandActionList(node));
    }

    private final Node node;
    private final List<InvokeCommandAction> list = new ArrayList<>(1);

    private final ChangeListener<Command> commandChanged = (observable, oldValue, newValue) -> {
        Command removedCommand = contains(oldValue, 0) ? oldValue : null;
        Command addedCommand = contains(newValue, 1) ? newValue : null;
        updateCommand(removedCommand, addedCommand);
    };

    private final InvalidationListener disabledInvalidated = observable -> updateDisabled();

    private CommandActionList(Node node) {
        this.node = node;
    }

    @Override
    public InvokeCommandAction get(int index) {
        return list.get(index);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    protected InvokeCommandAction doSet(int index, InvokeCommandAction action) {
        return doAddOrSet(index, action, false);
    }

    @Override
    protected void doAdd(int index, InvokeCommandAction action) {
        doAddOrSet(index, action, true);
    }

    private InvokeCommandAction doAddOrSet(int index, InvokeCommandAction action, boolean add) {
        action.disabledProperty().addListener(disabledInvalidated);
        action.commandProperty().addListener(commandChanged);
        InvokeCommandAction oldValue = null;

        if (add) {
            list.add(action);
        } else {
            oldValue = list.set(index, action);
        }

        updateDisabled();
        Command addedCommand = action.getCommand();

        if (addedCommand != null && contains(addedCommand, 1)) {
            updateCommand(null, addedCommand);
        }

        return oldValue;
    }

    @Override
    protected InvokeCommandAction doRemove(int index) {
        InvokeCommandAction action = get(index);
        action.disabledProperty().removeListener(disabledInvalidated);
        action.commandProperty().removeListener(commandChanged);
        list.remove(index);

        Command removedCommand = action.getCommand();
        if (removedCommand != null && contains(removedCommand, 0)) {
            updateCommand(removedCommand, null);
        }

        updateDisabled();
        return action;
    }

    private void updateCommand(Command removedCommand, Command addedCommand) {
        if (removedCommand != null) {
            invokeCommand(removedCommand, false);
            invokeHandler(removedCommand, false);
        }

        if (addedCommand != null) {
            invokeCommand(addedCommand, true);
            invokeHandler(addedCommand, true);
        }
    }

    private void invokeCommand(Command command, boolean attach) {
        try {
            if (attach) {
                command.onAttached(node);
            } else {
                command.onDetached(node);
            }
        } catch (Throwable ex) {
            Thread thread = Thread.currentThread();
            thread.getUncaughtExceptionHandler().uncaughtException(thread, ex);
        }
    }

    private void invokeHandler(Command command, boolean attach) {
        List<CommandHandlerBehavior<?>> commandHandlerBehaviors = CommandHandlerBehaviorList.tryGet(node);
        if (commandHandlerBehaviors == null || commandHandlerBehaviors.isEmpty()) {
            return;
        }

        for (CommandHandlerBehavior<?> commandHandlerBehavior : commandHandlerBehaviors) {
            try {
                if (attach) {
                    commandHandlerBehavior.onAttached(command);
                } else {
                    commandHandlerBehavior.onDetached(command);
                }
            } catch (Throwable ex) {
                Thread thread = Thread.currentThread();
                thread.getUncaughtExceptionHandler().uncaughtException(thread, ex);
            }
        }
    }

    private void updateDisabled() {
        if (!node.disableProperty().isBound()) {
            node.setDisable(isDisabled());
        }
    }

    private boolean isDisabled() {
        for (int i = 0, max = size(); i < max; ++i) {
            if (get(i).disabledProperty().get()) {
                return true;
            }
        }

        return false;
    }

    private boolean contains(Command command, int n) {
        int c = 0;

        for (int i = 0, max = size(); i < max; ++i) {
            if (get(i).getCommand() == command && ++c > n) {
                return false;
            }
        }

        return c == n;
    }

}
