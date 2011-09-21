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

import hudson.model.Hudson;
import hudson.model.Node;
import hudson.slaves.DumbSlave;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.PretendSlave;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Tests the {@link NodeManageLink} using JUnit Tests.
 * @author Nicklas Nilsson &lt;nicklas3.nilsson@sonyericsson.com&gt;
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DumbSlave.class, Hudson.class, PretendSlave.class })
public class SearchSlavesTest {

    private DumbSlave dumbSlave;
    private Hudson hudsonMock;
    private JSONObject searchParameters;
    private JSONObject noHit;

    /**
     * Adds a configured DumbSlave for Jenkins before starting the tests.
     * Also creates a dummy JSON object that has potential search strings.
     */
    @Before
    public void setup() {
        //test mock slave
        dumbSlave = PowerMockito.mock(DumbSlave.class);
        when(dumbSlave.getNodeName()).thenReturn("slave");
        when(dumbSlave.getNodeDescription()).thenReturn("This is the description");
        when(dumbSlave.getRemoteFS()).thenReturn("/jenkins/root");
        when(dumbSlave.getNumExecutors()).thenReturn(1);
        when(dumbSlave.getLabelString()).thenReturn("BUILDNODE");

        //test mock hudson instance
        hudsonMock = mock(Hudson.class);
        mockStatic(Hudson.class);
        when(Hudson.getInstance()).thenReturn(hudsonMock);
        when(hudsonMock.getNodes()).thenReturn(Collections.<Node>singletonList(dumbSlave));

        // Creates a request that shall give a search hit.
        searchParameters = new JSONObject();
        searchParameters.put("labels", "");
        searchParameters.put("remoteFS", "");
        searchParameters.put("description", "");
        searchParameters.put("executors", "");
        searchParameters.put("name", "");
    }

    /**
     * Tests {@link SearchSlaves#getNodes(net.sf.json.JSONObject)}.
     * Searching for DumbSlaves by label.
     */
    @Test
    public void testGetNodesByLabel() {
        searchParameters.put("labels", "BUILDNODE");
        NodeList result = SearchSlaves.getNodes(searchParameters);
        assertTrue(result.contains(dumbSlave));
        assertEquals(1, result.size());
    }

    /**
     * Tests {@link SearchSlaves#getNodes(net.sf.json.JSONObject)}.
     * Searching for DumbSlaves by FSRoot.
     */
    @Test
    public void testGetNodesByFSRoot() {
        searchParameters.put("label", "");
        searchParameters.put("remoteFS", "/jenkins/root");
        NodeList result = SearchSlaves.getNodes(searchParameters);
        assertTrue(result.contains(dumbSlave));
        assertEquals(1, result.size());
    }

    /**
     * Tests {@link SearchSlaves#getNodes(net.sf.json.JSONObject)}.
     * Searching for DumbSlaves by description.
     */
    @Test
    public void testGetNodesByDescription() {
        searchParameters.put("remoteFS", "");
        searchParameters.put("description", "This is the description");
        NodeList result = SearchSlaves.getNodes(searchParameters);
        assertTrue(result.contains(dumbSlave));
        assertEquals(1, result.size());
    }

    /**
     * Tests {@link SearchSlaves#getNodes(net.sf.json.JSONObject)}.
     * Searching for DumbSlaves by number of executors.
     */
    @Test
    public void testGetNodesByNbrOfExecutors() {
        searchParameters.put("description", "");
        searchParameters.put("executors", "1");
        NodeList result = SearchSlaves.getNodes(searchParameters);
        assertTrue(result.contains(dumbSlave));
        assertEquals(1, result.size());
    }

    /**
     * Tests {@link SearchSlaves#getNodes(net.sf.json.JSONObject)}.
     * Searching for DumbSlaves by name.
     */
    @Test
    public void testGetNodesByName() {
        searchParameters.put("executors", "");
        searchParameters.put("name", "slave");
        NodeList result = SearchSlaves.getNodes(searchParameters);
        assertTrue(result.contains(dumbSlave));
        assertEquals(1, result.size());
    }

    /**
     * Tests {@link SearchSlaves#SearchSlaves()}.
     * Test that you cant create new instances of this SearchSlaves.class.
     */
    @Test (expected = UnsupportedOperationException.class)
    public void testCreateSearchNodes() {
        SearchSlaves sn = new SearchSlaves();
    }

    /**
     * Tests {@link SearchSlaves#getNodes(net.sf.json.JSONObject)}.
     * Searching for DumbSlaves by label.
     */
    @Test
    public void testGetNodesByLabelNoHit() {
        searchParameters.put("executors", "");
        searchParameters.put("labels", "no hit");
        NodeList result = SearchSlaves.getNodes(searchParameters);
        assertTrue(result.isEmpty());
    }

    /**
     * Tests {@link SearchSlaves#getNodes(net.sf.json.JSONObject)}.
     * Searching for DumbSlaves by FSRoot.
     */
    @Test
    public void testGetNodesByFSRootNoHit() {
        searchParameters.put("label", "");
        searchParameters.put("remoteFS", "no hit");
        NodeList result = SearchSlaves.getNodes(searchParameters);
        assertTrue(result.isEmpty());
    }

    /**
     * Tests {@link SearchSlaves#getNodes(net.sf.json.JSONObject)}.
     * Searching for DumbSlaves by description.
     */
    @Test
    public void testGetNodesByDescriptionNoHit() {
        searchParameters.put("remoteFS", "");
        searchParameters.put("description", "no hit");
        NodeList result = SearchSlaves.getNodes(searchParameters);
        assertTrue(result.isEmpty());
    }

    /**
     * Tests {@link SearchSlaves#getNodes(net.sf.json.JSONObject)}.
     * Searching for DumbSlaves by number of executors.
     */
    @Test
    public void testGetNodesByNbrOfExecutorsNoHit() {
        searchParameters.put("description", "");
        searchParameters.put("executors", "7");
        NodeList result = SearchSlaves.getNodes(searchParameters);
        assertTrue(result.isEmpty());
    }

    /**
     * Tests {@link SearchSlaves#getNodes(net.sf.json.JSONObject)}.
     * Checks that only DumbSlaves being selected.
     */
    @Test
    public void testGetNodesOnlyDumbSlaves() {
        PretendSlave pretendSlave = PowerMockito.mock(PretendSlave.class);
        NodeList mockList = new NodeList();
        mockList.add(pretendSlave);
        mockList.add(dumbSlave);
        when(hudsonMock.getNodes()).thenReturn(mockList);
        NodeList result = SearchSlaves.getNodes(searchParameters);
        assertTrue(result.contains(dumbSlave));
        assertEquals(1, result.size());
    }

    /**
     * Tests {@link SearchSlaves#hasSearchHit(hudson.slaves.DumbSlave, String, String)}.
     * Checks that hasSearchHit() really returns true when a search parameter
     * that does exist on the slave is being passed.
     */
    @Test
    public void testHasSearchHit() {
        Boolean result = SearchSlaves.hasSearchHit(dumbSlave, "string1", "string string1 string2 string3");
        assertTrue(result);
    }

    /**
     * Tests {@link SearchSlaves#hasSearchHit(hudson.slaves.DumbSlave, String, String)}.
     * Checks that hasSearchHit() really returns false when a search parameter that does not exist on the slave is
     * being passed.
     */
    @Test
    public void testHasSearchHitNoHit() {
        Boolean result = SearchSlaves.hasSearchHit(dumbSlave, "string4", "string string1 string2 string3");
        assertFalse(result);
    }

    /**
     * Tests {@link SearchSlaves#hasSearchHit(hudson.slaves.DumbSlave, String, String)}.
     * Checks that hasSearchHit() returns false when passing two parameters that exist on the slave and one that
     * doesn't.
     */
    @Test
    public void testHasSearchHitNoHit2() {
        Boolean result = SearchSlaves.hasSearchHit(dumbSlave, "string string4 string1",
                "string string1 string2 string3");
        assertFalse(result);
    }

    /**
     * Tests {@link SearchSlaves#hasSearchHit(hudson.slaves.DumbSlave, String, String)}.
     * Checks that hasSearchHit() returns true when searching on multiple labels in different order.
     */
    @Test
    public void testHasSearchHitDifferentOrder() {
        Boolean result = SearchSlaves.hasSearchHit(dumbSlave, "string3 string1", "string string1 string2 string3");
        assertTrue(result);
    }

    /**
     * Tests {@link SearchSlaves#hasSearchHit(hudson.slaves.DumbSlave, String, String)}.
     * Checks that hasSearchHit() returns true when searching on $NAME and the slave parameter has the slave name.
     */
    @Test
    public void testHasSearchHitEnvironmentVariablesTrue() {
        Boolean result = SearchSlaves.hasSearchHit(dumbSlave, "$NAME", "slave string1 string2 string3");
        assertTrue(result);
    }

    /**
     * Tests {@link SearchSlaves#hasSearchHit(hudson.slaves.DumbSlave, String, String)}.
     * Checks that hasSearchHit() returns false when searching on $NAME and the slave parameter don't have the slave
     * name.
     */
    @Test
    public void testHasSearchHitEnvironmentVariablesFalse() {
        Boolean result = SearchSlaves.hasSearchHit(dumbSlave, "$NAME", "string string1 string2 string3");
        assertFalse(result);
    }



    /**
     * Tests {@link SearchSlaves#hasSearchHit(hudson.slaves.DumbSlave, String, String)}.
     * With empty slave parameter.
     */
    @Test
    public void testHasSearchHitEmptySlaveParameter() {
        Boolean result = SearchSlaves.hasSearchHit(dumbSlave, "test4", "");
        assertFalse(result);
    }

    /**
     * Tests {@link SearchSlaves#hasSearchHit(hudson.slaves.DumbSlave, String, String)}.
     * With null search parameter.
     */
    @Test
    public void testHasSearchHitNullSearchParameter() {
        Boolean result = SearchSlaves.hasSearchHit(dumbSlave, null, "test test1 test2 test3");
        assertTrue(result);
    }

    /**
     * Tests {@link SearchSlaves#hasSearchHit(hudson.slaves.DumbSlave, String, String)}.
     * With empty search parameter.
     */
    @Test
    public void testHasSearchHitEmptySearchParameter() {
        Boolean result = SearchSlaves.hasSearchHit(dumbSlave, "", "test test1 test2 test3");
        assertTrue(result);
    }

    /**
     * Tests {@link SearchSlaves#makeSearchable(String)}.
     * Tests that this method is trimming and making the string to lower case.
     */
    @Test
    public void testMakeSearchable() {
        String result = SearchSlaves.makeSearchable(" search String");
        assertEquals("search string", result);
    }

    /**
     * Tests {@link SearchSlaves#makeSearchable(String)}.
     * Tests that this method returns an empty string if a null parameter is being passed.
     */
    @Test
    public void testMakeSearchableNullString() {
        String result = SearchSlaves.makeSearchable(null);
        assertEquals("", result);
    }
}
