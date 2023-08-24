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

package org.jfxcore;

import java.util.ArrayList;
import java.util.List;

public final class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler, AutoCloseable {

    private final Thread.UncaughtExceptionHandler oldHandler;
    private final List<Throwable> exceptions = new ArrayList<>();

    private UncaughtExceptionHandler(Thread.UncaughtExceptionHandler oldHandler) {
        this.oldHandler = oldHandler;
    }

    public static UncaughtExceptionHandler forCurrentThread() {
        Thread currentThread = Thread.currentThread();
        var handler = new UncaughtExceptionHandler(currentThread.getUncaughtExceptionHandler());
        currentThread.setUncaughtExceptionHandler(handler);
        return handler;
    }

    public List<Throwable> getExceptions() {
        return exceptions;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        exceptions.add(e);
    }

    @Override
    public void close() {
        Thread.currentThread().setUncaughtExceptionHandler(oldHandler);
    }

}
