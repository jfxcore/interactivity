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

package org.jfxcore.command;

import org.jfxcore.interaction.TriggerAction;
import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.NamedArg;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.event.Event;

/**
 * An action that invokes a {@link Command}.
 * <p>
 * By default, this action disables its associated object when the command is not executable or while it is running.
 * This behavior can be toggled with the {@link InvokeCommandAction#disabledWhenExecuting} property.
 */
@DefaultProperty("command")
public class InvokeCommandAction extends TriggerAction<Object, Object> {

    private class DisabledValue extends ReadOnlyBooleanPropertyBase implements InvalidationListener {
        boolean disabledWhenNotExecutable = true;
        boolean disabledWhenExecuting = true;
        boolean executing;
        boolean executable;

        void set(boolean executable, boolean executing) {
            boolean oldValue = get();
            this.executable = executable;
            this.executing = executing;
            fireValueChangedEvent(oldValue);
        }

        @Override
        public Object getBean() {
            return InvokeCommandAction.this;
        }

        @Override
        public String getName() {
            return "disabled";
        }

        @Override
        public boolean get() {
            return !executable && disabledWhenNotExecutable || executing && disabledWhenExecuting;
        }

        @Override
        public Boolean getValue() {
            return get();
        }

        @Override
        public void invalidated(Observable observable) {
            boolean current = get();
            boolean value = ((ObservableBooleanValue)observable).get();

            if (observable == InvokeCommandAction.this.disabledWhenExecuting) {
                disabledWhenExecuting = value;
            } else if (observable == InvokeCommandAction.this.disabledWhenNotExecutable) {
                disabledWhenNotExecutable = value;
            } else {
                Command command = getCommand();
                if (command != null) {
                    if (observable == command.executableProperty()) {
                        executable = value;
                    } else if (command instanceof AsyncCommand c && observable == c.executingProperty()) {
                        executing = value;
                    }
                }
            }

            fireValueChangedEvent(current);
        }

        private void fireValueChangedEvent(boolean oldValue) {
            if (oldValue != get()) {
                fireValueChangedEvent();
            }
        }
    }

    private final DisabledValue disabled = new DisabledValue();

    ReadOnlyBooleanProperty disabledProperty() {
        return disabled;
    }

    private class CommandProperty extends ObjectPropertyBase<Command> {
        Command currentValue;

        @Override
        public Object getBean() {
            return InvokeCommandAction.this;
        }

        @Override
        public String getName() {
            return "command";
        }

        @Override
        protected void invalidated() {
            boolean newExecutable = false;
            boolean newExecuting = false;

            if (currentValue != null) {
                currentValue.executableProperty().removeListener(disabled);
            }

            if (currentValue instanceof AsyncCommand c) {
                c.executingProperty().removeListener(disabled);
            }

            currentValue = get();

            if (currentValue != null) {
                currentValue.executableProperty().addListener(disabled);
                newExecutable = currentValue.isExecutable();
            }

            if (currentValue instanceof AsyncCommand c) {
                c.executingProperty().addListener(disabled);
                newExecuting = c.isExecuting();
            }

            disabled.set(newExecutable, newExecuting);
        }
    }

    /**
     * The command that is invoked by this {@code InvokeCommandAction}.
     *
     * @defaultValue {@code null}
     */
    private final CommandProperty command = new CommandProperty();

    public final ObjectProperty<Command> commandProperty() {
        return command;
    }

    public final Command getCommand() {
        return command.get();
    }

    public final void setCommand(Command command) {
        this.command.set(command);
    }

    /**
     * The parameter that is passed to the {@link Command#execute(Object)} method when the command is invoked.
     * When set to a non-null value, this property takes precedence over {@link #passEventToCommand}.
     *
     * @defaultValue {@code null}
     */
    private ObjectProperty<Object> parameter;

    public final ObjectProperty<Object> parameterProperty() {
        if (parameter == null) {
            parameter = new SimpleObjectProperty<>(this, "parameter");
        }

        return parameter;
    }

    public final Object getParameter() {
        return parameter != null ? parameter.get() : null;
    }

    public final void setParameter(Object parameter) {
        if (this.parameter != null || parameter != null) {
            parameterProperty().set(parameter);
        }
    }

    /**
     * Indicates whether the {@link Event} that caused the command to be invoked will be passed
     * to the {@link Command#execute(Object)} method as a parameter. The value of this property
     * is ignored if the {@link #parameter} property is set to a non-null value.
     *
     * @defaultValue false
     */
    private BooleanProperty passEventToCommand;

    public final BooleanProperty passEventToCommandProperty() {
        if (passEventToCommand == null) {
            passEventToCommand = new SimpleBooleanProperty(this, "passEventToCommand");
        }

        return passEventToCommand;
    }

    public final boolean isPassEventToCommand() {
        return passEventToCommand != null && passEventToCommand.get();
    }

    public final void setPassEventToCommand(boolean value) {
        if (passEventToCommand != null || value) {
            passEventToCommandProperty().set(value);
        }
    }

    /**
     * Indicates whether the object to which this {@code InvokeCommandAction} applies will be
     * disabled for the duration of the command execution. If this property is set to {@code false},
     * the associated object can invoke an {@code AsyncCommand} while it is currently running, which
     * causes {@link AsyncCommand#cancel()} to be called instead.
     *
     * @defaultValue true
     */
    private BooleanProperty disabledWhenExecuting;

    public final BooleanProperty disabledWhenExecutingProperty() {
        if (disabledWhenExecuting == null) {
            disabledWhenExecuting = new SimpleBooleanProperty(this, "disabledWhenExecuting", true) {
                @Override
                protected void invalidated() {
                    disabled.invalidated(this);
                }
            };
        }

        return disabledWhenExecuting;
    }

    public final boolean isDisabledWhenExecuting() {
        return disabledWhenExecuting == null || disabledWhenExecuting.get();
    }

    public final void setDisabledWhenExecuting(boolean value) {
        if (disabledWhenExecuting != null || !value) {
            disabledWhenExecutingProperty().set(value);
        }
    }

    /**
     * Indicates whether the object to which this {@code InvokeCommandAction} applies will be
     * disabled when the command is not executable. If this property is set to {@code false}, the
     * command will not be executed if it is not executable at the time the action is triggered.
     *
     * @defaultValue true
     */
    private BooleanProperty disabledWhenNotExecutable;

    public final BooleanProperty disabledWhenNotExecutableProperty() {
        if (disabledWhenNotExecutable == null) {
            disabledWhenNotExecutable = new SimpleBooleanProperty(this, "disabledWhenNotExecutable", true) {
                @Override
                protected void invalidated() {
                    disabled.invalidated(this);
                }
            };
        }

        return disabledWhenNotExecutable;
    }

    public final boolean isDisabledWhenNotExecutable() {
        return disabledWhenNotExecutable == null || disabledWhenNotExecutable.get();
    }

    public final void setDisabledWhenNotExecutable(boolean value) {
        if (disabledWhenNotExecutable != null || !value) {
            disabledWhenNotExecutableProperty().set(value);
        }
    }

    /**
     * Initializes a new {@code InvokeCommandAction} instance.
     */
    public InvokeCommandAction() {}

    /**
     * Initializes a new {@code InvokeCommandAction} instance.
     *
     * @param command the command
     */
    public InvokeCommandAction(@NamedArg("command") Command command) {
        setCommand(command);
    }

    /**
     * Initializes a new {@code InvokeCommandAction} instance.
     *
     * @param command the command
     * @param parameter the command parameter
     */
    public InvokeCommandAction(@NamedArg("command") Command command, @NamedArg("parameter") Object parameter) {
        setCommand(command);
        setParameter(parameter);
    }

    @Override
    protected final void onExecute(Object parameter) {
        Command command = getCommand();
        if (command != null && command.isExecutable()) {
            if (command instanceof AsyncCommand ac && ac.isExecuting()) {
                ac.cancel();
            } else {
                Object actualParam = getParameter();
                if (actualParam == null && isPassEventToCommand() && parameter instanceof Event event) {
                    actualParam = event;
                }

                command.execute(actualParam);
            }
        }
    }

    @Override
    protected final void onAttached(Object associatedObject) {
        InvokeCommandActionList.get(associatedObject).add(this);
    }

    @Override
    protected final void onDetached(Object associatedObject) {
        InvokeCommandActionList.get(associatedObject).remove(this);
    }

}
