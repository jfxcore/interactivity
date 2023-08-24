/*
 * Copyright (c) 2022, 2023, JFXcore. All rights reserved.
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RelayCommandTest {

    @Test
    public void testCommandIsExecutableByDefault() {
        var command = new RelayCommand<Void>(() -> {});
        assertTrue(command.isExecutable());
    }

    @Test
    public void testExecutableCommandCanBeExecuted() {
        int[] test = new int[1];
        var command = new RelayCommand<Integer>(i -> test[0] = i);
        command.execute(5);
        assertEquals(5, test[0]);
    }

    @Test
    public void testNotExecutableCommandThrowsException() {
        var command = new RelayCommand<Void>(() -> {});
        command.setExecutable(false);
        assertThrows(IllegalStateException.class, () -> command.execute(null));
    }

    @Test
    public void testCommandWithoutExceptionHandlerThrowsException() {
        var command = new RelayCommand<Void>(() -> { throw new RuntimeException("foo"); });
        assertThrows(RuntimeException.class, () -> command.execute(null));
    }

    @Test
    public void testExceptionHandlerAcceptsException() {
        Throwable[] t = new Throwable[1];
        var command = new RelayCommand<Void>(() -> { throw new RuntimeException("foo"); }, ex -> t[0] = ex);
        command.execute(null);
        assertEquals("foo", t[0].getMessage());
    }

}
