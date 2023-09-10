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
import org.jfxcore.interaction.KeyEventTrigger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class KeyEventTriggerTest {

    static TestCountingCommand command;
    static KeyEventTrigger trigger;
    static Pane pane;

    @BeforeAll
    static void beforeAll() {
        command = new TestCountingCommand();
        pane = new Pane();
    }

    @BeforeEach
    void beforeEach() {
        command.count = 0;
        trigger = new KeyEventTrigger(KeyEvent.KEY_PRESSED);
        trigger.getActions().add(new InvokeCommandAction(command));
        Interaction.getTriggers(pane).add(trigger);
    }

    @AfterEach
    void afterEach() {
        Interaction.getTriggers(pane).remove(trigger);
    }

    @Test
    public void testCharacter() {
        var trigger = new KeyEventTrigger(KeyEvent.KEY_TYPED);
        trigger.getActions().add(new InvokeCommandAction(command));
        trigger.setCharacter("A");
        Interaction.getTriggers(pane).add(trigger);
        Event.fireEvent(pane, createEvent(pane, KeyCode.B, KeyEvent.KEY_TYPED));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, KeyCode.A, KeyEvent.KEY_TYPED));
        assertEquals(1, command.count);
        pane.setOnKeyTyped(null);
    }

    @Test
    public void testKeyCode() {
        trigger.setCode(KeyCode.A);
        Event.fireEvent(pane, createEvent(pane, KeyCode.B, KeyEvent.KEY_PRESSED));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, KeyCode.A, KeyEvent.KEY_PRESSED));
        assertEquals(1, command.count);
    }

    @Test
    public void testShiftDown() {
        trigger.setShiftDown(true);
        Event.fireEvent(pane, createEvent(pane, KeyCode.A, KeyEvent.KEY_PRESSED));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, KeyCode.A, KeyEvent.KEY_PRESSED, KeyModifier.SHIFT));
        assertEquals(1, command.count);
    }

    @Test
    public void testControlDown() {
        trigger.setControlDown(true);
        Event.fireEvent(pane, createEvent(pane, KeyCode.A, KeyEvent.KEY_PRESSED));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, KeyCode.A, KeyEvent.KEY_PRESSED, KeyModifier.CTRL));
        assertEquals(1, command.count);
    }

    @Test
    public void testAltDown() {
        trigger.setAltDown(true);
        Event.fireEvent(pane, createEvent(pane, KeyCode.A, KeyEvent.KEY_PRESSED));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, KeyCode.A, KeyEvent.KEY_PRESSED, KeyModifier.ALT));
        assertEquals(1, command.count);
    }

    @Test
    public void testMetaDown() {
        trigger.setMetaDown(true);
        Event.fireEvent(pane, createEvent(pane, KeyCode.A, KeyEvent.KEY_PRESSED));
        assertEquals(0, command.count);
        Event.fireEvent(pane, createEvent(pane, KeyCode.A, KeyEvent.KEY_PRESSED, KeyModifier.META));
        assertEquals(1, command.count);
    }

    private static KeyEvent createEvent(EventTarget target, KeyCode keyCode,
                                        EventType<KeyEvent> evtType, KeyModifier... modifiers) {
        List<KeyModifier> ml = Arrays.asList(modifiers);

        return new KeyEvent(
            null,
            target,
            evtType,
            evtType == KeyEvent.KEY_TYPED ? keyCode.getChar() : null,
            keyCode.getChar(),
            keyCode,
            ml.contains(KeyModifier.SHIFT),
            ml.contains(KeyModifier.CTRL),
            ml.contains(KeyModifier.ALT),
            ml.contains(KeyModifier.META));
    }

    private enum KeyModifier {
        SHIFT, CTRL, ALT, META
    }

}
