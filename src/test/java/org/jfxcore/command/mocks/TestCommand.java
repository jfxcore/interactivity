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

package org.jfxcore.command.mocks;

import org.jfxcore.command.AsyncCommand;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import java.util.List;

public class TestCommand extends AsyncCommand {

    private final boolean failOnAttachDetach;
    private final List<String> trace;
    private final String name;
    private final BooleanProperty executable = new SimpleBooleanProperty(this, "executable", true);
    private final BooleanProperty executing = new SimpleBooleanProperty(this, "executing");

    @Override
    public BooleanProperty executableProperty() {
        return executable;
    }

    @Override
    public BooleanProperty executingProperty() {
        return executing;
    }

    @Override
    public void onExecute(Object parameter) {
    }

    @Override
    public void cancel() {
    }

    public TestCommand(List<String> trace, String name, boolean failOnAttachDetach) {
        this.failOnAttachDetach = failOnAttachDetach;
        this.trace = trace;
        this.name = name;
    }

    @Override
    protected void onAttached(Node node) {
        if (trace != null) {
            trace.add("+" + name);
        }

        if (failOnAttachDetach) {
            throw new RuntimeException();
        }
    }

    @Override
    protected void onDetached(Node node) {
        if (trace != null) {
            trace.add("-" + name);
        }

        if (failOnAttachDetach) {
            throw new RuntimeException();
        }
    }

}
