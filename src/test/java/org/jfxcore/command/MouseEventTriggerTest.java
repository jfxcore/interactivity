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

import org.jfxcore.command.mocks.TestCountingCommand;
import org.jfxcore.interaction.Interaction;
import org.jfxcore.interaction.MouseEventTrigger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MouseEventTriggerTest {

    static TestCountingCommand command;
    static MouseEventTrigger<Node> trigger;
    static Pane pane;

    @BeforeAll
    static void beforeAll() {
        command = new TestCountingCommand();
        pane = new Pane();
    }

    @BeforeEach
    void beforeEach() {
        command.count = 0;
        trigger = new MouseEventTrigger<>(MouseEvent.MOUSE_PRESSED);
        trigger.getActions().add(new InvokeCommandAction(command));
        Interaction.getTriggers(pane).add(trigger);
    }

    @AfterEach
    void afterEach() {
        Interaction.getTriggers(pane).remove(trigger);
    }

    @Test
    public void testButton() {
        trigger.setButton(MouseButton.PRIMARY);
        Event.fireEvent(pane, createEvent(pane, MouseButton.SECONDARY));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY));
        assertEquals(1, command.count);
    }

    @Test
    public void testShiftDown() {
        trigger.setShiftDown(true);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY, Modifier.SHIFT));
        assertEquals(1, command.count);
    }

    @Test
    public void testControlDown() {
        trigger.setControlDown(true);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY, Modifier.CTRL));
        assertEquals(1, command.count);
    }

    @Test
    public void testAltDown() {
        trigger.setAltDown(true);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY, Modifier.ALT));
        assertEquals(1, command.count);
    }

    @Test
    public void testMetaDown() {
        trigger.setMetaDown(true);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY, Modifier.META));
        assertEquals(1, command.count);
    }

    @Test
    public void testPrimaryButtonDown() {
        trigger.setPrimaryButtonDown(true);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY, Modifier.PRIMARY));
        assertEquals(1, command.count);
    }

    @Test
    public void testSecondaryButtonDown() {
        trigger.setSecondaryButtonDown(true);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY, Modifier.SECONDARY));
        assertEquals(1, command.count);
    }

    @Test
    public void testMiddleButtonDown() {
        trigger.setMiddleButtonDown(true);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY, Modifier.MIDDLE));
        assertEquals(1, command.count);
    }

    @Test
    public void testBackButtonDown() {
        trigger.setBackButtonDown(true);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY, Modifier.BACK));
        assertEquals(1, command.count);
    }

    @Test
    public void testForwardButtonDown() {
        trigger.setForwardButtonDown(true);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, MouseButton.PRIMARY, Modifier.FORWARD));
        assertEquals(1, command.count);
    }

    private static MouseEvent createEvent(EventTarget target, MouseButton button, Modifier... modifiers) {
        List<Modifier> ml = Arrays.asList(modifiers);

        return new MouseEvent(
            null,
            target,
            MouseEvent.MOUSE_PRESSED,
            0, 0, 0, 0,
            button,
            1,
            ml.contains(Modifier.SHIFT),
            ml.contains(Modifier.CTRL),
            ml.contains(Modifier.ALT),
            ml.contains(Modifier.META),
            ml.contains(Modifier.PRIMARY),
            ml.contains(Modifier.MIDDLE),
            ml.contains(Modifier.SECONDARY),
            ml.contains(Modifier.BACK),
            ml.contains(Modifier.FORWARD),
            false, false, false, null);
    }

    private enum Modifier {
        SHIFT, CTRL, ALT, META, PRIMARY, SECONDARY, MIDDLE, FORWARD, BACK
    }

}
