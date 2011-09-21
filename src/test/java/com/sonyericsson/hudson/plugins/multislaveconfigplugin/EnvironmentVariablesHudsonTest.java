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

import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.os.windows.ManagedWindowsServiceLauncher;
import hudson.slaves.CommandLauncher;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.RetentionStrategy;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.IOException;
import java.util.Collections;

/**
 * Tests {@link EnvironmentVariables} using HudsonTestCase.
 * Using HudsonTestCase since it's hard to create a DumbSlave by mocking only.
 * @author Fredrik Persson &lt;fredrik4.persson@sonyericsson.com&gt;
 */
public class EnvironmentVariablesHudsonTest extends HudsonTestCase {

    private static final String SLAVE_NAME = "slave-name";
    private static final String DESCRIPTION = "description";
    private static final String REMOTE_FS = "remoteFS";
    private static final String LABELS = "labels";
    private static final String EXECUTORS = "1";
    private static final Node.Mode MODE = Node.Mode.NORMAL;
    private static final RetentionStrategy RETENTION_STRATEGY = new RetentionStrategy.Always();
    private static final CommandLauncher LAUNCHER = new CommandLauncher("command");

    /**
     * Creates a new launcher which is the same as the argument launcher,
     * but with the setting strings interpreted from environment variables.
     * @param launcher the ComputerLauncher to base the operation on
     * @return the new launcher
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    private ComputerLauncher modifyLauncherFromVariables(ComputerLauncher launcher) throws Descriptor.FormException,
            IOException {
        DumbSlave slave = new DumbSlave(SLAVE_NAME, DESCRIPTION, REMOTE_FS, EXECUTORS, MODE, LABELS,
                launcher, RETENTION_STRATEGY, Collections.EMPTY_LIST);
        slave = EnvironmentVariables.fromVariables(slave);
        return slave.getLauncher();
    }

    /**
     * Creates a new launcher which is the same as the argument launcher,
     * but with the setting strings interpreted to environment variables.
     * @param launcher the ComputerLauncher to base the operation on
     * @return  the new launcher
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    private ComputerLauncher modifyLauncherToVariables(ComputerLauncher launcher) throws Descriptor.FormException,
            IOException {
        DumbSlave slave = new DumbSlave(SLAVE_NAME, DESCRIPTION, REMOTE_FS, EXECUTORS, MODE, LABELS,
                launcher, RETENTION_STRATEGY, Collections.EMPTY_LIST);
        slave = EnvironmentVariables.toVariables(slave);
        return slave.getLauncher();
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave)} .
     * Converting from environment variables in the node description.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testFromVariablesDescription() throws Descriptor.FormException, IOException {
        String originalDescription = "$NAME-description";

        DumbSlave slave = new DumbSlave(SLAVE_NAME, originalDescription, REMOTE_FS, EXECUTORS, MODE,
                LABELS, LAUNCHER, RETENTION_STRATEGY, Collections.EMPTY_LIST);
        slave = EnvironmentVariables.fromVariables(slave);
        assertEquals(SLAVE_NAME + "-description", slave.getNodeDescription());
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave)}.
     * Converting from environment variables in the remote fs setting.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testFromVariablesRemoteFS() throws Descriptor.FormException, IOException {
        String originalRemoteFS = "$NAME/home";

        DumbSlave slave = new DumbSlave(SLAVE_NAME, DESCRIPTION, originalRemoteFS, EXECUTORS, MODE,
                LABELS, LAUNCHER, RETENTION_STRATEGY, Collections.EMPTY_LIST);
        slave = EnvironmentVariables.fromVariables(slave);
        assertEquals(SLAVE_NAME + "/home", slave.getRemoteFS());
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave)}.
     * Converting from environment variables in the label string.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testFromVariablesLabels() throws Descriptor.FormException, IOException {
        String originalLabels = "$NAME";

        DumbSlave slave = new DumbSlave(SLAVE_NAME, DESCRIPTION, REMOTE_FS, EXECUTORS, MODE,
                originalLabels, LAUNCHER, RETENTION_STRATEGY, Collections.EMPTY_LIST);
        slave = EnvironmentVariables.fromVariables(slave);
        assertEquals(SLAVE_NAME, slave.getLabelString());
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave)}.
     * Converting from environment variables in common strings, making sure that strings without environment
     * variables are unchanged.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testFromVariablesCommonStringsWithoutVariables() throws Descriptor.FormException, IOException {
        DumbSlave slave = new DumbSlave(SLAVE_NAME, DESCRIPTION, REMOTE_FS, EXECUTORS, MODE,
                LABELS, LAUNCHER, RETENTION_STRATEGY, Collections.EMPTY_LIST);
        slave = EnvironmentVariables.fromVariables(slave);
        assertEquals(DESCRIPTION, slave.getNodeDescription());
        assertEquals(REMOTE_FS, slave.getRemoteFS());
        assertEquals(LABELS, slave.getLabelString());
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave)}.
     * Converting from environment variables in the command string.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testFromVariablesCommandLauncherWithVariables() throws Descriptor.FormException, IOException {
        String originalCommand = "$NAME.command";

        CommandLauncher launcher = new CommandLauncher(originalCommand);
        launcher = (CommandLauncher)modifyLauncherFromVariables(launcher);
        assertEquals(SLAVE_NAME + ".command", launcher.getCommand());
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave)}.
     * Converting from environment variables in the command string, making sure that strings without environment
     * variables are untouched.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testFromVariablesCommandLauncherWithoutVariables() throws Descriptor.FormException, IOException {
        String originalCommand = "test.command";

        CommandLauncher launcher = new CommandLauncher(originalCommand);
        launcher = (CommandLauncher)modifyLauncherFromVariables(launcher);
        assertEquals(originalCommand, launcher.getCommand());
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave)}.
     * Converting from environment variables in the username string.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testFromVariablesWindowsLauncherUserNameWithVariables() throws Descriptor.FormException, IOException {
        String originalUsername = "$NAME-admin";
        String password = "password";

        ManagedWindowsServiceLauncher launcher = new ManagedWindowsServiceLauncher(originalUsername, password);
        launcher = (ManagedWindowsServiceLauncher)modifyLauncherFromVariables(launcher);
        assertEquals(SLAVE_NAME + "-admin", launcher.userName);
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave)}.
     * Converting from environment variables in the username string, making sure that strings without environment
     * variables are untouched.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testFromVariablesWindowsLauncherUserNameWithoutVariables() throws Descriptor.FormException,
            IOException {
        String originalUsername = "admin";
        String password = "password";

        ManagedWindowsServiceLauncher launcher = new ManagedWindowsServiceLauncher(originalUsername, password);
        launcher = (ManagedWindowsServiceLauncher)modifyLauncherFromVariables(launcher);
        assertEquals(originalUsername, launcher.userName);
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave)}.
     * Converting from environment variables in the password string.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testFromVariablesWindowsLauncherPasswordWithVariables() throws Descriptor.FormException, IOException {
        String userName = "username";
        String originalPassword = "$NAME-password";

        ManagedWindowsServiceLauncher launcher = new ManagedWindowsServiceLauncher(userName, originalPassword);
        launcher = (ManagedWindowsServiceLauncher)modifyLauncherFromVariables(launcher);
        assertEquals(SLAVE_NAME + "-password", launcher.password.getPlainText());
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave)}.
     * Converting from environment variables in the password string, making sure that strings without environment
     * variables are untouched.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testFromVariablesWindowsLauncherPasswordWithoutVariables() throws Descriptor.FormException,
            IOException {
        String userName = "username";
        String originalPassword = "password";

        ManagedWindowsServiceLauncher launcher = new ManagedWindowsServiceLauncher(userName, originalPassword);
        launcher = (ManagedWindowsServiceLauncher)modifyLauncherFromVariables(launcher);
        assertEquals(originalPassword, launcher.password.getPlainText());
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave)}.
     * Converting from environment variables in the tunnel string.
     * @throws IOException if the slave creation went wrong
     * @throws Descriptor.FormException if the slave creation went wrong
     */
    public void testFromVariablesJNLPLauncherTunnelWithVariables() throws IOException, Descriptor.FormException {
        String originalTunnel = "$NAME-tunnel";
        String vmargs = "vmargs";

        JNLPLauncher launcher = new JNLPLauncher(originalTunnel, vmargs);
        launcher = (JNLPLauncher)modifyLauncherFromVariables(launcher);
        assertEquals(SLAVE_NAME + "-tunnel", launcher.tunnel);
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave)}.
     * Converting from environment variables in the tunnel string, making sure that strings without environment
     * variables are untouched.
     * @throws IOException if the slave creation went wrong
     * @throws Descriptor.FormException if the slave creation went wrong
     */
    public void testFromVariablesJNLPLauncherTunnelWithoutVariables() throws IOException, Descriptor.FormException {
        String originalTunnel = "tunnel";
        String vmargs = "vmargs";

        JNLPLauncher launcher = new JNLPLauncher(originalTunnel, vmargs);
        launcher = (JNLPLauncher)modifyLauncherFromVariables(launcher);
        assertEquals(originalTunnel, launcher.tunnel);
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave)}.
     * Converting from environment variables in the vmargs string.
     * @throws IOException if the slave creation went wrong
     * @throws Descriptor.FormException if the slave creation went wrong
     */
    public void testFromVariablesJNLPLauncherVmargsWithVariables() throws IOException, Descriptor.FormException {
        String tunnel = "tunnel";
        String originalVmargs = "$NAME-vmargs";

        JNLPLauncher launcher = new JNLPLauncher(tunnel, originalVmargs);
        launcher = (JNLPLauncher)modifyLauncherFromVariables(launcher);
        assertEquals(SLAVE_NAME + "-vmargs", launcher.vmargs);
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave)}.
     * Converting from environment variables in the vmargs string, making sure that strings without environment
     * variables are untouched.
     * @throws IOException if the slave creation went wrong
     * @throws Descriptor.FormException if the slave creation went wrong
     */
    public void testFromVariablesJNLPLauncherVmargsWithoutVariables() throws IOException, Descriptor.FormException {
        String tunnel = "tunnel";
        String originalVmargs = "vmargs";

        JNLPLauncher launcher = new JNLPLauncher(tunnel, originalVmargs);
        launcher = (JNLPLauncher)modifyLauncherFromVariables(launcher);
        assertEquals(originalVmargs, launcher.vmargs);
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave)} .
     * Converting to environment variables in the description string.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testToVariablesDescription() throws Descriptor.FormException, IOException {
        String originalDescription = SLAVE_NAME + "-description";

        DumbSlave slave = new DumbSlave(SLAVE_NAME, originalDescription, REMOTE_FS, EXECUTORS, MODE,
                LABELS, LAUNCHER, RETENTION_STRATEGY, Collections.EMPTY_LIST);
        slave = EnvironmentVariables.toVariables(slave);
        assertEquals("$NAME-description", slave.getNodeDescription());
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave)}.
     * Converting to environment variables in the remote fs string.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testToVariablesRemoteFS() throws Descriptor.FormException, IOException {
        String originalRemoteFS = SLAVE_NAME + "/home";

        DumbSlave slave = new DumbSlave(SLAVE_NAME, DESCRIPTION, originalRemoteFS, EXECUTORS, MODE,
                LABELS, LAUNCHER, RETENTION_STRATEGY, Collections.EMPTY_LIST);
        slave = EnvironmentVariables.toVariables(slave);
        assertEquals("$NAME/home", slave.getRemoteFS());
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave)}.
     * Converting to environment variables in the label string.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testToVariablesLabels() throws Descriptor.FormException, IOException {
        String originalLabels = SLAVE_NAME;

        DumbSlave slave = new DumbSlave(SLAVE_NAME, DESCRIPTION, REMOTE_FS, EXECUTORS, MODE,
                originalLabels, LAUNCHER, RETENTION_STRATEGY, Collections.EMPTY_LIST);
        slave = EnvironmentVariables.toVariables(slave);
        assertEquals("$NAME", slave.getLabelString());
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave)}.
     * Converting to environment variables in common strings, making sure that strings without environment
     * variables are untouched.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testToVariablesCommonStringsWithoutVariables() throws Descriptor.FormException, IOException {
        String originalDescription = "description";
        String originalRemoteFS = "remoteFS";
        String originalLabels = "labels";

        DumbSlave slave = new DumbSlave(SLAVE_NAME, originalDescription, originalRemoteFS, EXECUTORS, MODE,
                originalLabels, LAUNCHER, RETENTION_STRATEGY, Collections.EMPTY_LIST);
        slave = EnvironmentVariables.toVariables(slave);
        assertEquals(originalDescription, slave.getNodeDescription());
        assertEquals(originalRemoteFS, slave.getRemoteFS());
        assertEquals(originalLabels, slave.getLabelString());
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave)}.
     * Converting to environment variables in the launch command.
     * @throws IOException if the slave creation went wrong
     * @throws Descriptor.FormException if the slave creation went wrong
     */
    public void testToVariablesCommandLauncherWithVariables() throws IOException, Descriptor.FormException {
        String originalCommand = SLAVE_NAME + ".run";

        CommandLauncher launcher = new CommandLauncher(originalCommand);
        launcher = (CommandLauncher)modifyLauncherToVariables(launcher);
        assertEquals("$NAME.run", launcher.getCommand());
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave)}.
     * Converting to environment variables in the launch command, making sure that strings without environment
     * variables are untouched.
     * @throws IOException if the slave creation went wrong
     * @throws Descriptor.FormException if the slave creation went wrong
     */
    public void testToVariablesCommandLauncherWithoutVariables() throws IOException, Descriptor.FormException {
        String originalCommand = "command";

        CommandLauncher launcher = new CommandLauncher(originalCommand);
        launcher = (CommandLauncher)modifyLauncherToVariables(launcher);
        assertEquals(originalCommand, launcher.getCommand());
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave)}.
     * Converting to environment variables in the username string.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testToVariablesWindowsLauncherUserNameWithVariables() throws Descriptor.FormException, IOException {
        String originalUsername = SLAVE_NAME + "-admin";
        String password = "password";

        ManagedWindowsServiceLauncher launcher = new ManagedWindowsServiceLauncher(originalUsername, password);
        launcher = (ManagedWindowsServiceLauncher)modifyLauncherToVariables(launcher);
        assertEquals("$NAME-admin", launcher.userName);
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave)}.
     * Converting to environment variables in the username string, making sure that strings without environment
     * variables are untouched.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testToVariablesWindowsLauncherUserNameWithoutVariables() throws Descriptor.FormException,
            IOException {
        String originalUsername = "admin";
        String password = "password";

        ManagedWindowsServiceLauncher launcher = new ManagedWindowsServiceLauncher(originalUsername, password);
        launcher = (ManagedWindowsServiceLauncher)modifyLauncherToVariables(launcher);
        assertEquals(originalUsername, launcher.userName);
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave)}.
     * Converting to environment variables in the password string.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testToVariablesWindowsLauncherPasswordWithVariables() throws Descriptor.FormException, IOException {
        String userName = "username";
        String originalPassword = SLAVE_NAME + "-password";

        ManagedWindowsServiceLauncher launcher = new ManagedWindowsServiceLauncher(userName, originalPassword);
        launcher = (ManagedWindowsServiceLauncher)modifyLauncherToVariables(launcher);
        assertEquals("$NAME-password", launcher.password.getPlainText());
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave)}.
     * Converting to environment variables in the password string, making sure that strings without environment
     * variables are untouched.
     * @throws Descriptor.FormException if the slave creation went wrong
     * @throws IOException if the slave creation went wrong
     */
    public void testToVariablesWindowsLauncherPasswordWithoutVariables() throws Descriptor.FormException,
            IOException {
        String userName = "username";
        String originalPassword = "password";

        ManagedWindowsServiceLauncher launcher = new ManagedWindowsServiceLauncher(userName, originalPassword);
        launcher = (ManagedWindowsServiceLauncher)modifyLauncherToVariables(launcher);
        assertEquals(originalPassword, launcher.password.getPlainText());
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave)}.
     * Converting to environment variables in the tunnel string.
     * @throws IOException if the slave creation went wrong
     * @throws Descriptor.FormException if the slave creation went wrong
     */
    public void testToVariablesJNLPLauncherTunnelWithVariables() throws IOException, Descriptor.FormException {
        String originalTunnel = SLAVE_NAME + "-tunnel";
        String vmargs = "vmargs";

        JNLPLauncher launcher = new JNLPLauncher(originalTunnel, vmargs);
        launcher = (JNLPLauncher)modifyLauncherToVariables(launcher);
        assertEquals("$NAME-tunnel", launcher.tunnel);
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave)}.
     * Converting to environment variables in the tunnel string, making sure that strings without environment
     * variables are untouched.
     * @throws IOException if the slave creation went wrong
     * @throws Descriptor.FormException if the slave creation went wrong
     */
    public void testToVariablesJNLPLauncherTunnelWithoutVariables() throws IOException, Descriptor.FormException {
        String originalTunnel = "tunnel";
        String vmargs = "vmargs";

        JNLPLauncher launcher = new JNLPLauncher(originalTunnel, vmargs);
        launcher = (JNLPLauncher)modifyLauncherToVariables(launcher);
        assertEquals(originalTunnel, launcher.tunnel);
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave)}.
     * Converting to environment variables in the vmargs string.
     * @throws IOException if the slave creation went wrong
     * @throws Descriptor.FormException if the slave creation went wrong
     */
    public void testToVariablesJNLPLauncherVmargsWithVariables() throws IOException, Descriptor.FormException {
        String tunnel = "tunnel";
        String originalVmargs = SLAVE_NAME + "-vmargs";

        JNLPLauncher launcher = new JNLPLauncher(tunnel, originalVmargs);
        launcher = (JNLPLauncher)modifyLauncherToVariables(launcher);
        assertEquals("$NAME-vmargs", launcher.vmargs);
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave)}.
     * Converting to environment variables in the vmargs string, making sure that strings without environment
     * variables are untouched.
     * @throws IOException if the slave creation went wrong
     * @throws Descriptor.FormException if the slave creation went wrong
     */
    public void testToVariablesJNLPLauncherVmargsWithoutVariables() throws IOException, Descriptor.FormException {
        String tunnel = "tunnel";
        String originalVmargs = "vmargs";

        JNLPLauncher launcher = new JNLPLauncher(tunnel, originalVmargs);
        launcher = (JNLPLauncher)modifyLauncherToVariables(launcher);
        assertEquals(originalVmargs, launcher.vmargs);
    }

}
