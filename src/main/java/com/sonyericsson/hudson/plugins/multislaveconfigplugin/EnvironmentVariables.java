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
import hudson.model.Descriptor;
import hudson.model.Failure;
import hudson.os.windows.ManagedWindowsServiceLauncher;
import hudson.slaves.CommandLauncher;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.JNLPLauncher;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used for interpreting environment variables.
 * @author Fredrik Persson &lt;fredrik4.persson@sonyericsson.com&gt;
 */
public class EnvironmentVariables {

    private static final Logger logger = Logger.getLogger(EnvironmentVariables.class.getName());

    /**
     * Not allowing to create instances of this class.
     */
    protected EnvironmentVariables() {
        throw new UnsupportedOperationException();
    }

    /**
     * Switches to environment variables in the argument string.
     * For example the actual node name becomes $NAME if the argument string contains such
     * @param slave the slave to base the environment variables on
     * @param interpretString the string to interpret
     * @return interpreted string
     */
    public static String toVariables(DumbSlave slave, String interpretString) {
        if (interpretString != null) {
            //Interprets node name:
            interpretString = toVariables(slave.getNodeName(), interpretString);
            //More environment variables to interpret..
        }
        return interpretString;
    }

    /**
     * Switches from environment variables in the argument string.
     * For example $NAME becomes the actual node name if the argument string contains such
     * @param slave the DumbSlave to base the environment variables on
     * @param interpretString the string to interpret
     * @return interpreted string
     */
    public static String fromVariables(DumbSlave slave, String interpretString) {
        if (interpretString != null) {
            //Interprets node name:
            interpretString = fromVariables(slave.getNodeName(), interpretString);
            //More environment variables to interpret..
        }
        return interpretString;
    }

    /**
     * Switches to environment variables in the argument string.
     * The actual node name becomes $NAME if the argument string contains such.
     * This method is used when no slave has been created with the particular name yet,
     * but environment variable interpreting is needed anyway.
     * @param slaveName the name of the slave to base the environment variables on
     * @param interpretString the string to interpret
     * @return interpreted string
     */
    public static String toVariables(String slaveName, String interpretString) {
        if (interpretString != null) {
            interpretString = interpretString.replace(slaveName, "$NAME");
            //More environment variables to interpret..
        }
        return interpretString;
    }

    /**
     * Switches from environment variables in the argument string.
     * For example $NAME becomes the actual node name if the argument string contains such.
     * This method is used when no slave has been created with the particular name yet,
     * but environment variable interpreting is needed anyway.
     * @param slaveName  the name of the slave to base the environment variables on
     * @param interpretString the string to interpret
     * @return interpreted string
     */
    public static String fromVariables(String slaveName, String interpretString) {
        if (interpretString != null) {
            interpretString = interpretString.replace("$NAME", slaveName);
            //More environment variables to interpret..
        }
        return interpretString;
    }

    /**
     * Checks if the argument string contains environment variables, for example $NAME.
     * @param interpretString the string to check for environment variables
     * @return true or false if the string contained environment variables
     */
    public static boolean containsEnvironmentVariables(String interpretString) {
        return interpretString.contains("$NAME");
        //More environment variables to interpret..
    }

    /**
     * Switches from environment variables in most configuration fields for the slave and returns a new slave.
     * For example, if the description contains $NAME,
     * a new slave with the actual node name as description will be returned.
     * @param slave the slave to interpret environment variables on
     * @return a new slave with environment variables interpreted, otherwise same settings
     */
    public static DumbSlave fromVariables(DumbSlave slave) {
        ComputerLauncher launcher = slave.getLauncher();
        String description = slave.getNodeDescription();
        String remoteFS = slave.getRemoteFS();
        String labels = slave.getLabelString();

        if (launcher instanceof CommandLauncher) {
            String command = ((CommandLauncher)launcher).getCommand();
            if (containsEnvironmentVariables(command)) {
                command = fromVariables(slave, command);
                launcher = new CommandLauncher(command);
            }
        } else if (launcher instanceof ManagedWindowsServiceLauncher) {
            String password = ((ManagedWindowsServiceLauncher)launcher).password.getPlainText();
            String userName = ((ManagedWindowsServiceLauncher)launcher).userName;
            if (containsEnvironmentVariables(password)
                    || EnvironmentVariables.containsEnvironmentVariables(userName)) {
                password = fromVariables(slave, password);
                userName = fromVariables(slave, userName);
                launcher = new ManagedWindowsServiceLauncher(userName, password);
            }
        } else if (launcher instanceof JNLPLauncher) {
            String tunnel = Util.fixNull(((JNLPLauncher)launcher).tunnel);
            String vmargs = Util.fixNull(((JNLPLauncher)launcher).vmargs);
            if (containsEnvironmentVariables(tunnel)
                    || containsEnvironmentVariables(vmargs)) {
                tunnel = fromVariables(slave, tunnel);
                vmargs = fromVariables(slave, vmargs);
                launcher = new JNLPLauncher(tunnel, vmargs);
            }
        }
        description = fromVariables(slave, description);
        remoteFS = fromVariables(slave, remoteFS);
        labels = fromVariables(slave, labels);

        try {
            return new DumbSlave(slave.getNodeName(), description, remoteFS, String.valueOf(slave.getNumExecutors()),
                    slave.getMode(), labels, launcher, slave.getRetentionStrategy(),
                    slave.getNodeProperties().toList());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to interpret environment variables on slave");
            throw new Failure(Messages.FailedToInterpretEnvVars());
        }  catch (Descriptor.FormException e) {
            logger.log(Level.WARNING, "Failed to interpret environment variables on slave");
            throw new Failure(Messages.FailedToInterpretEnvVars());
        }
    }

    /**
     * Switches to environment variables in most configuration fields for the slave and returns a new slave.
     * For example, if the description contains the actual node name,
     * a new slave with $NAME as description will be returned.
     * @param slave the slave to interpret environment variables on
     * @return a new slave with environment variables interpreted, otherwise same settings
     */
    public static DumbSlave toVariables(DumbSlave slave) {
        ComputerLauncher launcher = slave.getLauncher();
        String description = slave.getNodeDescription();
        String remoteFS = slave.getRemoteFS();
        String labels = slave.getLabelString();

        if (launcher instanceof CommandLauncher) {
            String command = ((CommandLauncher)launcher).getCommand();
            command = toVariables(slave, command);
            launcher = new CommandLauncher(command);
        } else if (launcher instanceof ManagedWindowsServiceLauncher) {
            String password = ((ManagedWindowsServiceLauncher)launcher).password.getPlainText();
            String userName = ((ManagedWindowsServiceLauncher)launcher).userName;
            password = toVariables(slave, password);
            userName = toVariables(slave, userName);
            launcher = new ManagedWindowsServiceLauncher(userName, password);
        } else if (launcher instanceof JNLPLauncher) {
            String tunnel = Util.fixNull(((JNLPLauncher)launcher).tunnel);
            String vmargs = Util.fixNull(((JNLPLauncher)launcher).vmargs);
            tunnel = toVariables(slave, tunnel);
            vmargs = toVariables(slave, vmargs);
            launcher = new JNLPLauncher(tunnel, vmargs);
        }
        description = toVariables(slave, description);
        remoteFS = toVariables(slave, remoteFS);
        labels = toVariables(slave, labels);

        try {
            return new DumbSlave(slave.getNodeName(), description, remoteFS, String.valueOf(slave.getNumExecutors()),
                    slave.getMode(), labels, launcher, slave.getRetentionStrategy(),
                    slave.getNodeProperties().toList());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to interpret environment variables on slave");
            throw new Failure(Messages.FailedToInterpretEnvVars());
        }  catch (Descriptor.FormException e) {
            logger.log(Level.WARNING, "Failed to interpret environment variables on slave");
            throw new Failure(Messages.FailedToInterpretEnvVars());
        }
    }
}
