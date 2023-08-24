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
import org.jfxcore.command.mocks.TestCommandHandlerBehavior;
import org.jfxcore.interaction.ActionEventTrigger;
import org.jfxcore.interaction.Interaction;
import org.junit.jupiter.api.Test;
import javafx.scene.layout.Pane;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CommandHandlerBehaviorTest {

    @Test
    public void testCommandHandlerIsInvokedWhenCommandIsAttached() {
        var trace = new ArrayList<String>();
        var handler = new TestCommandHandlerBehavior(trace, "H");
        var pane = new Pane();
        Interaction.getBehaviors(pane).add(handler);
        assertEquals(List.of(), trace);
        var trigger = new ActionEventTrigger<>();
        trigger.getActions().add(new InvokeCommandAction(new TestCommand(trace, "A", false)));
        Interaction.getTriggers(pane).add(trigger);
        assertEquals(List.of("+A", "+H"), trace);
        Interaction.getTriggers(pane).remove(trigger);
        assertEquals(List.of("+A", "+H", "-A", "-H"), trace);
    }

    @Test
    public void testCommandHandlerIsInvokedForExistingCommand() {
        var trace = new ArrayList<String>();
        var pane = new Pane();
        var trigger = new ActionEventTrigger<>();
        trigger.getActions().add(new InvokeCommandAction(new TestCommand(trace, "A", false)));
        Interaction.getTriggers(pane).add(trigger);
        assertEquals(List.of("+A"), trace);
        var handler = new TestCommandHandlerBehavior(trace, "H");
        Interaction.getBehaviors(pane).add(handler);
        assertEquals(List.of("+A", "+H"), trace);
    }

    @Test
    public void testRemovedCommandHandlerIsInvoked() {
        var trace = new ArrayList<String>();
        var handler = new TestCommandHandlerBehavior(trace, "H");
        var pane = new Pane();
        Interaction.getBehaviors(pane).add(handler);
        assertEquals(List.of(), trace);
        var trigger = new ActionEventTrigger<>();
        trigger.getActions().add(new InvokeCommandAction(new TestCommand(trace, "A", false)));
        Interaction.getTriggers(pane).add(trigger);
        assertEquals(List.of("+A", "+H"), trace);
        Interaction.getBehaviors(pane).remove(handler);
        assertEquals(List.of("+A", "+H", "-H"), trace);
    }

    @Test
    public void testCommandHandlerCannotBeAddedTwice() {
        var handler = new TestCommandHandlerBehavior(null, null);
        var pane = new Pane();
        Interaction.getBehaviors(pane).add(handler);
        assertThrows(IllegalStateException.class, () -> Interaction.getBehaviors(pane).add(handler));
    }

}
