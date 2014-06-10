/*
 *  The MIT License
 *
 *  Copyright 2011 Sony Ericsson Mobile Communications. All rights reserved.
 *  Copyright 2014 Sony Mobile Communications AB. All rights reserved.
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
import hudson.model.Failure;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.os.windows.ManagedWindowsServiceLauncher;
import hudson.slaves.CommandLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SimpleScheduledRetentionStrategy;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink.ICON;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink.URL;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink.UserMode.CONFIGURE;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink.UserMode.DELETE;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink.UserMode.ADD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
/**
 * Tests the {@link NodeManageLink} using JUnit Tests.
 * @author Nicklas Nilsson &lt;nicklas3.nilsson@sonyericsson.com&gt;
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DumbSlave.class, Hudson.class, StaplerRequest.class, HttpSession.class, JSONObject.class,
        Stapler.class })
public class NodeManageLinkTest {

    private DumbSlave dumbSlaveMock;
    private Hudson hudsonMock;
    private NodeManageLink nodeManageLink;
    private StaplerRequest staplerRequestMock;
    private HttpSession httpSessionMock;
    private StaplerResponse staplerResponse;
    private JSONObject jsonObjectMock;
    private Stapler staplerMock;
    private HashSet<String> names;
    /**
     * Adds a configured DumbSlave for Jenkins before starting the tests.
     * Also creates a dummy JSON object that has potential search strings.
     * @throws ServletException if so.
     */
    @Before
    public void setup() throws ServletException {

        names = new HashSet<String>();
        nodeManageLink = new NodeManageLink();

        //DumbSlave mock
        dumbSlaveMock = PowerMockito.mock(DumbSlave.class);
        when(dumbSlaveMock.getNodeName()).thenReturn("TestSlave");
        when(dumbSlaveMock.getNodeDescription()).thenReturn("This is the description");
        when(dumbSlaveMock.getRemoteFS()).thenReturn("/jenkins/root");
        when(dumbSlaveMock.getNumExecutors()).thenReturn(1);
        when(dumbSlaveMock.getLabelString()).thenReturn("BUILDNODE");

        //JSON mock
        jsonObjectMock = mock(JSONObject.class);

        //Hudson instance mock
        hudsonMock = mock(Hudson.class);
        mockStatic(Hudson.class);
        when(Hudson.getInstance()).thenReturn(hudsonMock);
        when(hudsonMock.getNodes()).thenReturn(Collections.<Node>singletonList(dumbSlaveMock));

        //HTTP Session mock
        httpSessionMock = mock(HttpSession.class);
        when(httpSessionMock.getId()).thenReturn("currentUserId");

        //StaplerRequest mock
        staplerRequestMock = mock(StaplerRequest.class);
        when(staplerRequestMock.getSession()).thenReturn(httpSessionMock);
        when(staplerRequestMock.getSubmittedForm()).thenReturn(jsonObjectMock);

        //Stapler mock
        staplerMock = mock(Stapler.class);
        mockStatic(Stapler.class);
        when(Stapler.getCurrentRequest()).thenReturn(staplerRequestMock);

        //StaplerResponse mock
        staplerResponse = mock(StaplerResponse.class);

    }
    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink#getIconFileName()}.
     * Checks that the returned icon is correct.
     */
    @Test
    public void testGetIconFileName() {
        assertEquals(ICON, nodeManageLink.getIconFileName());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink#getUrlName()}.
     * Checks that the returned URL is correct.
     */
    @Test
    public void testGetUrlName() {
        assertEquals(URL, nodeManageLink.getUrlName());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink#getDisplayName()}.
     * Checks that the returned display name is correct.
     */
    @Test
    public void testGetDisplayName() {
        assertEquals(Messages.Name(), nodeManageLink.getDisplayName());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink#getDescription()}.
     * Checks that the returned description is correct.
     */
    @Test
    public void testGetDescription() {
        assertEquals(Messages.Description(), nodeManageLink.getDescription());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink#isConfigureMode()}.
     * Checks that IsConfigureMode returns true when userMode is set to configure.
     */
    @Test
    public void testIsConfigureMode() {
        nodeManageLink.userMode.put("currentUserId", CONFIGURE);
        assertTrue(nodeManageLink.isConfigureMode());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink#isConfigureMode()}.
     * Checks that IsConfigureMode returns false when userMode is set to anything but configure.
     */
    @Test
    public void testIsConfigureModeFalse() {
        nodeManageLink.userMode.put("currentUserId", DELETE);
        assertFalse(nodeManageLink.isConfigureMode());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink#isDeleteMode()}.
     * Checks that isDeleteMode returns true when userMode is set to delete.
     */
    @Test
    public void testIsDeleteMode() {
        nodeManageLink.userMode.put("currentUserId", DELETE);
        assertTrue(nodeManageLink.isDeleteMode());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink#isDeleteMode()}.
     * Checks that IsDeleteMode returns false when userMode is set to anything but delete.
     */
    @Test
    public void testIsDeleteModeFalse() {
        nodeManageLink.userMode.put("currentUserId", CONFIGURE);
        assertFalse(nodeManageLink.isDeleteMode());
    }

    /**
     * Tests {@link NodeManageLink#generateStars(int)}.
     * Checks that the right amount of stars is generated, and that it only is stars.
     */
    @Test
    public void testGenerateStars() {
        final int testNumber = 3;
        String stars = nodeManageLink.generateStars(testNumber);
        assertEquals(stars, "***");
    }

    /**
     * Tests {@link NodeManageLink.UserMode}.
     * Check that you can set the usermode configure to a user.
     */
    @Test
    public void testPutUserModeConfigure() {
        nodeManageLink.userMode.put("currentUserId", CONFIGURE);
        assertEquals(nodeManageLink.userMode.get("currentUserId"), CONFIGURE);
    }

    /**
     * Tests {@link NodeManageLink.UserMode}.
     * Check that you can set the usermode delete to a user.
     */
    @Test
    public void testPutUserModeDelete() {
        nodeManageLink.userMode.put("currentUserId", DELETE);
        assertEquals(nodeManageLink.userMode.get("currentUserId"), DELETE);
    }

    /**
     * Tests {@link NodeManageLink.UserMode}.
     * Check that you can set the usermode add to a user.
     */
    @Test
    public void testPutUserModeAdd() {
        nodeManageLink.userMode.put("currentUserId", ADD);
        assertEquals(nodeManageLink.userMode.get("currentUserId"), ADD);
    }

    /**
     * Tests {@link NodeManageLink#doSearch(String, net.sf.json.JSONObject)}.
     * Checks that a hudson Failure is being thrown when redirecting without having any slaves in the system and
     * usermode is configure.
     * @throws IOException if so.
     */
    @Test (expected = Failure.class)
    public void testDoSearchRedirectTestEmptyList() throws IOException {
        nodeManageLink.userMode.put("currentUserId", CONFIGURE);
        when(hudsonMock.getNodes()).thenReturn(Collections.<Node>emptyList());
        nodeManageLink.doConfigureRedirect(staplerRequestMock, staplerResponse);
    }

    /**
     * Tests {@link NodeManageLink#doDeleteRedirect
     * (org.kohsuke.stapler.StaplerRequest, org.kohsuke.stapler.StaplerResponse)}.
     * Checks that a hudson Failure is being thrown when redirecting without having any slaves in the system and
     * usermode is delete.
     * @throws IOException if so.
     */
    @Test (expected = Failure.class)
    public void testDoDeleteRedirectEmptyList() throws IOException {
        nodeManageLink.userMode.put("currentUserId", DELETE);
        when(hudsonMock.getNodes()).thenReturn(Collections.<Node>emptyList());
        nodeManageLink.doDeleteRedirect(staplerRequestMock, staplerResponse);
    }

    /**
     * Tests {@link NodeManageLink#getNodeList(String)}.
     * Checks that nothing is returned when trying to get a NodeList that not exist.
     */
    @Test
    public void testGetNodeList() {
        assertNull(nodeManageLink.getNodeList("differentUserId"));
    }

    /**
     * Tests {@link NodeManageLink#getSlaveNames(String, String, String, String)}.
     * Test with null nodeNames string parameter.
     */
    @Test
    public void testGetSlaveNamesNullSlaveNames() {
        names = nodeManageLink.getSlaveNames(null, "name", "0", "1");
        assertEquals(2, names.size());
        assertTrue(names.contains("name0"));
        assertTrue(names.contains("name1"));
    }

    /**
     * Tests {@link NodeManageLink#getSlaveNames(String, String, String, String)}.
     * Test with null nodeName, first and last string parameters.
     */
    @Test
    public void testGetSlaveNamesNullNodeName() {
        names = nodeManageLink.getSlaveNames("Slave Slave2", null, null, null);
        assertEquals(2, names.size());
        assertTrue(names.contains("Slave"));
        assertTrue(names.contains("Slave2"));
    }

    /**
     * Tests {@link NodeManageLink#getSlaveNames(String, String, String, String)}.
     * Test with non digit string parameter. Shall throw failure.
     */
    @Test (expected = Failure.class)
    public void testGetSlaveNamesNotANumber() {
        names = nodeManageLink.getSlaveNames("Slave Slave2", "Slave", "Non digit", "Non digit");
    }

    /**
     * Tests {@link NodeManageLink#getSlaveNames(String, String, String, String)}.
     * Shall throw failure when first int is bigger than last int.
     */
    @Test (expected = Failure.class)
    public void testGetSlaveNamesWrongIntervalSpan() {
        names = nodeManageLink.getSlaveNames("", "Slave", "5", "1");
    }

    /**
     * Tests {@link NodeManageLink#getSlaveNames(String, String, String, String)}.
     * Shall throw failure when you try to add a slave with the same name as one that already exist.
     */
    @Test (expected = Failure.class)
    public void testGetSlaveNamesExistingNodeName() {
        when(hudsonMock.getNode("TestSlave")).thenReturn(dumbSlaveMock);
        names = nodeManageLink.getSlaveNames("TestSlave", "Slave", "1", "5");
    }

    /**
     * Tests{@link NodeManageLink#isManagedWindowsServiceLauncher(hudson.slaves.ComputerLauncher)}.
     * Testing that a ManagedWindowsServiceLauncher makes this method return true.
     */
    @Test
    public void testIsManagedWindowsServiceLauncher() {
        assertTrue(nodeManageLink.isManagedWindowsServiceLauncher(new ManagedWindowsServiceLauncher("", "")));
    }

    /**
     * Tests{@link NodeManageLink#isCommandLauncher(hudson.slaves.ComputerLauncher)}.
     * Testing that a CommandLauncher makes this method return true.
     */
    @Test
    public void testIsCommandLauncher() {
        assertTrue(nodeManageLink.isCommandLauncher(new CommandLauncher("")));
    }

    /**
     * Tests{@link NodeManageLink#isJNLPLauncher(hudson.slaves.ComputerLauncher)}.
     * Testing that a JNLPLauncher makes this method return true.
     */
    @Test
    public void testIsJNLPLauncher() {
        assertTrue(nodeManageLink.isJNLPLauncher(new JNLPLauncher("", "")));
    }

    /**
     * Tests{@link NodeManageLink#isRetentionStrategyAlways(hudson.slaves.RetentionStrategy)}.
     * Testing that a RetentionStrategy with mode always makes this method return true.
     */
    @Test
    public void testIsRetentionStrategyAlways() {
        assertTrue(nodeManageLink.isRetentionStrategyAlways(new RetentionStrategy.Always()));
    }

    /**
     * Tests{@link NodeManageLink#isRetentionStrategyDemand(hudson.slaves.RetentionStrategy)}.
     * Testing that a RetentionStrategy with mode demand makes this method return true.
     */
    @Test
    public void testIsRetentionStrategyDemand() {
        assertTrue(nodeManageLink.isRetentionStrategyDemand(new RetentionStrategy.Demand(1, 2)));
    }

    /**
     * Tests{@link NodeManageLink#isRetentionStrategyDemand(hudson.slaves.RetentionStrategy)}.
     * Testing that a SimpleScheduledRetentionStrategy makes this method return true.
     * @throws ANTLRException if so.
     */
    @Test
    public void testIsSimpleScheduledRetentionStrategy() throws ANTLRException {
        assertTrue(nodeManageLink.isSimpleScheduledRetentionStrategy(
                new SimpleScheduledRetentionStrategy("", 1, true)));
    }

    /**
     * Tests{@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink#getAllNodes()} )}.
     * Shall return all slaves in the system, currently one.
     */
    @Test
    public void testGetAllNodes() {
        assertEquals(1, nodeManageLink.getAllNodes().size());
    }

    /**
     * Tests{@link NodeManageLink#doSelectSlaves
     * (org.kohsuke.stapler.StaplerRequest, org.kohsuke.stapler.StaplerResponse)} .
     * Tests that this method throws a failure when get("selectedSlaves") return null.
     * @throws IOException if so.
     */
    @Test (expected = Failure.class)
    public void testDoSelectNodesJsonNull() throws IOException {
        when(jsonObjectMock.get("selectedSlaves")).thenReturn(null);
        nodeManageLink.doSelectSlaves(staplerRequestMock, staplerResponse);
    }
}

