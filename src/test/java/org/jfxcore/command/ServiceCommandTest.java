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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class ServiceCommandTest {

    private Service<String> createService(Supplier<String> s) {
        return new Service<>() {
            @Override
            protected Task<String> createTask() {
                return new Task<>() {
                    @Override
                    protected String call() {
                        return s.get();
                    }
                };
            }

            @Override
            protected void executeTask(Task<String> task) {
                task.run();
            }
        };
    }

    @Test
    public void testInitialValues() {
        var command = new ServiceCommand(createService(() -> null));
        assertTrue(command.isExecutable());
        assertFalse(command.isExecuting());
        assertEquals("", command.getMessage());
        assertEquals("", command.getTitle());
        assertEquals(-1, command.getProgress());
    }

    @Test
    public void testExecutableCommandCanBeExecuted(FxRobot robot) {
        robot.interact(() -> {
            boolean[] test = new boolean[1];
            var command = new ServiceCommand(createService(() -> {
                test[0] = true;
                return null;
            }));
            command.execute(5);
            assertTrue(test[0]);
        });
    }

    @Test
    public void testNotExecutableCommandThrowsException() {
        var command = new ServiceCommand(createService(() -> null));
        command.setExecutable(false);
        assertThrows(IllegalStateException.class, () -> command.execute(null));
    }

    @Test
    public void testCommandWithoutExceptionHandlerDoesNotThrowException() {
        var command = new ServiceCommand(createService(() -> { throw new RuntimeException("foo"); }));
        assertDoesNotThrow(() -> command.execute(null));
    }

    @Test
    public void testExceptionHandlerAcceptsException(FxRobot robot) {
        Throwable[] exception = new Throwable[1];
        robot.interact(() -> {
            var command = new ServiceCommand(createService(() -> {
                throw new RuntimeException("foo");
            }), ex -> {
                exception[0] = ex;
            });

            command.execute(null);
        });

        assertEquals("foo", exception[0].getMessage());
    }

    @Test
    public void testServiceState(FxRobot robot) {
        robot.interact(() -> {
            var service = new Service<>() {
                @Override
                protected Task<Object> createTask() {
                    return new Task<>() {
                        @Override
                        protected Object call() {
                            updateMessage("testMessage");
                            updateTitle("testTitle");
                            updateProgress(50, 100);
                            return null;
                        }
                    };
                }

                @Override
                protected void executeTask(Task<Object> task) {
                    task.run();
                }
            };

            var command = new ServiceCommand(service);
            var trace = new ArrayList<String>();
            command.messageProperty().addListener(((observable, oldValue, newValue) -> trace.add("message: " + newValue)));
            command.titleProperty().addListener(((observable, oldValue, newValue) -> trace.add("title: " + newValue)));
            command.progressProperty().addListener(((observable, oldValue, newValue) -> trace.add("progress: " + newValue)));
            command.execute(null);

            assertEquals(List.of("message: testMessage", "title: testTitle", "progress: 0.5"), trace);
        });
    }

}
