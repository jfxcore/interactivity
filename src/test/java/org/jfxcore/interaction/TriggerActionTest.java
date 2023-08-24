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

package org.jfxcore.interaction;

import org.jfxcore.UncaughtExceptionHandler;
import org.junit.jupiter.api.Test;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TriggerActionTest {

    @Test
    public void testAddAndRemoveTriggerAction() {
        var trigger = new Trigger<>() {};
        var action = new TriggerAction<>() {
            @Override protected void onExecute(Object parameter) {}
        };
        trigger.getActions().add(action);
        assertEquals(List.of(action), trigger.getActions());
        trigger.getActions().remove(action);
        assertEquals(List.of(), trigger.getActions());
    }

    @Test
    public void testCannotAddTriggerActionMultipleTimes() {
        var trigger = new Trigger<>() {};
        var action = new TriggerAction<>() {
            @Override protected void onExecute(Object parameter) {}
        };
        trigger.getActions().add(action);
        assertThrows(RuntimeException.class, () -> trigger.getActions().add(action));
        trigger.getActions().remove(action);
        assertDoesNotThrow(() -> trigger.getActions().add(action));
    }

    @Test
    public void testCannotAddTriggerActionToMultipleTriggers() {
        var trigger1 = new Trigger<>() {};
        var trigger2 = new Trigger<>() {};
        var action = new TriggerAction<>() {
            @Override protected void onExecute(Object parameter) {}
        };
        trigger1.getActions().add(action);
        assertThrows(RuntimeException.class, () -> trigger2.getActions().add(action));
        trigger1.getActions().remove(action);
        assertDoesNotThrow(() -> trigger2.getActions().add(action));
    }

    @Test
    public void testTriggerActionThrowingInOnAttachMethodIsCorrectlyAdded() {
        var node = new Pane();
        var trigger = new Trigger<>() {};
        var action = new TriggerAction<>() {
            @Override
            protected void onExecute(Object parameter) {}

            @Override
            protected void onAttached(Node node) {
                throw new RuntimeException("foo");
            }
        };

        try (var handler = UncaughtExceptionHandler.forCurrentThread()) {
            trigger.getActions().add(action);
            assertEquals(0, handler.getExceptions().size());

            Interaction.getTriggers(node).add(trigger);
            assertEquals(1, handler.getExceptions().size());
            assertEquals("foo", handler.getExceptions().get(0).getMessage());
            assertEquals(List.of(action), trigger.getActions());
            assertEquals(List.of(trigger), Interaction.getTriggers(node));
        }
    }

    @Test
    public void testTriggerActionThrowingInOnAttachMethodIsCorrectlySet() {
        var pane = new Pane();
        var trigger = new Trigger<>() {};
        var action1 = new TriggerAction<>() {
            @Override
            protected void onExecute(Object parameter) {}

            @Override
            protected void onDetached(Node node) {
                throw new RuntimeException("trigger1");
            }
        };
        var action2 = new TriggerAction<>() {
            @Override
            protected void onExecute(Object parameter) {}

            @Override
            protected void onAttached(Node node) {
                throw new RuntimeException("trigger2");
            }
        };

        Interaction.getTriggers(pane).add(trigger);

        try (var handler = UncaughtExceptionHandler.forCurrentThread()) {
            trigger.getActions().add(action1);
            assertEquals(List.of(action1), trigger.getActions());
            assertEquals(0, handler.getExceptions().size());

            trigger.getActions().set(0, action2);
            assertEquals(1, handler.getExceptions().size());
            assertEquals("trigger2", handler.getExceptions().get(0).getMessage());
            assertEquals(1, handler.getExceptions().get(0).getSuppressed().length);
            assertEquals("trigger1", handler.getExceptions().get(0).getSuppressed()[0].getMessage());
            assertEquals(List.of(action2), trigger.getActions());
        }
    }

    @Test
    public void testTriggerActionThrowingInOnDetachMethodIsCorrectlyRemoved() {
        var pane = new Pane();
        var trigger = new Trigger<>() {};
        var action = new TriggerAction<>() {
            @Override
            protected void onExecute(Object parameter) {}

            @Override
            protected void onDetached(Node node) {
                throw new RuntimeException("foo");
            }
        };

        Interaction.getTriggers(pane).add(trigger);

        try (var handler = UncaughtExceptionHandler.forCurrentThread()) {
            trigger.getActions().add(action);
            assertEquals(List.of(action), trigger.getActions());
            assertEquals(0, handler.getExceptions().size());

            trigger.getActions().remove(action);
            assertEquals(1, handler.getExceptions().size());
            assertEquals("foo", handler.getExceptions().get(0).getMessage());
            assertEquals(List.of(), trigger.getActions());
        }
    }

}
