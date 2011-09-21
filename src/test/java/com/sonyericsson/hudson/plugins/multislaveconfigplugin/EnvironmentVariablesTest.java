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

import hudson.slaves.DumbSlave;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.EnvironmentVariables.*;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests {@link EnvironmentVariables} using JUnit Tests.
 * @author Fredrik Persson &lt;fredrik4.persson@sonyericsson.com&gt;
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(DumbSlave.class)
public class EnvironmentVariablesTest {

    private static final String SLAVE_NAME = "slave-name";
    DumbSlave slave;

    /**
     * Sets up the tests, making sure the slave has a mocked name.
     */
    @Before
    public void setupTest() {
        slave = mock(DumbSlave.class);
        when(slave.getNodeName()).thenReturn(SLAVE_NAME);
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave, String)}.
     * Converting to environment variables in a string
     */
    @Test
    public void testToVariables() {
        String s1 = "test" + SLAVE_NAME + " test";
        assertEquals("test$NAME test", toVariables(slave, s1));
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave, String)}.
     * Converting to environment variables with null as argument string.
     */
    @Test
    public void testToVariablesNull1() {
        assertNull(toVariables(slave, null));
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(String, String)}.
     * Converting to environment variables with null as argument string.
     */
    @Test
    public void testToVariablesNull2() {
        assertNull(toVariables(SLAVE_NAME, null));
    }

    /**
     * Tests {@link EnvironmentVariables#fromVariables(hudson.slaves.DumbSlave, String)}.
     * Converting from environment variables in a string.
     */
    @Test
    public void testFromVariables() {
        String s1 = "test$NAME test";
        assertEquals("test" + SLAVE_NAME + " test", fromVariables(slave, s1));
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(hudson.slaves.DumbSlave, String)}.
     * Converting to environment variables with null as argument string.
     */
    @Test
    public void testFromVariablesNull1() {
        assertNull(fromVariables(slave, null));
    }

    /**
     * Tests {@link EnvironmentVariables#toVariables(String, String)}.
     * Converting to environment variables with null as argument string.
     */
    @Test
    public void testFromVariablesNull2() {
        assertNull(fromVariables(SLAVE_NAME, null));
    }

    /**
     * Tests {@link EnvironmentVariables#containsEnvironmentVariables(String)}.
     * Checks a string that contains an environment variable
     */
    @Test
    public void testContainsEnvironmentVariables() {
        String variableString = "$NAME test";
        assertTrue(containsEnvironmentVariables(variableString));
    }

    /**
     * Tests {@link EnvironmentVariables#containsEnvironmentVariables(String)}.
     * Checks a string that does not contain an environment variable
     */
    @Test
    public void testContainsEnvironmentVariablesNot() {
        String variableString = "test test";
        assertFalse(containsEnvironmentVariables(variableString));
    }

}
