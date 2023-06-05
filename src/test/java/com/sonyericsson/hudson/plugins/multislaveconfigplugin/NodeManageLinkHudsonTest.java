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

import org.htmlunit.ElementNotFoundException;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.html.HtmlPage;
import hudson.os.windows.ManagedWindowsServiceLauncher;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * Tests the {@link NodeManageLink} using HudsonTestCase.
 * @author Nicklas Nilsson &lt;nicklas3.nilsson@sonyericsson.com&gt;
 */
public class NodeManageLinkHudsonTest extends HudsonTestCase {


    /**
     * Tests{@link NodeManageLink#isManagedWindowsServiceLauncher(hudson.slaves.ComputerLauncher)}.
     * Testing that a ManagedWindowsServiceLauncher makes this method return true.
     */
    @LocalData
    public void testIsManagedWindowsServiceLauncher() {
        assertTrue(NodeManageLink.getInstance().isManagedWindowsServiceLauncher(
                new ManagedWindowsServiceLauncher("", "")));
    }

    /**
     * Checks that anonymous users cant reach the plugins URL.
     * @throws Exception if so.
     */
    @LocalData
    public void testAnonymousPermission() throws Exception {
        final int httpForbidden = 403;
        WebClient wc = createWebClient();
        try {
            HtmlPage page = wc.goTo(NodeManageLink.URL);
        } catch (FailingHttpStatusCodeException e) {
            assertEquals(httpForbidden, e.getStatusCode());
            return;
        }
        fail("Anonymous users should not be able to reach the plugins URL");
    }

    /**
     * Checks that admins can reach the plugins URL.
     * @throws Exception if so.
     */
    @LocalData
    public void testAdminPermission() throws Exception {
        WebClient wc = createWebClient().login("admin", "admin");
        try {
            HtmlPage page = wc.goTo(NodeManageLink.URL);
            //Trying to get element from plugin page.
            assertNotNull(page.getAnchorByText("Configure slaves"));
        } catch (ElementNotFoundException e) {
            fail("Users that are admins should be able to reach the plugins URL.");
            return;
        }

    }

    /**
     * Checks that regular users cant reach to plugins url.
     * It should be hidden from an Anonymous user as configured in the test-configuration.
     * @throws Exception if so.
     */
    @LocalData
    public void testRegularUserPermission() throws Exception {
        final int httpForbidden = 403;
        WebClient wc = createWebClient().login("summerworkers", "summerworkers");
        try {
            HtmlPage page = wc.goTo(NodeManageLink.URL);
        } catch (FailingHttpStatusCodeException e) {
            assertEquals(httpForbidden, e.getStatusCode());
            return;
        }
        fail("Regular users should not be able so se the plugins URL");
    }

    /**
     * Tests {@link com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink#getInstance()}.
     * Checks that this method returns an instance of ManagementLink.
     */
    public void testGetInstance() {
        NodeManageLink nodeManageLink = NodeManageLink.getInstance();
        assertNotNull(nodeManageLink);
    }
}
