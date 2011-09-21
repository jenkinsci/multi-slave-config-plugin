/*
 *  The MIT License
 *
 *  Copyright 2011 Sony Ericsson Mobile Communications. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package com.sonyericsson.hudson.plugins.multislaveconfigplugin;

import hudson.Util;
import hudson.model.Slave;
import hudson.os.windows.ManagedWindowsServiceLauncher;
import hudson.slaves.CommandLauncher;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SimpleScheduledRetentionStrategy;

/**
 * Available setting types.
 */
public enum Setting {

    /**
     * Label string, used on all slaves.
     */
    LABELS {
        /**
         * Gets the label string on a specific slave.
         * @param slave which slave to get the setting from
         * @return label string
         */
        String getSettingString(Slave slave) {
            return slave.getLabelString();
        }
    },
    /**
     * Description, used on all slaves.
     */
    DESCRIPTION {
        /**
         * Gets the description of a specific slave.
         * @param slave which slave to get the setting from
         * @return description
         */
        String getSettingString(Slave slave) {
            return slave.getNodeDescription();
        }
    },
    /**
     * Number of executors, used on all slaves.
     */
    NUM_EXECUTORS {
        /**
         * Gets the number of executors on a specific slave.
         * @param slave which slave to get the setting from
         * @return number of executors
         */
        String getSettingString(Slave slave) {
            return String.valueOf(slave.getNumExecutors());
        }
    },
    /**
     * Remote FS, used on all slaves.
     */
    REMOTE_FS {
        /**
         * Gets the remote FS setting on a specific slave.
         * @param slave which slave to get the setting from
         * @return remote fs
         */
        String getSettingString(Slave slave) {
            return slave.getRemoteFS();
        }
    },
    /**
     * Launch command, used within CommandLauncher.
     */
    LAUNCH_COMMAND {
        /**
         * Gets the launch command on a specific slave.
         * @param slave which slave to get the setting from
         * @return launch command
         */
        String getSettingString(Slave slave) {
            return ((CommandLauncher)slave.getLauncher()).getCommand();
        }
    },
    /**
     * Password string, used within ManagedWindowsServiceLauncher.
     */
    PASSWORD_STRING {
        /**
         * Gets the password string on a specific slave.
         * @param slave which slave to get the setting from
         * @return password string
         */
        String getSettingString(Slave slave) {
            return ((ManagedWindowsServiceLauncher)slave.getLauncher()).password.getPlainText();
        }
    },
    /**
     * Username, used within ManagedWindowsServiceLauncher.
     */
    USERNAME {
        /**
         * Gets the username setting on a specific slave.
         * @param slave which slave to get the setting from
         * @return the username setting
         */
        String getSettingString(Slave slave) {
            return ((ManagedWindowsServiceLauncher)slave.getLauncher()).userName;
        }
    },
    /**
     * Tunnel, used within JNLPLauncher.
     */
    TUNNEL {
        /**
         * Gets the tunnel setting on a specific slave.
         * @param slave which slave to get the setting from
         * @return tunnel setting
         */
        String getSettingString(Slave slave) {
            return Util.fixNull(((JNLPLauncher)slave.getLauncher()).tunnel);
        }
    },
    /**
     * Vm args, used within JNLPLauncher.
     */
    VM_ARGS {
        /**
         * Gets the vm args on a specific slave.
         * @param slave which slave to get the setting from
         * @return vm args
         */
        String getSettingString(Slave slave) {
            return Util.fixNull(((JNLPLauncher)slave.getLauncher()).vmargs);
        }
    },
    /**
     * Idle delay, used withing RetentionStrategy.Demand.
     */
    IDLE_DELAY {
        /**
         * Gets the idle delay on a specific slave.
         * @param slave which slave to get the setting from
         * @return idle delay
         */
        String getSettingString(Slave slave) {
            return String.valueOf(((RetentionStrategy.Demand)slave.getRetentionStrategy()).getIdleDelay());
        }
    },
    /**
     * In demand delay setting, used within RetentionStrategy.Demand.
     */
    IN_DEMAND_DELAY {
        /**
         * Gets the in demand delay on a specific slave.
         * @param slave which slave to get the setting from
         * @return in demand delay
         */
        String getSettingString(Slave slave) {
            return String.valueOf(((RetentionStrategy.Demand)slave.getRetentionStrategy()).getInDemandDelay());
        }
    },
    /**
     * Keep up when active setting, used within SimpleScheduledRetentionStrategy.
     */
    KEEP_UP_WHEN_ACTIVE {
        /**
         * Gets the keep up when active setting on a specific slave.
         * @param slave which slave to get the setting from
         * @return keep up when active setting
         */
        String getSettingString(Slave slave) {
            return String.valueOf(((SimpleScheduledRetentionStrategy)slave.getRetentionStrategy()).
                    isKeepUpWhenActive());
        }
    },
    /**
     * Start time specification, used within SimpleScheduledRetentionStrategy.
     */
    START_TIME_SPEC {
        /**
         * Gets the start time specification on a specific slave.
         * @param slave which slave to get the setting from
         * @return start time specification
         */
        String getSettingString(Slave slave) {
            return ((SimpleScheduledRetentionStrategy)slave.getRetentionStrategy()).getStartTimeSpec();
        }

    },
    /**
     * Uptime mins, used within SimpleScheduledRetentionStrategy.
     */
    UPTIME_MINS {
        /**
         * Gets the uptime mins setting on a specific slave.
         * @param slave which slave to get the setting from
         * @return uptime mins string
         */
        String getSettingString(Slave slave) {
            return String.valueOf(((SimpleScheduledRetentionStrategy)slave.getRetentionStrategy()).getUpTimeMins());
        }
    };

    /**
     * Gets the setting string of a setting type on a specific slave
     * @param slave which slave to get the setting from
     * @return the setting string
     */
    abstract String getSettingString(Slave slave);
}
