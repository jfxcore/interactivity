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
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceCommandTest {

//    private Service<String> createService(Supplier<Task<String>> s) {
//        return new ServiceShim<>() {
//            { setExecutor(Runnable::run); }
//            @Override protected Task<String> createTask() { return s.get(); }
//            @Override public void runLater(Runnable r) { r.run(); }
//            @Override public boolean isFxApplicationThread() { return true; }
//
//        };
//    }
//
//    @Test
//    public void testInitialValues() {
//        var command = new ServiceCommand(createService(() -> new AbstractTask() {
//            @Override protected String call() { return null; }
//            @Override public boolean isFxApplicationThread() { return true; }
//            @Override public void runLater(Runnable r) { r.run(); }
//        }));
//        assertTrue(command.isExecutable());
//        assertFalse(command.isExecuting());
//        assertEquals("", command.getMessage());
//        assertEquals("", command.getTitle());
//        assertEquals(-1, command.getProgress());
//    }
//
//    @Test
//    public void testExecutableCommandCanBeExecuted() {
//        boolean[] test = new boolean[1];
//        var command = new ServiceCommand(createService(() -> new AbstractTask() {
//            @Override protected String call() {
//                test[0] = true; return null;
//            }
//            @Override public boolean isFxApplicationThread() { return true; }
//            @Override public void runLater(Runnable r) { r.run(); }
//        }));
//        command.execute(5);
//        assertTrue(test[0]);
//    }
//
//    @Test
//    public void testNotExecutableCommandThrowsException() {
//        var command = new ServiceCommand(createService(() -> new AbstractTask() {
//            @Override protected String call() { return null; }
//            @Override public boolean isFxApplicationThread() { return true; }
//            @Override public void runLater(Runnable r) { r.run(); }
//        }));
//        command.setExecutable(false);
//        assertThrows(IllegalStateException.class, () -> command.execute(null));
//    }
//
//    @Test
//    public void testCommandWithoutExceptionHandlerDoesNotThrowException() {
//        var command = new ServiceCommand(createService(() -> new AbstractTask() {
//            @Override protected String call() { throw new RuntimeException("foo"); }
//        }));
//        assertDoesNotThrow(() -> command.execute(null));
//    }
//
//    @Test
//    public void testExceptionHandlerAcceptsException() {
//        Throwable[] t = new Throwable[1];
//        var command = new ServiceCommand(createService(() -> new AbstractTask() {
//            @Override protected String call() { throw new RuntimeException("foo"); }
//        }), ex -> t[0] = ex);
//        command.execute(null);
//        assertEquals("foo", t[0].getMessage());
//    }
//
//    @Test
//    public void testServiceState() {
//        var command = new ServiceCommand(createService(() -> new AbstractTask() {
//            @Override protected String call() {
//                updateMessage("testMessage");
//                updateTitle("testTitle");
//                updateProgress(50, 100);
//                return null;
//            }
//        }));
//        var trace = new ArrayList<String>();
//        command.messageProperty().addListener(((observable, oldValue, newValue) -> trace.add("message: " + newValue)));
//        command.titleProperty().addListener(((observable, oldValue, newValue) -> trace.add("title: " + newValue)));
//        command.progressProperty().addListener(((observable, oldValue, newValue) -> trace.add("progress: " + newValue)));
//        command.execute(null);
//        assertEquals(List.of("message: testMessage", "title: testTitle", "progress: 0.5"), trace);
//    }

}
