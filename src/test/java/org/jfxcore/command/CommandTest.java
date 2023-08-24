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

import org.jfxcore.command.mocks.TestCommand;
import org.jfxcore.interaction.ActionEventTrigger;
import org.jfxcore.interaction.Interaction;
import org.jfxcore.interaction.KeyEventTrigger;
import org.junit.jupiter.api.Test;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CommandTest {

    @Test
    public void testAttachAndDetach() {
        testAttachAndDetach(false);
    }

    @Test
    public void testAttachAndDetachExceptional() {
        testAttachAndDetach(true);
    }

    private void testAttachAndDetach(boolean failOnAttachDetach) {
        var trace = new ArrayList<String>();
        var pane = new Pane();
        var command1 = new TestCommand(trace, "A", failOnAttachDetach);
        var command2 = new TestCommand(trace, "B", failOnAttachDetach);
        var trigger1 = new ActionEventTrigger<>(new InvokeCommandAction(command1));
        var trigger2 = new ActionEventTrigger<>(new InvokeCommandAction(command2));
        Interaction.getTriggers(pane).add(trigger1);
        Interaction.getTriggers(pane).add(trigger2);
        assertEquals(2, InvokeCommandActionList.get(pane).size());
        assertEquals(List.of("+A", "+B"), trace);
        Interaction.getTriggers(pane).remove(trigger1);
        assertEquals(1, InvokeCommandActionList.get(pane).size());
        assertEquals(List.of("+A", "+B", "-A"), trace);
        Interaction.getTriggers(pane).remove(trigger2);
        assertEquals(0, InvokeCommandActionList.get(pane).size());
        assertEquals(List.of("+A", "+B", "-A", "-B"), trace);
    }

    @Test
    public void testAddEventBindingWithoutCommand() {
        var trace = new ArrayList<String>();
        var pane = new Pane();
        var command = new TestCommand(trace, "A", false);
        var commandAction = new InvokeCommandAction();
        var trigger = new KeyEventTrigger<>(KeyEvent.KEY_PRESSED, commandAction);
        Interaction.getTriggers(pane).add(trigger);
        assertEquals(1, InvokeCommandActionList.get(pane).size());
        assertEquals(List.of(), trace);
        commandAction.setCommand(command);
        assertEquals(1, InvokeCommandActionList.get(pane).size());
        assertEquals(List.of("+A"), trace);
        commandAction.setCommand(null);
        assertEquals(1, InvokeCommandActionList.get(pane).size());
        assertEquals(List.of("+A", "-A"), trace);
    }

    @Test
    public void testCommandIsAttachedOnlyOnce() {
        var trace = new ArrayList<String>();
        var pane = new Pane();
        var command = new TestCommand(trace, "A", false);
        var commandAction1 = new InvokeCommandAction(command);
        var trigger1 = new KeyEventTrigger<>(KeyEvent.KEY_PRESSED, commandAction1);
        var commandAction2 = new InvokeCommandAction(command);
        var trigger2 = new KeyEventTrigger<>(KeyEvent.KEY_RELEASED, commandAction2);

        Interaction.getTriggers(pane).addAll(trigger1, trigger2);
        assertEquals(2, InvokeCommandActionList.get(pane).size());
        assertEquals(List.of("+A"), trace);
        commandAction1.setCommand(null);
        assertEquals(2, InvokeCommandActionList.get(pane).size());
        assertEquals(List.of("+A"), trace);
        commandAction2.setCommand(null);
        assertEquals(2, InvokeCommandActionList.get(pane).size());
        assertEquals(List.of("+A", "-A"), trace);

        commandAction1.setCommand(command);
        assertEquals(2, InvokeCommandActionList.get(pane).size());
        assertEquals(List.of("+A", "-A", "+A"), trace);
        commandAction2.setCommand(command);
        assertEquals(2, InvokeCommandActionList.get(pane).size());
        assertEquals(List.of("+A", "-A", "+A"), trace);

        Interaction.getTriggers(pane).remove(trigger1);
        assertEquals(1, InvokeCommandActionList.get(pane).size());
        assertEquals(List.of("+A", "-A", "+A"), trace);
        Interaction.getTriggers(pane).remove(trigger2);
        assertEquals(0, InvokeCommandActionList.get(pane).size());
        assertEquals(List.of("+A", "-A", "+A", "-A"), trace);
    }

}
