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

public class BehaviorTest {

    @Test
    public void testAddAndRemoveBehavior() {
        var pane = new Pane();
        var behavior = new Behavior<>() {};
        Interaction.getBehaviors(pane).add(behavior);
        assertEquals(List.of(behavior), Interaction.getBehaviors(pane));
        Interaction.getBehaviors(pane).remove(behavior);
        assertEquals(List.of(), Interaction.getBehaviors(pane));
    }

    @Test
    public void testCannotAddBehaviorMultipleTimes() {
        var pane = new Pane();
        var behavior = new Behavior<>() {};
        Interaction.getBehaviors(pane).add(behavior);
        assertThrows(RuntimeException.class, () -> Interaction.getBehaviors(pane).add(behavior));
        Interaction.getBehaviors(pane).remove(behavior);
        assertDoesNotThrow(() -> Interaction.getBehaviors(pane).add(behavior));
    }

    @Test
    public void testCannotAddBehaviorToMultipleNodes() {
        var pane1 = new Pane();
        var pane2 = new Pane();
        var behavior = new Behavior<>() {};
        Interaction.getBehaviors(pane1).add(behavior);
        assertThrows(RuntimeException.class, () -> Interaction.getBehaviors(pane2).add(behavior));
        Interaction.getBehaviors(pane1).remove(behavior);
        assertDoesNotThrow(() -> Interaction.getBehaviors(pane2).add(behavior));
    }

    @Test
    public void testBehaviorThrowingInOnAttachMethodIsCorrectlyAdded() {
        var pane = new Pane();
        var behavior = new Behavior<>() {
            @Override
            protected void onAttached(Node node) {
                throw new RuntimeException("foo");
            }
        };

        try (var handler = UncaughtExceptionHandler.forCurrentThread()) {
            Interaction.getBehaviors(pane).add(behavior);
            assertEquals(1, handler.getExceptions().size());
            assertEquals("foo", handler.getExceptions().get(0).getMessage());
            assertEquals(List.of(behavior), Interaction.getBehaviors(pane));
        }
    }

    @Test
    public void testBehaviorThrowingInOnAttachMethodIsCorrectlySet() {
        var pane = new Pane();
        var behavior1 = new Behavior<>() {
            @Override
            protected void onDetached(Node node) {
                throw new RuntimeException("behavior1");
            }
        };
        var behavior2 = new Behavior<>() {
            @Override
            protected void onAttached(Node node) {
                throw new RuntimeException("behavior2");
            }
        };

        try (var handler = UncaughtExceptionHandler.forCurrentThread()) {
            Interaction.getBehaviors(pane).add(behavior1);
            assertEquals(List.of(behavior1), Interaction.getBehaviors(pane));
            assertEquals(0, handler.getExceptions().size());

            Interaction.getBehaviors(pane).set(0, behavior2);
            assertEquals(1, handler.getExceptions().size());
            assertEquals("behavior2", handler.getExceptions().get(0).getMessage());
            assertEquals(1, handler.getExceptions().get(0).getSuppressed().length);
            assertEquals("behavior1", handler.getExceptions().get(0).getSuppressed()[0].getMessage());
            assertEquals(List.of(behavior2), Interaction.getBehaviors(pane));
        }
    }

    @Test
    public void testBehaviorThrowingInOnDetachMethodIsCorrectlyRemoved() {
        var pane = new Pane();
        var behavior = new Behavior<>() {
            @Override
            protected void onDetached(Node node) {
                throw new RuntimeException("foo");
            }
        };

        try (var handler = UncaughtExceptionHandler.forCurrentThread()) {
            Interaction.getBehaviors(pane).add(behavior);
            assertEquals(List.of(behavior), Interaction.getBehaviors(pane));
            assertEquals(0, handler.getExceptions().size());

            Interaction.getBehaviors(pane).remove(behavior);
            assertEquals(1, handler.getExceptions().size());
            assertEquals("foo", handler.getExceptions().get(0).getMessage());
            assertEquals(List.of(), Interaction.getBehaviors(pane));
        }
    }

}
