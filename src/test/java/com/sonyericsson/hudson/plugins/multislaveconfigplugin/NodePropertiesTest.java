/*
 *  The MIT License
 *
 *  Copyright (c) 2014 Sony Mobile Communications Inc. All rights reserved.
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

import hudson.EnvVars;
import hudson.model.Node;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests that common {@link NodeProperty}s for {@link NodeList}s
 * are found as expected.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
public class NodePropertiesTest {

    /**
     * Jenkins rule instance.
     */
    // CS IGNORE VisibilityModifier FOR NEXT 3 LINES. REASON: Mocks tests.
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private DumbSlave slave0;
    private DumbSlave slave1;

    /**
     * Creates two slaves before every test is being run.
     * @throws Exception if something goes wrong
     */
    @Before
    public void setUp() throws Exception {
        slave0 = jenkinsRule.createSlave();
        slave1 = jenkinsRule.createSlave();
    }

    /**
     * Tests that the list of common {@link NodeProperty}s is empty
     * when no slaves have any node properties.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetCommonPropertiesBothEmpty() throws Exception {
        NodeList nodeList = new NodeList();
        nodeList.add(slave0);
        nodeList.add(slave1);

        assertTrue("New slaves should have no common node properties",
                nodeList.getNodeProperties().isEmpty());
    }

    /**
     * Tests that the list of common {@link NodeProperty}s is empty
     * when one of the slaves have node properties but the other does not.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetCommonPropertiesOneEmpty() throws Exception {
        EnvironmentVariablesNodeProperty.Entry meaningfulEntry =
                new EnvironmentVariablesNodeProperty.Entry("MEANING_OF_LIFE", "42");
        slave0.getNodeProperties().add(new EnvironmentVariablesNodeProperty(meaningfulEntry));

        NodeList nodeList = new NodeList();
        nodeList.add(slave0);
        nodeList.add(slave1);

        assertTrue("There should be no common node properties",
                nodeList.getNodeProperties().isEmpty());
    }

    /**
     * Tests that the list of common {@link NodeProperty}s is empty
     * when both slaves have different node properties.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetCommonPropertiesDifferent() throws Exception {
        EnvironmentVariablesNodeProperty.Entry meaningfulEntry =
                new EnvironmentVariablesNodeProperty.Entry("MEANING_OF_LIFE", "42");
        slave0.getNodeProperties().add(new EnvironmentVariablesNodeProperty(meaningfulEntry));

        EnvironmentVariablesNodeProperty.Entry restrictingEntry =
                new EnvironmentVariablesNodeProperty.Entry("MAX_LIMIT", "SKY");
        slave1.getNodeProperties().add(new EnvironmentVariablesNodeProperty(restrictingEntry));

        NodeList nodeList = new NodeList();
        nodeList.add(slave0);
        nodeList.add(slave1);

        assertTrue("There should be no common node properties",
                nodeList.getNodeProperties().isEmpty());
    }

    /**
     * Tests that the list of common {@link NodeProperty}s contains
     * exactly one value when the slaves have one property in common.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetCommonPropertiesSame() throws Exception {
        final String commonKey = "4LL y0Ur B45E";
        final String commonValue = "4R3_B3loN9_7O_u5";

        EnvironmentVariablesNodeProperty.Entry commonEntry =
                new EnvironmentVariablesNodeProperty.Entry(commonKey, commonValue);
        EnvironmentVariablesNodeProperty.Entry nonCommonEntry =
                new EnvironmentVariablesNodeProperty.Entry("N0N_C0MM0N", "ENTRY");

        slave0.getNodeProperties().add(new EnvironmentVariablesNodeProperty(commonEntry));
        slave0.getNodeProperties().add(new EnvironmentVariablesNodeProperty(nonCommonEntry));

        slave1.getNodeProperties().add(new EnvironmentVariablesNodeProperty(commonEntry));

        NodeList nodeList = new NodeList();
        nodeList.add(slave0);
        nodeList.add(slave1);

        List<NodeProperty> nodeProperties = nodeList.getNodeProperties();
        assertEquals("There should one common node property",
                1, nodeProperties.size());

        EnvironmentVariablesNodeProperty firstProp = (EnvironmentVariablesNodeProperty)nodeProperties.get(0);
        EnvVars vars = firstProp.getEnvVars();
        assertEquals(1, vars.size());

        assertEquals("All your base should be belong to us", commonValue, vars.get(commonKey));
    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.Map)}.
     * Sets node properties for the selected slaves.
     */
    @Test
    public void testAddOrChangeSettingsNodeProperties() {
        NodeList nodeList = new NodeList();
        nodeList.add(slave0);
        nodeList.add(slave1);
        Map settings = new HashMap();

        NodeProperty<?> property = new EnvironmentVariablesNodeProperty();
        List<NodeProperty<?>> list = new ArrayList<NodeProperty<?>>();
        list.add(property);
        settings.put("addOrChangeProperties", list);
        nodeList.changeSettings(settings);

        List<Node> registeredNodes = jenkinsRule.getInstance().getNodes();

        assertEquals(list, registeredNodes.get(0).getNodeProperties());
        assertEquals(list, registeredNodes.get(1).getNodeProperties());
    }

    /**
     * Tests {@link NodeList#changeSettings(java.util.Map)}.
     * Removes node properties for the selected slaves.
     * @throws Exception if Settings can't be removed
     */
    @Test
    public void testRemoveSettingsNodeProperties() throws Exception {
        NodeList nodeList = new NodeList();
        nodeList.add(slave0);
        nodeList.add(slave1);
        Map settings = new HashMap();

        NodeProperty<?> property = new EnvironmentVariablesNodeProperty();
        List<NodeProperty<?>> list = new ArrayList<NodeProperty<?>>();
        list.add(property);
        settings.put("addOrChangeProperties", list);

        nodeList.changeSettings(settings);
        settings.clear();

        String className = EnvironmentVariablesNodeProperty.class.getName();
        List<String> removeList = new ArrayList<String>();
        removeList.add(className);

        settings.put("removeProperties", removeList);

        nodeList.changeSettings(settings);

        List<Node> registeredNodes = jenkinsRule.getInstance().getNodes();

        assertTrue(registeredNodes.get(0).getNodeProperties().toList().isEmpty());
        assertTrue(registeredNodes.get(1).getNodeProperties().toList().isEmpty());
    }

}
