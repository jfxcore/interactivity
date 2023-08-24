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

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;

/**
 * Contains a set of standard capabilities that can be reported by commands.
 * <p>
 * Using standard capabilities allows application authors to depend on standard interfaces,
 * rather than concrete command implementations.
 */
public sealed interface StandardCommandCapabilities
        permits StandardCommandCapabilities.Message,
                StandardCommandCapabilities.Title,
                StandardCommandCapabilities.Progress {

    /**
     * Marks a command that implements the <em>message</em> capability.
     */
    non-sealed interface Message extends StandardCommandCapabilities {
        /**
         * Gets a property that represents the reported message of the command.
         *
         * @return the {@code message} property
         */
        ReadOnlyStringProperty messageProperty();

        /**
         * Gets the reported message of the command.
         *
         * @return the message or {@code null}
         */
        String getMessage();
    }

    /**
     * Marks a command that implements the <em>title</em> capability.
     */
    non-sealed interface Title extends StandardCommandCapabilities {
        /**
         * Gets a property that represents the reported title of the command.
         *
         * @return the {@code title} property
         */
        ReadOnlyStringProperty titleProperty();

        /**
         * Gets the reported title of the command.
         *
         * @return the title or {@code null}
         */
        String getTitle();
    }

    /**
     * Marks a command that implements the <em>progress</em> capability.
     */
    non-sealed interface Progress extends StandardCommandCapabilities {
        /**
         * Gets a property that indicates the execution progress of the operation,
         * ranging from 0 (inclusive) to 1 (inclusive). If the progress cannot be
         * determined, the value is -1.
         *
         * @return the {@code progress} property
         */
        ReadOnlyDoubleProperty progressProperty();

        /**
         * Gets the execution progress of the command.
         *
         * @return the execution progress, ranging from 0 (inclusive) to 1 (inclusive),
         *         or -1 if the progress cannot be determined
         */
        double getProgress();
    }

}
