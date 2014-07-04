/*
 *  The MIT License
 *
 *  Copyright (c) 2011 Sony Mobile Communications Inc. All rights reserved.
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

import antlr.ANTLRException;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.os.windows.ManagedWindowsServiceLauncher;
import hudson.slaves.CommandLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SimpleScheduledRetentionStrategy;
import hudson.tools.ToolLocationNodeProperty;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.PretendSlave;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Tests {@link NodeList} using HudsonTestCases.
 * @author Fredrik Persson &lt;fredrik4.persson@sonyericsson.com&gt;
 */
public class NodeListHudsonTest extends HudsonTestCase {

    NodeList nodeList;
    DumbSlave dumbSlave1;
    DumbSlave dumbSlave2;
    HashMap<String, Object> settings;

    /**
     * Sets up the tests by creating a NodeList and example slaves.
     * @throws Exception if super.setUp() or slave creation goes wrong.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        nodeList = new NodeList();
        dumbSlave1 = new DumbSlave("dumbSlave1", null, null, null, null, null, null, null, Collections.EMPTY_LIST);
        dumbSlave2 = new DumbSlave("dumbSlave2", null, null, null, null, null, null, null, Collections.EMPTY_LIST);
        settings = new HashMap<String, Object>();
    }

    /**
     * Adds two slaves to nodelist and then changes settings on them according to argument.
     * @param applySettings HashMap with settings to apply.
     */
    protected void changeSettingsHelper(HashMap<String, Object> applySettings) {
        nodeList.add(dumbSlave1);
        nodeList.add(dumbSlave2);
        nodeList.changeSettings(applySettings);
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#slavesStillExist()}.
     * Makes sure it works when all slaves still exist.
     * @throws IOException if it fails to add a node.
     */
    public void testSlavesStillExist() throws IOException {
        hudson.addNode(dumbSlave1);
        hudson.addNode(dumbSlave2);

        nodeList.add(dumbSlave1);
        nodeList.add(dumbSlave2);

        assertTrue(nodeList.slavesStillExist());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#slavesStillExist()}.
     * Makes sure it works when one slave has been deleted.
     * @throws IOException if it fails to add a node.
     */
    public void testSlavesStillExistNot() throws IOException {
        //Adding just one slave to the master list, but two to the nodeList
        //simulates that one has been deleted from the system.
        hudson.addNode(dumbSlave1);

        nodeList.add(dumbSlave1);
        nodeList.add(dumbSlave2);

        assertFalse(nodeList.slavesStillExist());
    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.HashMap)}.
     * Makes sure that changing settings on a NodeList containing other than DumbSlaves
     * keeps the non-DumbSlaves untouched.
     * @throws Exception if creating a pretendSlave goes wrong.
     */
    public void testChangeSettingsNonDumbSlave() throws Exception {
        //A new PretendSlave is created with the description "pretending a slave"
        PretendSlave pretendSlave = createPretendSlave(null);
        nodeList.add(dumbSlave1);
        nodeList.add(pretendSlave);

        String description = "This is a hard working slave";
        settings.put("description", description);

        nodeList.changeSettings(settings);
        List<Node> registeredNodes = hudson.getNodes();

        //Checks that the slaves exist only once and that the non-DumbSlave still exists
        assertEquals(2, registeredNodes.size());
        //Checks that the non-DumbSlave is untouched
        assertEquals("pretending a slave", registeredNodes.get(1).getNodeDescription());
    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.HashMap)}.
     * Changes description.
     */
    public void testChangeSettingsDescription() {
        settings.put("description", "This is the new description on $NAME");
        changeSettingsHelper(settings);
        List<Node> registeredNodes = hudson.getNodes();
        assertEquals("This is the new description on dumbSlave1", registeredNodes.get(0).getNodeDescription());
        assertEquals("This is the new description on dumbSlave2", registeredNodes.get(1).getNodeDescription());
    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.HashMap)}.
     * Changes remote FS.
     */
    public void testChangeSettingsRemoteFS() {
        settings.put("remoteFS", "home/$NAME");
        changeSettingsHelper(settings);
        List<Node> registeredNodes = hudson.getNodes();
        assertEquals("home/dumbSlave1", ((DumbSlave)registeredNodes.get(0)).getRemoteFS());
        assertEquals("home/dumbSlave2", ((DumbSlave)registeredNodes.get(1)).getRemoteFS());
    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.HashMap)}.
     * Changes number of executors.
     */
    public void testChangeSettingsNumExecutors() {
        settings.put("numExecutors", "2");
        changeSettingsHelper(settings);
        List<Node> registeredNodes = hudson.getNodes();
        assertEquals(2, ((DumbSlave)registeredNodes.get(0)).getNumExecutors());
        assertEquals(2, ((DumbSlave)registeredNodes.get(1)).getNumExecutors());
    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.HashMap)}.
     * Sets new labels.
     */
    public void testChangeSettingsSetLabels() {
        String label = "LABEL";
        settings.put("setLabelString", label);
        changeSettingsHelper(settings);
        List<Node> registeredNodes = hudson.getNodes();
        assertEquals(label, registeredNodes.get(0).getLabelString());
        assertEquals(label, registeredNodes.get(1).getLabelString());
    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.HashMap)}.
     * Adds new labels, also makes sure that already existing labels are not being removed
     * and that slaves which already have the label doesn't get it twice.
     * @throws Descriptor.FormException if slave creation goes wrong.
     * @throws IOException if slave creation goes wrong.
     */
    public void testChangeSettingsAddLabels() throws Descriptor.FormException, IOException {
        dumbSlave1 = new DumbSlave("dumbSlave1", null, null, null, null, "LABEL1 LABEL2", null, null,
                Collections.EMPTY_LIST);
        //note: dumbSlave2 doesn't have any labels

        settings.put("addLabelString", "LABEL2");
        changeSettingsHelper(settings);
        List<Node> registeredNodes = hudson.getNodes();
        assertEquals("LABEL1 LABEL2", registeredNodes.get(0).getLabelString());
        assertEquals("LABEL2", registeredNodes.get(1).getLabelString());
    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.HashMap)}.
     * Removes labels on slaves, also making sure that the plugin understands that the slaves
     * had different label strings before making the change.
     * @throws Descriptor.FormException if slave creation goes wrong.
     * @throws IOException if slave creation goes wrong.
     */
    public void testChangeSettingsRemoveLabels() throws Descriptor.FormException, IOException {
        dumbSlave1 = new DumbSlave("dumbSlave1", null, null, null, null, "LABEL1 LABEL2 LABEL3", null, null,
                Collections.EMPTY_LIST);
        dumbSlave2 = new DumbSlave("dumbSlave2", null, null, null, null, "LABEL1 LABEL2", null, null,
                Collections.EMPTY_LIST);
        settings.put("removeLabelString", "LABEL1 LABEL2");
        changeSettingsHelper(settings);
        List<Node> registeredNodes = hudson.getNodes();

        assertEquals("LABEL3", registeredNodes.get(0).getLabelString());
        assertEquals("", registeredNodes.get(1).getLabelString());
    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.HashMap)}.
     * Sets mode.
     */
    public void testChangeSettingsMode() {
        settings.put("mode", Node.Mode.EXCLUSIVE);
        changeSettingsHelper(settings);
        List<Node> registeredNodes = hudson.getNodes();

        assertEquals(registeredNodes.get(0).getMode(), Node.Mode.EXCLUSIVE);
        assertEquals(registeredNodes.get(1).getMode(), Node.Mode.EXCLUSIVE);
    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.HashMap)}.
     * Sets launcher.
     */
    public void testChangeSettingsLauncher() {
        CommandLauncher commandLauncher = new CommandLauncher("$NAME.run");
        settings.put("launcher", commandLauncher);
        changeSettingsHelper(settings);
        List<Node> registeredNodes = hudson.getNodes();

        assertEquals("dumbSlave1.run", ((CommandLauncher)((Slave)registeredNodes.get(0)).getLauncher()).getCommand());
        assertEquals("dumbSlave2.run", ((CommandLauncher)((Slave)registeredNodes.get(1)).getLauncher()).getCommand());
    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.HashMap)}.
     * Sets RetentionStrategy.
     * @throws ANTLRException if creating RetentionStrategy goes wrong.
     */
    public void testChangeSettingsRetentionStrategy() throws ANTLRException {
        RetentionStrategy retentionStrategy = new SimpleScheduledRetentionStrategy("* * * * *", 0, true);
        settings.put("retentionStrategy", retentionStrategy);
        changeSettingsHelper(settings);
        List<Node> registeredNodes = hudson.getNodes();

        assertEquals(retentionStrategy, ((Slave)registeredNodes.get(0)).getRetentionStrategy());
        assertEquals(retentionStrategy, ((Slave)registeredNodes.get(1)).getRetentionStrategy());
    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.HashMap)}.
     * Sets node properties for the selected slaves.
     */
    public void testAddOrChangeSettingsNodeProperties() {
        NodeProperty<?> property = new EnvironmentVariablesNodeProperty();
        List<NodeProperty<?>> list = new ArrayList<NodeProperty<?>>();
        list.add(property);
        settings.put("addOrChangeProperties", list);
        changeSettingsHelper(settings);

        List<Node> registeredNodes = hudson.getNodes();

        assertEquals(list, ((Slave)registeredNodes.get(0)).getNodeProperties());
        assertEquals(list, ((Slave)registeredNodes.get(1)).getNodeProperties());

    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.HashMap)}.
     * Removes node properties for the selected slaves.
     * @throws Exception if Settings can't be removed
     */
    public void testRemoveSettingsNodeProperties() throws Exception {

        NodeProperty<?> property = new EnvironmentVariablesNodeProperty();
        List<NodeProperty<?>> list = new ArrayList<NodeProperty<?>>();
        list.add(property);
        settings.put("addOrChangeProperties", list);
        changeSettingsHelper(settings);
        settings.remove("addOrChangeProperties");

        String className = EnvironmentVariablesNodeProperty.class.getName();
        List<String> removeList = new ArrayList<String>();
        removeList.add(className);

        settings.put("removeProperties", removeList);

        nodeList.changeSettings(settings);

        List<Node> registeredNodes = hudson.getNodes();

        assertTrue(((Slave)registeredNodes.get(0)).getNodeProperties().toList().isEmpty());
        assertTrue(((Slave)registeredNodes.get(1)).getNodeProperties().toList().isEmpty());
    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.HashMap)}.
     * Tests that the Node properties are modified according to the desired precedence.
     * Remove should remove only the current properties.
     * Add should replace any current properties.
     */
    public void testPrecedenceOfNodeProperties() {
        nodeList.add(dumbSlave1);
        nodeList.add(dumbSlave2);

        NodeProperty<?> toolLocationNodeProperty = new ToolLocationNodeProperty();
        EnvironmentVariablesNodeProperty environmentVariablesNodeProperty =
                new EnvironmentVariablesNodeProperty(new EnvironmentVariablesNodeProperty.Entry("key1", "value1"));

        List<NodeProperty<?>> list = new ArrayList<NodeProperty<?>>();
        list.add(environmentVariablesNodeProperty);
        list.add(toolLocationNodeProperty);

        settings.put("addOrChangeProperties", list);
        nodeList.changeSettings(settings);
        settings.remove("addOrChangeProperties");

        // Remove the EnvironmentVariablesNodeProperty
        String className = EnvironmentVariablesNodeProperty.DescriptorImpl.class.getName();
        List<String> removeList = new ArrayList<String>();
        removeList.add(className);

        // New EnvironmentVariablesNodeProperty and ToolLocationNodeProperty
        toolLocationNodeProperty = new ToolLocationNodeProperty();
        environmentVariablesNodeProperty = new EnvironmentVariablesNodeProperty(
                new EnvironmentVariablesNodeProperty.Entry("key2", "value2"));

        list = new ArrayList<NodeProperty<?>>();
        list.add(environmentVariablesNodeProperty);
        list.add(toolLocationNodeProperty);

        settings.put("addOrChangeProperties", list);
        settings.put("removeProperties", removeList);

        nodeList.changeSettings(settings);

        List<Node> registeredNodes = hudson.getNodes();

        for (Node node : registeredNodes) {
            List<NodeProperty<?>> nodePropertiesList = node.getNodeProperties().toList();
            assertEquals(list, nodePropertiesList);
            for (NodeProperty property : nodePropertiesList) {
                if (property instanceof EnvironmentVariablesNodeProperty) {
                    environmentVariablesNodeProperty = (EnvironmentVariablesNodeProperty)property;
                    assertTrue(environmentVariablesNodeProperty.getEnvVars().containsKey("key2"));
                    assertEquals("value2", environmentVariablesNodeProperty.getEnvVars().get("key2"));
                }
            }
        }
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getComplementaryNodes()}.
     * Makes sure the tested method returns the correct slave
     * when there should be one "complementary slave".
     * @throws IOException if adding nodes goes wrong.
     */
    public void testGetComplementarySlaves() throws IOException {
        nodeList.add(dumbSlave1);
        hudson.addNode(dumbSlave1);
        hudson.addNode(dumbSlave2);

        assertEquals(1, nodeList.getComplementaryNodes().size());
        assertEquals(dumbSlave2, nodeList.getComplementaryNodes().get(0));
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getLauncher()}.
     * The list contains slaves with totally different launcher types.
     * @throws Descriptor.FormException if Slave creation goes wrong.
     * @throws IOException if Slave creation goes wrong.
     */
    public void testGetLauncherDifferentTypes() throws Descriptor.FormException, IOException {
        Slave slave1 = new DumbSlave("slave1", null, null, null, null, null, new CommandLauncher("launchcommand"),
                null, Collections.EMPTY_LIST);
        Slave slave2 = new DumbSlave("slave2", null, null, null, null, null,
                new ManagedWindowsServiceLauncher("username", "password"), null, Collections.EMPTY_LIST);
        nodeList.add(slave1);
        nodeList.add(slave2);

        assertNull(nodeList.getLauncher());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getLauncher()}.
     * All slaves have CommandLaunchers with same launch commands.
     * @throws Descriptor.FormException if Slave creation goes wrong.
     * @throws IOException if Slave creation goes wrong.
     */
    public void testGetLauncherCommandLauncherSame() throws Descriptor.FormException, IOException {
        String launchCommand = "launchcommand";

        Slave slave1 = new DumbSlave("slave1", null, null, null, null, null, new CommandLauncher(launchCommand),
                null, Collections.EMPTY_LIST);
        Slave slave2 = new DumbSlave("slave2", null, null, null, null, null, new CommandLauncher(launchCommand),
                null, Collections.EMPTY_LIST);
        nodeList.add(slave1);
        nodeList.add(slave2);

        assertEquals(launchCommand, ((CommandLauncher)nodeList.getLauncher()).getCommand());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getLauncher()}.
     * All slaves have CommandLaunchers with different launch commands.
     * @throws Descriptor.FormException if Slave creation goes wrong.
     * @throws IOException if Slave creation goes wrong.
     */
    public void testGetLauncherCommandLauncherDifferent() throws Descriptor.FormException, IOException {
        Slave slave1 = new DumbSlave("slave1", null, null, null, null, null, new CommandLauncher("launchcommand1"),
                null, Collections.EMPTY_LIST);
        Slave slave2 = new DumbSlave("slave2", null, null, null, null, null, new CommandLauncher("launchcommand2"),
                null, Collections.EMPTY_LIST);
        nodeList.add(slave1);
        nodeList.add(slave2);

        assertEquals(CommandLauncher.class, nodeList.getLauncher().getClass());
        assertNull(((CommandLauncher)nodeList.getLauncher()).getCommand());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getLauncher()}.
     * All slaves have ManagedWindowsServiceLaunchers with same user names/passwords.
     * @throws Descriptor.FormException if Slave creation goes wrong.
     * @throws IOException if Slave creation goes wrong.
     */
    public void testGetLauncherManagedWindowsServiceLauncherSame() throws Descriptor.FormException, IOException {
        String userName = "admin";
        String password = "password";

        Slave slave1 = new DumbSlave("slave1", null, null, null, null, null,
                new ManagedWindowsServiceLauncher(userName, password), null, Collections.EMPTY_LIST);
        Slave slave2 = new DumbSlave("slave2", null, null, null, null, null,
                new ManagedWindowsServiceLauncher(userName, password), null, Collections.EMPTY_LIST);
        nodeList.add(slave1);
        nodeList.add(slave2);

        assertEquals(userName, ((ManagedWindowsServiceLauncher)nodeList.getLauncher()).userName);
        assertEquals(password, ((ManagedWindowsServiceLauncher)nodeList.getLauncher()).password.getPlainText());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getLauncher()}.
     * All slaves have ManagedWindowsServiceLauncher with different user names/passwords.
     * @throws Descriptor.FormException if Slave creation goes wrong.
     * @throws IOException if Slave creation goes wrong.
     */
    public void testGetLauncherManagedWindowsServiceLauncherDifferent() throws Descriptor.FormException, IOException {
        Slave slave1 = new DumbSlave("slave1", null, null, null, null, null,
                new ManagedWindowsServiceLauncher("user1", "password1"), null, Collections.EMPTY_LIST);
        Slave slave2 = new DumbSlave("slave2", null, null, null, null, null,
                new ManagedWindowsServiceLauncher("user2", "password2"), null, Collections.EMPTY_LIST);
        nodeList.add(slave1);
        nodeList.add(slave2);

        assertEquals(ManagedWindowsServiceLauncher.class, nodeList.getLauncher().getClass());
        assertNull(((ManagedWindowsServiceLauncher)nodeList.getLauncher()).userName);
        assertEquals("", ((ManagedWindowsServiceLauncher)nodeList.getLauncher()).password.getPlainText());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getLauncher()}.
     * All slaves have ManagedWindowsServiceLaunchers with same tunnel/vmargs settings.
     * @throws Descriptor.FormException if Slave creation goes wrong.
     * @throws IOException if Slave creation goes wrong.
     */
    public void testGetLauncherJNLPLauncherSame() throws Descriptor.FormException, IOException {
        String expectedTunnel = "tunnel";
        String expectedVmargs = "vmargs";

        Slave slave1 = new DumbSlave("slave1", null, null, null, null, null,
                new JNLPLauncher(expectedTunnel, expectedVmargs), null, Collections.EMPTY_LIST);
        Slave slave2 = new DumbSlave("slave2", null, null, null, null, null,
                new JNLPLauncher(expectedTunnel, expectedVmargs), null, Collections.EMPTY_LIST);
        nodeList.add(slave1);
        nodeList.add(slave2);

        assertEquals(expectedTunnel, ((JNLPLauncher)nodeList.getLauncher()).tunnel);
        assertEquals(expectedVmargs, ((JNLPLauncher)nodeList.getLauncher()).vmargs);
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getLauncher()}.
     * All slaves have ManagedWindowsServiceLauncher with different tunnel/vmargs settings.
     * @throws Descriptor.FormException if Slave creation goes wrong.
     * @throws IOException if Slave creation goes wrong.
     */
    public void testGetLauncherJNLPLauncherDifferent() throws Descriptor.FormException, IOException {
        Slave slave1 = new DumbSlave("slave1", null, null, null, null, null,
                new JNLPLauncher("tunnel1", "vmargs1"), null, Collections.EMPTY_LIST);
        Slave slave2 = new DumbSlave("slave2", null, null, null, null, null,
                new JNLPLauncher("tunnel2", "vmargs2"), null, Collections.EMPTY_LIST);
        nodeList.add(slave1);
        nodeList.add(slave2);

        assertEquals(JNLPLauncher.class, nodeList.getLauncher().getClass());
        assertNull(((JNLPLauncher)nodeList.getLauncher()).tunnel);
        assertNull(((JNLPLauncher)nodeList.getLauncher()).vmargs);
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getRetentionStrategy()}.
     * The list contains slaves with totally different retention strategies.
     * @throws Descriptor.FormException if Slave creation goes wrong.
     * @throws IOException if Slave creation goes wrong.
     */
    public void testGetRetentionStrategyDifferentTypes() throws Descriptor.FormException, IOException {
        Slave slave1 = new DumbSlave("slave1", null, null, null, null, null, null, new RetentionStrategy.Always(),
                Collections.EMPTY_LIST);
        Slave slave2 = new DumbSlave("slave2", null, null, null, null, null, null, new RetentionStrategy.Demand(0, 0),
                Collections.EMPTY_LIST);
        nodeList.add(slave1);
        nodeList.add(slave2);

        assertNull(nodeList.getRetentionStrategy());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getLauncher()}.
     * All slaves use RetentionStrategy.Always.
     * @throws Descriptor.FormException if Slave creation goes wrong.
     * @throws IOException if Slave creation goes wrong.
     */
    public void testGetRetentionStrategyAlways() throws Descriptor.FormException, IOException {
        Slave slave1 = new DumbSlave("slave1", null, null, null, null, null, null, new RetentionStrategy.Always(),
                Collections.EMPTY_LIST);
        Slave slave2 = new DumbSlave("slave1", null, null, null, null, null, null, new RetentionStrategy.Always(),
                Collections.EMPTY_LIST);
        nodeList.add(slave1);
        nodeList.add(slave2);

        assertEquals(RetentionStrategy.Always.class, nodeList.getRetentionStrategy().getClass());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getLauncher()}.
     * All slaves have RetentionStrategy.Demand with same delays.
     * @throws Descriptor.FormException if Slave creation goes wrong.
     * @throws IOException if Slave creation goes wrong.
     */
    public void testGetRetentionStrategyDemandSame() throws Descriptor.FormException, IOException {
        Slave slave1 = new DumbSlave("slave1", null, null, null, null, null, null, new RetentionStrategy.Demand(0, 1),
                Collections.EMPTY_LIST);
        Slave slave2 = new DumbSlave("slave2", null, null, null, null, null, null, new RetentionStrategy.Demand(0, 1),
                Collections.EMPTY_LIST);
        nodeList.add(slave1);
        nodeList.add(slave2);

        assertEquals(0, ((RetentionStrategy.Demand)nodeList.getRetentionStrategy()).getInDemandDelay());
        assertEquals(1, ((RetentionStrategy.Demand)nodeList.getRetentionStrategy()).getIdleDelay());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getLauncher()}.
     * All slaves have RetentionStrategy.Demand with different delays.
     * @throws Descriptor.FormException if Slave creation goes wrong.
     * @throws IOException if Slave creation goes wrong.
     */
    public void testGetRetentionStrategyDemandDifferent() throws Descriptor.FormException, IOException {
        Slave slave1 = new DumbSlave("slave1", null, null, null, null, null, null, new RetentionStrategy.Demand(2, 2),
                Collections.EMPTY_LIST);
        Slave slave2 = new DumbSlave("slave2", null, null, null, null, null, null, new RetentionStrategy.Demand(1, 1),
                Collections.EMPTY_LIST);
        nodeList.add(slave1);
        nodeList.add(slave2);

        assertEquals(0, ((RetentionStrategy.Demand)nodeList.getRetentionStrategy()).getInDemandDelay());
        assertEquals(1, ((RetentionStrategy.Demand)nodeList.getRetentionStrategy()).getIdleDelay());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getLauncher()}.
     * All slaves have SimpleScheduledRetentionStrategy with same settings.
     * @throws Descriptor.FormException if Slave creation goes wrong.
     * @throws IOException if Slave creation goes wrong.
     * @throws ANTLRException if Slave creation goes wrong.
     */
    public void testGetRetentionStrategySimpleScheduledSame() throws Descriptor.FormException, IOException,
            ANTLRException {
        Slave slave1 = new DumbSlave("slave1", null, null, null, null, null, null,
                new SimpleScheduledRetentionStrategy("3 * * * *", 2, true), Collections.EMPTY_LIST);
        Slave slave2 = new DumbSlave("slave2", null, null, null, null, null, null,
                new SimpleScheduledRetentionStrategy("3 * * * *", 2, true), Collections.EMPTY_LIST);
        nodeList.add(slave1);
        nodeList.add(slave2);

        assertEquals("3 * * * *", ((SimpleScheduledRetentionStrategy)nodeList.getRetentionStrategy()).
                getStartTimeSpec());
        assertEquals(2, ((SimpleScheduledRetentionStrategy)nodeList.getRetentionStrategy()).getUpTimeMins());
        assertTrue(((SimpleScheduledRetentionStrategy)nodeList.getRetentionStrategy()).isKeepUpWhenActive());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getLauncher()}.
     * All slaves have SimpleScheduledRetentionStrategy with different settings.
     * @throws Descriptor.FormException if Slave creation goes wrong.
     * @throws IOException if Slave creation goes wrong.
     * @throws ANTLRException if Slave creation goes wrong.
     */
    public void testGetRetentionStrategySimpleScheduledDifferent() throws Descriptor.FormException, IOException,
            ANTLRException {
        Slave slave1 = new DumbSlave("slave1", null, null, null, null, null, null,
                new SimpleScheduledRetentionStrategy("5 * * * *", 0, true), Collections.EMPTY_LIST);
        Slave slave2 = new DumbSlave("slave2", null, null, null, null, null, null,
                new SimpleScheduledRetentionStrategy("6 * * * *", 2, false), Collections.EMPTY_LIST);
        nodeList.add(slave1);
        nodeList.add(slave2);

        assertEquals("", ((SimpleScheduledRetentionStrategy)nodeList.getRetentionStrategy()).
                getStartTimeSpec());
        assertEquals(1, ((SimpleScheduledRetentionStrategy)nodeList.getRetentionStrategy()).getUpTimeMins());
        assertTrue(((SimpleScheduledRetentionStrategy)nodeList.getRetentionStrategy()).isKeepUpWhenActive());
    }

}
