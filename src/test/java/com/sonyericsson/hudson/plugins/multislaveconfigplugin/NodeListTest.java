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

import hudson.model.Failure;
import hudson.model.Node;
import hudson.os.windows.ManagedWindowsServiceLauncher;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SimpleScheduledRetentionStrategy;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.PretendSlave;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.Setting.DESCRIPTION;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.Setting.LABELS;
import static hudson.model.Node.Mode.EXCLUSIVE;
import static hudson.model.Node.Mode.NORMAL;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests {@link NodeList} using JUnit Tests..
 * @author Fredrik Persson &lt;fredrik4.persson@sonyericsson.com&gt;
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DumbSlave.class, Stapler.class, StaplerRequest.class, HttpSession.class })
public class NodeListTest {

    DumbSlave dumbSlave1;
    DumbSlave dumbSlave2;
    DumbSlave dumbSlave3;
    NodeList nodeList;

    /**
     * Sets up the test by creating a new node list and creating 3 DumbSlaves.
     */
    @Before
    public void setUp() {
        nodeList = new NodeList();
        dumbSlave1 = mock(DumbSlave.class);
        dumbSlave2 = mock(DumbSlave.class);
        dumbSlave3 = mock(DumbSlave.class);
        when(dumbSlave1.getNodeName()).thenReturn("dumbSlave1");
        when(dumbSlave2.getNodeName()).thenReturn("dumbSlave2");
        when(dumbSlave3.getNodeName()).thenReturn("dumbSlave3");
    }

    /**
     * Returns a JSONObject with basic settings.
     * A submitted form from the jelly page could look like this.
     * @return JSONObject with basic settings.
     */
    protected static JSONObject getBasicSettings() {
        JSONObject json = new JSONObject();

        json.put("_description", false);
        json.put("_remoteFS", false);
        json.put("_numExecutors", false);
        json.put("_mode", false);
        json.put("_labelString", false);
        json.put("_addLabelString", false);
        json.put("_removeLabelString", false);
        json.put("_launcher", false);
        json.put("_retentionStrategy", false);

        json.put("description", "This is a slave");
        json.put("remoteFS", "home");
        json.put("numExecutors", "13");
        json.put("labelString", "LABEL1");
        json.put("addLabelString", "LABEL2");
        json.put("removeLabelString", "LABEL1");

        JSONObject launcher = new JSONObject();
        launcher.put("password", "password");
        launcher.put("stapler-class", ManagedWindowsServiceLauncher.class);
        launcher.put("userName", "admin");
        json.put("launcher", launcher);

        JSONObject retentionStrategy = new JSONObject();
        retentionStrategy.put("keepUpWhenActive", true);
        retentionStrategy.put("stapler-class", SimpleScheduledRetentionStrategy.class);
        retentionStrategy.put("startTimeSpec", "* * * * *");
        retentionStrategy.put("upTimeMins", "0");
        json.put("retentionStrategy", retentionStrategy);

        return json;
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#isEmpty()}.
     * Makes sure it works with a totally empty nodelist.
     */
    @Test
    public void testIsEmptyTotallyEmpty() {
        assertTrue(nodeList.isEmpty());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#isEmpty()}.
     * Makes sure it works when only non DumbSlaves are in the nodeList.
     */
    @Test
    public void testIsEmptyHasOnlyNonSlaves() {
        AbstractCloudSlave abstractCloudSlave = mock(AbstractCloudSlave.class);
        PretendSlave pretendSlave = mock(PretendSlave.class);
        nodeList.add(abstractCloudSlave);
        nodeList.add(pretendSlave);
        assertTrue(nodeList.isEmpty());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#isEmpty()}.
     * Makes sure it works when mixed nodes are in the nodeList.
     */
    @Test
    public void testIsEmptyHasMixedNodes() {
        AbstractCloudSlave abstractCloudSlave = mock(AbstractCloudSlave.class);
        PretendSlave pretendSlave = mock(PretendSlave.class);
        nodeList.add(abstractCloudSlave);
        nodeList.add(pretendSlave);
        nodeList.add(dumbSlave1);
        assertFalse(nodeList.isEmpty());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#isEmpty()}.
     * Makes sure it works when only DumbSlaves are in the list.
     */
    @Test
    public void testIsEmptyHasOnlyDumbSlaves() {
        nodeList.add(dumbSlave1);
        assertFalse(nodeList.isEmpty());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#sortByName()}.
     * Adds three slaves unsorted, then sorts the list.
     */
    @Test
    public void testSortByName() {
        //Adding the slaves unsorted
        nodeList.add(dumbSlave2);
        nodeList.add(dumbSlave3);
        nodeList.add(dumbSlave1);

        nodeList = nodeList.sortByName();

        //Makes sure they are sorted
        assertEquals(dumbSlave1, nodeList.get(0));
        assertEquals(dumbSlave2, nodeList.get(1));
        assertEquals(dumbSlave3, nodeList.get(2));
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#sortByName()}.
     * Tries to sort null objects.
     */
    @Test
    public void testSortByNameNull() {
        //Adding the slaves unsorted
        nodeList.add(null);
        nodeList.add(dumbSlave2);
        nodeList.add(dumbSlave1);
        nodeList.add(null);

        //Adding the slaves sorted
        List<Node> sortedList = new ArrayList<Node>();
        sortedList.add(null);
        sortedList.add(null);
        sortedList.add(dumbSlave1);
        sortedList.add(dumbSlave2);

        nodeList = nodeList.sortByName();

        //Checks that the list contains the same elements in same order
        assertEquals(sortedList.size(), nodeList.size());
        for (int i = 0; i < sortedList.size(); i++) {
            assertEquals(sortedList.get(i), nodeList.get(i));
        }
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#toString()}.
     * Tries to do a String representation of a list containing two slaves.
     */
    @Test
    public void testToString() {
        nodeList.add(dumbSlave1);
        nodeList.add(dumbSlave2);

        String nodeListString = nodeList.toString();
        assertEquals("dumbSlave1 dumbSlave2", nodeListString);
    }


    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getFirstSlave()}.
     * Tries to get the first dumb slave when the list contains other slave types as well.
     */
    @Test
    public void testGetFirstSlaveMixed() {
        AbstractCloudSlave abstractCloudSlave = mock(AbstractCloudSlave.class);

        nodeList.add(abstractCloudSlave);
        nodeList.add(null);
        nodeList.add(dumbSlave1);

        assertEquals(dumbSlave1, nodeList.getFirstSlave());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getFirstSlave()}.
     * Runs without any slaves in the list.
     */
    @Test
    public void testGetFirstSlaveEmpty() {
        assertNull(nodeList.getFirstSlave());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getFirstSlave()}.
     * Runs without any slaves of type DumbSlaves in the list.
     */
    @Test
    public void testGetFirstSlaveNonDumbSlaves() {
        AbstractCloudSlave abstractCloudSlave = mock(AbstractCloudSlave.class);
        PretendSlave pretendSlave = mock(PretendSlave.class);

        nodeList.add(abstractCloudSlave);
        nodeList.add(pretendSlave);

        assertNull(nodeList.getFirstSlave());
    }

    /**
     * Tests {@link NodeList#hasLabels(String)}.
     * Both the argument labels should exist on slaves in the list.
     */
    @Test
    public void testHasLabels() {
        when(dumbSlave1.getLabelString()).thenReturn("LABEL1");
        when(dumbSlave2.getLabelString()).thenReturn("LABEL2");
        nodeList.add(dumbSlave1);
        nodeList.add(dumbSlave2);

        assertTrue(nodeList.hasLabels("LABEL1 LABEL2"));
    }

    /**
     * Tests {@link NodeList#hasLabels(String)}.
     * The argument label should not exist on any slave.
     */
    @Test
    public void testHasLabelsNot() {
        when(dumbSlave1.getLabelString()).thenReturn("LABEL1");
        when(dumbSlave2.getLabelString()).thenReturn("LABEL2");
        nodeList.add(dumbSlave1);
        nodeList.add(dumbSlave2);

        assertFalse(nodeList.hasLabels("LABEL3"));
    }

    /**
     * Tests {@link NodeList#hasLabels(String)}.
     * Covers the null case.
     */
    @Test
    public void testHasLabelsNull() {
        assertTrue(nodeList.hasLabels(null));
    }

    /**
     * Tests {@link NodeList#interpretJSON(net.sf.json.JSONObject)}.
     * Description is checked.
     */
    @Test
    public void testInterpretJSONDescription() {
        JSONObject json = getBasicSettings();
        json.put("_description", true);
        HashMap interpretedJSON = NodeList.interpretJSON(json);

        assertEquals(1, interpretedJSON.size());
        assertEquals("This is a slave", interpretedJSON.get("description"));
    }

    /**
     * Tests {@link NodeList#interpretJSON(net.sf.json.JSONObject)}.
     * Remote FS is checked.
     */
    @Test
    public void testInterpretJSONRemoteFS() {
        JSONObject json = getBasicSettings();
        json.put("_remoteFS", true);
        HashMap interpretedJSON = NodeList.interpretJSON(json);

        assertEquals(1, interpretedJSON.size());
        assertEquals("home", interpretedJSON.get("remoteFS"));
    }

    /**
     * Tests {@link NodeList#interpretJSON(net.sf.json.JSONObject)}.
     * Number of executors is checked.
     */
    @Test
    public void testInterpretJSONExecutors() {
        JSONObject json = getBasicSettings();
        json.put("_numExecutors", true);
        HashMap interpretedJSON = NodeList.interpretJSON(json);

        assertEquals(1, interpretedJSON.size());
        assertEquals("13", interpretedJSON.get("numExecutors"));
    }

    /**
     * Tests {@link NodeList#interpretJSON(net.sf.json.JSONObject)}.
     * Mode is checked and NORMAL.
     */
    @Test
    public void testInterpretJSONNormalMode() {
        JSONObject json = getBasicSettings();
        json.put("_mode", true);
        json.put("mode", "NORMAL");
        HashMap interpretedJSON = NodeList.interpretJSON(json);

        assertEquals(1, interpretedJSON.size());
        assertEquals(NORMAL, interpretedJSON.get("mode"));
    }

    /**
     * Tests {@link NodeList#interpretJSON(net.sf.json.JSONObject)}.
     * Mode is checked and EXCLUSIVE.
     */
    @Test
    public void testInterpretJSONExclusiveMode() {
        JSONObject json = getBasicSettings();
        json.put("_mode", true);
        json.put("mode", "EXCLUSIVE");
        HashMap interpretedJSON = NodeList.interpretJSON(json);

        assertEquals(1, interpretedJSON.size());
        assertEquals(EXCLUSIVE, interpretedJSON.get("mode"));
    }

    /**
     * Tests {@link NodeList#interpretJSON(net.sf.json.JSONObject)}.
     * Mode is checked and invalid.
     */
    @Test (expected = Failure.class)
    public void testInterpretJSONInvalidMode() {
        JSONObject json = getBasicSettings();
        json.put("_mode", true);
        json.put("mode", "Invalid");
        NodeList.interpretJSON(json);
    }

    /**
     * Tests {@link NodeList#interpretJSON(net.sf.json.JSONObject)}.
     * Set label string is checked.
     */
    @Test
    public void testInterpretJSONSetLabelString() {
        JSONObject json = getBasicSettings();
        json.put("_labelString", true);
        HashMap interpretedJSON = NodeList.interpretJSON(json);

        assertEquals(1, interpretedJSON.size());
        assertEquals("LABEL1", interpretedJSON.get("setLabelString"));
    }

    /**
     * Tests {@link NodeList#interpretJSON(net.sf.json.JSONObject)}.
     * Add label string is checked.
     */
    @Test
    public void testInterpretJSONAddLabelString() {
        JSONObject json = getBasicSettings();
        json.put("_addLabelString", true);
        HashMap interpretedJSON = NodeList.interpretJSON(json);

        assertEquals(1, interpretedJSON.size());
        assertEquals("LABEL2", interpretedJSON.get("addLabelString"));
    }

    /**
     * Tests {@link NodeList#interpretJSON(net.sf.json.JSONObject)}.
     * Remove label string is checked.
     */
    @Test
    public void testInterpretJSONRemoveLabelString() {
        JSONObject json = getBasicSettings();
        json.put("_removeLabelString", true);
        HashMap interpretedJSON = NodeList.interpretJSON(json);

        assertEquals(1, interpretedJSON.size());
        assertEquals("LABEL1", interpretedJSON.get("removeLabelString"));
    }

    /**
     * Tests {@link NodeList#interpretJSON(net.sf.json.JSONObject)}.
     * Launcher is checked.
     */
    @Test
    public void testInterpretJSONLauncher() {
        JSONObject json = getBasicSettings();
        json.put("_launcher", true);

        StaplerRequest staplerRequestMock = mock(StaplerRequest.class);
        Stapler staplerMock = mock(Stapler.class);
        mockStatic(Stapler.class);
        when(staplerMock.getCurrentRequest()).thenReturn(staplerRequestMock);

        NodeList.interpretJSON(json);

        Mockito.verify(staplerRequestMock).bindJSON(
                Mockito.eq(ComputerLauncher.class),
                Mockito.eq((JSONObject)json.get("launcher"))
        );
    }

    /**
     * Tests {@link NodeList#interpretJSON(net.sf.json.JSONObject)}.
     * Retention Strategy is checked.
     */
    @Test
    public void testInterpretJSONRetentionStrategy() {
        JSONObject json = getBasicSettings();
        json.put("_retentionStrategy", true);

        StaplerRequest staplerRequestMock = mock(StaplerRequest.class);
        Stapler staplerMock = mock(Stapler.class);
        mockStatic(Stapler.class);
        when(staplerMock.getCurrentRequest()).thenReturn(staplerRequestMock);

        NodeList.interpretJSON(json);

        Mockito.verify(staplerRequestMock).bindJSON(
                Mockito.eq(RetentionStrategy.class),
                Mockito.eq((JSONObject)json.get("retentionStrategy"))
        );
    }

    /**
     * Tests {@link NodeList#getCommon(Setting)}.
     * Test for empty list.
     */
    @Test (expected = Failure.class)
    public void testGetCommonEmptyList() {
        PretendSlave pretendSlave = mock(PretendSlave.class);
        nodeList.add(pretendSlave);

        //Should cast Failure since the list contains no DumbSlaves
        nodeList.getCommon(LABELS);
    }

    /**
     * Tests {@link NodeList#getCommon(Setting)}.
     * The slaves have a common label String without environment variables.
     */
    @Test
    public void testGetCommonHasSameNonEnvVar() {
        String label = "LABEL";
        when(dumbSlave1.getLabelString()).thenReturn(label);
        when(dumbSlave2.getLabelString()).thenReturn(label);
        nodeList.add(dumbSlave1);
        nodeList.add(dumbSlave2);

        assertEquals(label, nodeList.getCommon(LABELS));
    }

    /**
     * Tests {@link NodeList#getCommon(Setting)}.
     * The slaves don't have a common label String.
     */
    @Test
    public void testGetCommonHasNotSame() {
        when(dumbSlave1.getLabelString()).thenReturn("LABEL1");
        when(dumbSlave2.getLabelString()).thenReturn("LABEL2");
        nodeList.add(dumbSlave1);
        nodeList.add(dumbSlave2);

        assertNull(nodeList.getCommon(LABELS));
    }

    /**
     * Tests {@link NodeList#getCommon(Setting)}.
     * The slaves have a common label String with environment variables.
     */
    @Test
    public void testGetCommonSameIsEnvVar() {
        when(dumbSlave1.getNodeDescription()).thenReturn("Description about dumbSlave1.");
        when(dumbSlave2.getNodeDescription()).thenReturn("Description about dumbSlave2.");
        nodeList.add(dumbSlave1);
        nodeList.add(dumbSlave2);

        assertEquals("Description about $NAME.", nodeList.getCommon(DESCRIPTION));
    }

    /**
     * Tests {@link NodeList#getCommon(Setting)}.
     * Special case where the description could be interpreted as an environment variable,
     * but should not.
     */
    @Test
    public void testGetCommonHasSameSpecialCase() {
        when(dumbSlave1.getNodeDescription()).thenReturn("Description about dumbSlave1.");
        when(dumbSlave2.getNodeDescription()).thenReturn("Description about dumbSlave1.");
        nodeList.add(dumbSlave1);
        nodeList.add(dumbSlave2);

        assertEquals("Description about dumbSlave1.", nodeList.getCommon(DESCRIPTION));
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getMode()}.
     * The list contains two DumbSlaves with the same Mode and a couple of other
     * Slave type with a different modes, which should not disturb.
     */
    @Test
    public void testGetModeSame() {
        PretendSlave pretendSlave1 = mock(PretendSlave.class);
        PretendSlave pretendSlave2 = mock(PretendSlave.class);

        when(pretendSlave1.getMode()).thenReturn(NORMAL);
        when(pretendSlave2.getMode()).thenReturn(EXCLUSIVE);
        when(dumbSlave1.getMode()).thenReturn(EXCLUSIVE);
        when(dumbSlave2.getMode()).thenReturn(EXCLUSIVE);

        nodeList.add(dumbSlave1);
        nodeList.add(dumbSlave2);
        nodeList.add(pretendSlave1);
        nodeList.add(pretendSlave2);

        assertEquals(EXCLUSIVE, nodeList.getMode());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getMode()}.
     * The list contains two slaves with different modes.
     */
    @Test
    public void testGetModeDifferent() {
        when(dumbSlave1.getMode()).thenReturn(EXCLUSIVE);
        when(dumbSlave2.getMode()).thenReturn(NORMAL);

        nodeList.add(dumbSlave1);
        nodeList.add(dumbSlave2);

        assertNull(nodeList.getMode());
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeList#getMode()}.
     * The list contains no DumbSlaves.
     */
    @Test (expected = Failure.class)
    public void testGetModeEmptyList() {
        PretendSlave pretendSlave = mock(PretendSlave.class);
        nodeList.add(pretendSlave);

        //Should throw Failure
        nodeList.getMode();
    }
}
