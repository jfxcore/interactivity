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

package org.jfxcore.interaction;

import org.jfxcore.UncaughtExceptionHandler;
import org.junit.jupiter.api.Test;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TriggerTest {

    @Test
    public void testAddAndRemoveTrigger() {
        var pane = new Pane();
        var trigger = new Trigger<>() {};
        Interaction.getTriggers(pane).add(trigger);
        assertEquals(List.of(trigger), Interaction.getTriggers(pane));
        Interaction.getTriggers(pane).remove(trigger);
        assertEquals(List.of(), Interaction.getTriggers(pane));
    }

    @Test
    public void testAddAndRemoveTriggerNonNode() {
        var owner = new Object();
        var trigger = new Trigger<>() {};
        Interaction.getTriggers(owner).add(trigger);
        assertEquals(List.of(trigger), Interaction.getTriggers(owner));
        Interaction.getTriggers(owner).remove(trigger);
        assertEquals(List.of(), Interaction.getTriggers(owner));
    }

    @Test
    public void testCannotAddTriggerMultipleTimes() {
        var pane = new Pane();
        var trigger = new Trigger<>() {};
        Interaction.getTriggers(pane).add(trigger);
        assertThrows(RuntimeException.class, () -> Interaction.getTriggers(pane).add(trigger));
        Interaction.getTriggers(pane).remove(trigger);
        assertDoesNotThrow(() -> Interaction.getTriggers(pane).add(trigger));
    }

    @Test
    public void testCannotAddTriggerToMultipleNodes() {
        var pane1 = new Pane();
        var pane2 = new Pane();
        var trigger = new Trigger<>() {};
        Interaction.getTriggers(pane1).add(trigger);
        assertThrows(RuntimeException.class, () -> Interaction.getTriggers(pane2).add(trigger));
        Interaction.getTriggers(pane1).remove(trigger);
        assertDoesNotThrow(() -> Interaction.getTriggers(pane2).add(trigger));
    }

    @Test
    public void testAddActionInConstructorAndRemoveLater() {
        var action = new TriggerAction<>() {
            @Override protected void onExecute(Object parameter) {}
        };

        var trigger = new Trigger<>(action) {};
        assertEquals(1, trigger.getActions().size());
        assertSame(action, trigger.getActions().get(0));

        trigger.getActions().remove(0);
        assertEquals(0, trigger.getActions().size());
    }

    @Test
    public void testTriggerThrowingInOnAttachMethodIsCorrectlyAdded() {
        var pane = new Pane();
        var trigger = new Trigger<Node, Object>() {
            @Override
            protected void onAttached(Node associatedObject) {
                throw new RuntimeException("foo");
            }
        };

        try (var handler = UncaughtExceptionHandler.forCurrentThread()) {
            Interaction.getTriggers(pane).add(trigger);
            assertEquals(1, handler.getExceptions().size());
            assertEquals("foo", handler.getExceptions().get(0).getMessage());
            assertEquals(List.of(trigger), Interaction.getTriggers(pane));
        }
    }

    @Test
    public void testTriggerThrowingInOnAttachMethodIsCorrectlySet() {
        var pane = new Pane();
        var trigger1 = new Trigger<Node, Object>() {
            @Override
            protected void onDetached(Node associatedObject) {
                throw new RuntimeException("trigger1");
            }
        };
        var trigger2 = new Trigger<Node, Object>() {
            @Override
            protected void onAttached(Node associatedObject) {
                throw new RuntimeException("trigger2");
            }
        };

        try (var handler = UncaughtExceptionHandler.forCurrentThread()) {
            Interaction.getTriggers(pane).add(trigger1);
            assertEquals(List.of(trigger1), Interaction.getTriggers(pane));
            assertEquals(0, handler.getExceptions().size());

            Interaction.getTriggers(pane).set(0, trigger2);
            assertEquals(1, handler.getExceptions().size());
            assertEquals("trigger2", handler.getExceptions().get(0).getMessage());
            assertEquals(1, handler.getExceptions().get(0).getSuppressed().length);
            assertEquals("trigger1", handler.getExceptions().get(0).getSuppressed()[0].getMessage());
            assertEquals(List.of(trigger2), Interaction.getTriggers(pane));
        }
    }

    @Test
    public void testTriggerThrowingInOnDetachMethodIsCorrectlyRemoved() {
        var pane = new Pane();
        var trigger = new Trigger<Node, Object>() {
            @Override
            protected void onDetached(Node associatedObject) {
                throw new RuntimeException("foo");
            }
        };

        try (var handler = UncaughtExceptionHandler.forCurrentThread()) {
            Interaction.getTriggers(pane).add(trigger);
            assertEquals(List.of(trigger), Interaction.getTriggers(pane));
            assertEquals(0, handler.getExceptions().size());

            Interaction.getTriggers(pane).remove(trigger);
            assertEquals(1, handler.getExceptions().size());
            assertEquals("foo", handler.getExceptions().get(0).getMessage());
            assertEquals(List.of(), Interaction.getTriggers(pane));
        }
    }

}
