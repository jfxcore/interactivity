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

import javafx.scene.Node;
import java.util.ArrayList;

final class CommandHandlerBehaviorList extends ArrayList<CommandHandlerBehavior<?>> {

    public static CommandHandlerBehaviorList tryGet(Node node) {
        if (node.hasProperties()) {
            return (CommandHandlerBehaviorList)node.getProperties().get(CommandHandlerBehaviorList.class);
        }

        return null;
    }

    public static CommandHandlerBehaviorList get(Node node) {
        return (CommandHandlerBehaviorList)node.getProperties().computeIfAbsent(
            CommandHandlerBehaviorList.class, key -> new CommandHandlerBehaviorList(node));
    }

    private final Node node;

    private CommandHandlerBehaviorList(Node node) {
        super(2);
        this.node = node;
    }

    @Override
    public boolean add(CommandHandlerBehavior handler) {
        if (handler == null) {
            throw new NullPointerException(CommandHandlerBehavior.class.getSimpleName() + " cannot be null");
        }

        if (contains(handler)) {
            throw new IllegalStateException(CommandHandlerBehavior.class.getSimpleName() + " is already set on " + node);
        }

        super.add(handler);
        invokeHandler(handler, true);

        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (super.remove(o)) {
            invokeHandler((CommandHandlerBehavior<?>)o, false);
            return true;
        }

        return false;
    }

    private void invokeHandler(CommandHandlerBehavior<?> handler, boolean attach) {
        for (InvokeCommandAction action : InvokeCommandActionList.get(node)) {
            Command command = action.getCommand();
            if (command != null) {
                try {
                    if (attach) {
                        handler.onAttached(command);
                    } else {
                        handler.onDetached(command);
                    }
                } catch (Throwable ex) {
                    Thread thread = Thread.currentThread();
                    thread.getUncaughtExceptionHandler().uncaughtException(thread, ex);
                }
            }
        }
    }

}
