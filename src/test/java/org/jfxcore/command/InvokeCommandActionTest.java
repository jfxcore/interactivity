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

import org.jfxcore.command.mocks.TestCommand;
import org.jfxcore.interaction.ActionEventTrigger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InvokeCommandActionTest {

    @Test
    public void testActionWithoutCommandIsDisabled() {
        var commandAction = new InvokeCommandAction();
        assertTrue(commandAction.disabledProperty().get());
    }

    @Test
    public void testActionWithExecutableCommandIsEnabled() {
        var action = new InvokeCommandAction();
        var command = new TestCommand(null, null, false);
        command.executableProperty().set(true);
        action.setCommand(command);
        assertFalse(action.disabledProperty().get());
        command.executableProperty().set(false);
        assertTrue(action.disabledProperty().get());
    }

    @Test
    public void testActionIsDisabledWhenCommandIsExecuting() {
        var action = new InvokeCommandAction();
        var command = new TestCommand(null, null, false);
        command.executableProperty().set(true);
        command.executingProperty().set(true);
        action.setCommand(command);
        assertTrue(action.disabledProperty().get());
        action.setDisabledWhenExecuting(false);
        assertFalse(action.disabledProperty().get());
    }

    @Test
    public void testActionIsDisabledWhenCommandIsNotExecutable() {
        var action = new InvokeCommandAction();
        var command = new TestCommand(null, null, false);
        command.executableProperty().set(false);
        command.executingProperty().set(false);
        action.setCommand(command);
        assertTrue(action.disabledProperty().get());
        action.setDisabledWhenNotExecutable(false);
        assertFalse(action.disabledProperty().get());
    }

    @Test
    public void testCommandIsNotExecutedWhenActionIsTriggeredWhenCommandIsNotExecutable() {
        var action = new InvokeCommandAction();
        var command = new TestCommand(null, null, false) {
            @Override
            public void onExecute(Object parameter) {
                fail();
            }
        };

        command.executableProperty().set(false);
        command.executingProperty().set(false);
        action.setCommand(command);
        action.setDisabledWhenNotExecutable(false);
        action.onExecute(null);
    }

    @Test
    public void testActionIsUpdatedWhenCommandIsChanged() {
        var action = new InvokeCommandAction();
        var command1 = new TestCommand(null, null ,false);
        var command2 = new TestCommand(null, null, false);
        command1.executableProperty().set(true);
        command2.executableProperty().set(false);
        action.setCommand(command1);
        assertFalse(action.disabledProperty().get());
        action.setCommand(command2);
        assertTrue(action.disabledProperty().get());
        action.setCommand(command1);
        assertFalse(action.disabledProperty().get());
        action.setCommand(null);
        assertTrue(action.disabledProperty().get());
    }

    @Test
    public void testActionCannotBeReused() {
        var action = new InvokeCommandAction();
        var trigger1 = new ActionEventTrigger();
        trigger1.getActions().add(action);
        var trigger2 = new ActionEventTrigger();
        assertThrows(IllegalStateException.class, () -> trigger2.getActions().add(action));
        trigger1.getActions().remove(action);
        assertDoesNotThrow(() -> trigger2.getActions().add(action));
    }

}
