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

import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequestSettings;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlOption;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;
import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.nodes.JobRestrictionProperty;
import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.restrictions.job.RegexNameRestriction;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.slaves.CommandLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.lang.RandomStringUtils;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.UIHudsonTest.Change.ADD_LABELS;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.UIHudsonTest.Change.AVAILABILITY;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.UIHudsonTest.Change.DESCRIPTION;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.UIHudsonTest.Change.LAUNCH_METHOD;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.UIHudsonTest.Change.NBR_OF_EXECUTORS;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.UIHudsonTest.Change.REMOTE_FS;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.UIHudsonTest.Change.REMOVE_LABELS;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.UIHudsonTest.Change.SET_LABELS;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.UIHudsonTest.Change.USAGE_MODE;
import static hudson.model.Node.Mode.EXCLUSIVE;
import static hudson.slaves.EnvironmentVariablesNodeProperty.Entry;

//CS IGNORE MagicNumber FOR NEXT 1000 LINES. REASON: Tests.

/**
 * Tests the UI of the plugin, mostly by using HTMLUnit.
 * @author Fredrik Persson &lt;fredrik4.persson@sonyericsson.com&gt;
 * @author Nicklas Nilsson &lt;nicklas3.nilsson@sonyericsson.com&gt;
 */
public class UIHudsonTest extends HudsonTestCase {

    static final String CONFIGURE = "Configure slaves";
    static final String DELETE = "Delete slaves";
    static final String ADD = "Add slaves";
    static final String MANAGE = "Manage slaves";

    WebClient webClient;
    HtmlPage currentPage;
    DumbSlave slave0;
    DumbSlave slave1;
    DumbSlave slave2;
    DumbSlave slave3;

    /**
     * Sets up the tests.
     * @throws Exception if so.
     */
    public void setUp() throws Exception {
        super.setUp();
        webClient = createWebClient();
        currentPage = webClient.goTo(NodeManageLink.getInstance().getUrlName());
        webClient.setThrowExceptionOnScriptError(false);
        webClient.setThrowExceptionOnFailingStatusCode(false);
        slave0 = createSlave();
        slave1 = createSlave();
        slave2 = new DumbSlave("slave2", "This is the description on dumbSlave1", "HOME/slave2", "2",
                null, "LABEL1 LABEL3", null, null, Collections.EMPTY_LIST);
        slave3 = new DumbSlave("slave3", "This is the description on dumbSlave2", "home/slave3", "4",
                null, "label1", null, null, Collections.EMPTY_LIST);
        hudson.addNode(slave2);
        hudson.addNode(slave3);
    }

    /**
     * Testing the help links on the various pages affected by multislave.
     * @throws Exception if so.
     */
    public void testAllHelpLinksFound() throws Exception {
        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        //Takes the web client to "manageoptions"-page after selecting slaves.
        searchForAndSelectAllSlaves();

        List<?> helpButtons = currentPage.selectNodes("//a[@class='help-button']");
        assertNotNull(helpButtons);
        assertFalse(helpButtons.isEmpty());

        // The click() results in an Ajax call. If it fails, the test will fail
        for (HtmlAnchor helpButton : (List<HtmlAnchor>)helpButtons) {
            helpButton.click();
        }
    }

    /**
     * Test setting properties for a number of nodes.
     * The tests mocks a form containing the desired changes.
     * This was done since HTMLUNIT's javascript support isn't good enough.
     * @throws Exception on failure.
     */
    public void testSetNodeProperties() throws Exception {
        setUpNodeProperties();

        List<Node> registeredNodes = hudson.getNodes();
        List<NodeProperty<?>> list = registeredNodes.get(0).getNodeProperties().toList();

        EnvironmentVariablesNodeProperty env = (EnvironmentVariablesNodeProperty)list.get(0);
        assertTrue(env.getEnvVars().containsKey("FOODPREF"));
        assertTrue(env.getEnvVars().containsValue("BURGERS"));
    }

    /**
     * Tests that the settings selector page is auto populated with common
     * node properties.
     * @throws Exception on failure.
     */
    public void testNodePropertiesAutoPopulation() throws Exception {
        setUpNodeProperties();

        currentPage = webClient.goTo(NodeManageLink.getInstance().getUrlName());

        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        searchForAndSelectAllSlaves();

        assertTrue("The settings page should contain BURGERS (common key)",
                currentPage.asText().contains("BURGERS"));
    }

    /**
     * Test removing properties for a number of nodes.
     * The tests mocks a form containing the desired changes.
     * This was done since HTMLUNIT's javascript support isn't good enough.
     * @throws Exception on failure.
     */
    public void testRemoveNodeProperties() throws Exception {
        //First make sure the nodes have some random properties:
        for (Node node : jenkins.getNodes()) {
            Entry entry = new Entry(
                    RandomStringUtils.random(5), RandomStringUtils.random(5));
            node.getNodeProperties().add(new EnvironmentVariablesNodeProperty(entry));
        }

        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        searchForAndSelectAllSlaves();

        // Instead of requesting the page directly we create a WebRequestSettings object
        WebRequestSettings requestSettings = new WebRequestSettings(
                webClient.createCrumbedUrl(NodeManageLink.URL + "/apply"), HttpMethod.POST);

        // Then we set the request parameters
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("json", "{\"removeProperties\": "
                + "{\"stapler-class\": \"hudson.slaves.EnvironmentVariablesNodeProperty$DescriptorImpl\",\"kind\": "
                + "\"hudson.slaves.EnvironmentVariablesNodeProperty$DescriptorImpl\"}}"));
        params.add(new NameValuePair("Submit", "Save"));

        requestSettings.setRequestParameters(params);
        webClient.getPage(requestSettings);

        for (Node node : jenkins.getNodes()) {
            assertTrue("Slave should not have any node properties", node.getNodeProperties().isEmpty());
        }
    }

    /**
     * This test tries to configure the job restrictions plugin with help from multislave.
     * @throws Exception on failure.
     */
    public void testConfigureJobRestrictionsNodeProperty() throws Exception {
        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        searchForAndSelectAllSlaves();

        // Instead of requesting the page directly we create a WebRequestSettings object
        WebRequestSettings requestSettings = new WebRequestSettings(
                webClient.createCrumbedUrl(NodeManageLink.URL + "/apply"), HttpMethod.POST);


        // Then we set the request parameters
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("json", "{\"addOrChangeProperties\": { \"\": \"5\", \"jobRestriction\": "
                + "{ \"stapler-class\": \"com.synopsys.arc.jenkinsci.plugins.jobrestrictions."
                + "restrictions.job.RegexNameRestriction\", \"regexExpression\": \"testtest\", "
                + "\"checkShortName\": false }, \"stapler-class\": \"com.synopsys.arc.jenkinsci."
                + "plugins.jobrestrictions.nodes.JobRestrictionProperty\", \"kind\": \"com.synopsys.arc.jenkinsci."
                + "plugins.jobrestrictions.nodes.JobRestrictionProperty\" }}"));
        params.add(new NameValuePair("Submit", "Save"));

        requestSettings.setRequestParameters(params);

        webClient.getPage(requestSettings);

        List<Node> registeredNodes = hudson.getNodes();
        List<NodeProperty<?>> list = ((Slave)registeredNodes.get(0)).getNodeProperties().toList();

        JobRestrictionProperty jobRestrictionProperty = (JobRestrictionProperty)list.get(0);
        RegexNameRestriction restriction = (RegexNameRestriction)jobRestrictionProperty.getJobRestriction();
        assertEquals(restriction.getRegexExpression(), "testtest");

    }

    /**
     * Test for taking several nodes offline by using the UI.
     * @throws Exception if so.
     */
    public void testTakeNodesOffline() throws Exception {
        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(MANAGE);

        //Takes the web client to "manageoptions"-page after selecting slaves.
        searchForAndSelectAllSlaves();

        HtmlElement offlineReason = currentPage.getElementById("offlineReason");
        HtmlElement takeOffline = currentPage.getElementById("takeOffline");

        offlineReason.setTextContent("Testing...");
        takeOffline.fireEvent("click");

        assertTrue(slave0.toComputer().isTemporarilyOffline());
        assertTrue(slave1.toComputer().isTemporarilyOffline());
    }

    /**
     * Test for taking several nodes online by using the UI.
     * Online in this context means not temporarily offline.
     * @throws Exception if so.
     */
    public void testBringNodesOnline() throws Exception {
        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(MANAGE);

        slave0.toComputer().setTemporarilyOffline(true);
        slave1.toComputer().setTemporarilyOffline(true);

        //Takes the web client to "manageoptions"-page after selecting slaves.
        searchForAndSelectAllSlaves();

        HtmlElement takeOnline = currentPage.getElementById("takeOnline");

        takeOnline.fireEvent("click");

        assertFalse(slave0.toComputer().isTemporarilyOffline());
        assertFalse(slave1.toComputer().isTemporarilyOffline());
    }

    /**
     * Test for connecting to several nodes from the UI.
     * @throws Exception if so.
     */
    public void testConnectToNodes() throws Exception {
        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(MANAGE);

        //Takes the web client to "manageoptions"-page after selecting slaves.
        searchForAndSelectAllSlaves();

        HtmlElement connect = currentPage.getElementById("connectSlaves");

        connect.fireEvent("click");

        assertNotNull(slave0.toComputer().getChannel());
        assertNotNull(slave1.toComputer().getChannel());
    }

    /**
     * Test for disconnecting several nodes from the UI.
     * @throws Exception if so.
     */
    public void testDisconnectFromNodes() throws Exception {
        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(MANAGE);

        //Takes the web client to "manageoptions"-page after selecting slaves.
        searchForAndSelectAllSlaves();

        HtmlElement disconnect = currentPage.getElementById("disconnectSlaves");

        disconnect.fireEvent("click");

        assertNull(slave0.toComputer().getChannel());
        assertNull(slave1.toComputer().getChannel());
    }

    /**
     * Looks up the argument link text on currentPage and clicks on the link.
     * @param linkText the link text to look for.
     * @throws IOException if so.
     */
    private void clickLinkOnCurrentPage(String linkText) throws IOException {
        currentPage = currentPage.getAnchorByText(linkText).click();
    }

    /**
     * Selecting all slaves and and submitting the form by using the UI.
     * This method requires currentPage to be the "slave filter"-page
     * @throws Exception if so.
     */
    private void searchForAndSelectAllSlaves() throws Exception {
        HtmlForm form = currentPage.getFormByName("viewerForm");
        currentPage = submit(form);
    }

    /**
     * Test for changing the description of several slaves by using the UI.
     * @throws Exception if so.
     */
    public void testConfigureDescriptionChain() throws Exception {
        final String changedDescription = "This description is more describable...";
        final int expectedListSize = hudson.getNodes().size();

        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        //Takes the web client to "settings selector"-page.
        searchForAndSelectAllSlaves();

        HtmlForm form = currentPage.getFormByName("settingsForm");
        form.getInputByName("_description").setChecked(true);
        form.getInputByName("description").setValue(changedDescription);
        //Takes the web client to "applied settings"-page.
        //submitForm(form);
        currentPage = submit(form);
        String pageAsText = currentPage.asText();

        //Checks that the page contains a confirmation with the changed description
        assertTrue(pageAsText.contains(changedDescription));

        //Checks that the page does not contain confirmation for other settings
        checkOnlyAppliedSetting(Change.DESCRIPTION);

        //Checks that the slaves still exist and that their setting has changed
        for (int i = 0; i < expectedListSize; i++) {
            assertEquals(changedDescription, hudson.getNodes().get(i).getNodeDescription());
        }
    }

    /**
     * Test for changing the # of executors for several slaves by using the UI.
     * @throws Exception if so.
     */
    public void testConfigureExecutorsChain() throws Exception {
        final int changedExecutors = 2;
        final int expectedListSize = hudson.getNodes().size();

        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        //Takes the web client to "settings selector"-page.
        searchForAndSelectAllSlaves();

        HtmlForm form = currentPage.getFormByName("settingsForm");
        form.getInputByName("_numExecutors").setChecked(true);
        form.getInputByName("numExecutors").setValue(String.valueOf(changedExecutors));
        //Takes the web client to "applied settings"-page.
        currentPage = submit(form);

        String pageAsText = currentPage.asText();

        //Checks that the page contains a confirmation with the changed # of executors
        assertTrue(pageAsText.contains(String.valueOf(changedExecutors)));

        //Checks that the page does not contain confirmation for other settings
        checkOnlyAppliedSetting(Change.NBR_OF_EXECUTORS);

        //Checks that the slaves still exist and that their setting has changed
        assertEquals(expectedListSize, hudson.getNodes().size());
        for (int i = 0; i < expectedListSize; i++) {
            assertEquals(changedExecutors, hudson.getNodes().get(i).getNumExecutors());
        }
    }

    /**
     * Test for changing the remote FS for several slaves by using the UI.
     * @throws Exception if so.
     */
    public void testConfigureRemoteFSChain() throws Exception {
        final String changedRemoteFS = "/newHome";
        final int expectedListSize = hudson.getNodes().size();

        //Takes the web client to  "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        //Takes the web client to  "settings selector"-page.
        searchForAndSelectAllSlaves();

        HtmlForm form = currentPage.getFormByName("settingsForm");
        form.getInputByName("_remoteFS").setChecked(true);
        form.getInputByName("remoteFS").setValue(changedRemoteFS);
        //Takes the web client to  "applied settings"-page.
        currentPage = submit(form);

        String pageAsText = currentPage.asText();

        //Checks that the page contains a confirmation with the changed remote fs
        assertTrue(pageAsText.contains(changedRemoteFS));

        //Checks that the page does not contain confirmation for other settings
        checkOnlyAppliedSetting(Change.REMOTE_FS);

        //Checks that the slaves still exist and that their setting has changed
        assertEquals(expectedListSize, hudson.getNodes().size());
        for (int i = 0; i < expectedListSize; i++) {
            assertEquals(changedRemoteFS, ((DumbSlave)hudson.getNodes().get(i)).getRemoteFS());
        }
    }

    /**
     * Test for setting new labels for two slaves by using the UI.
     * @throws Exception if so.
     */
    public void testConfigureSetLabelsChain() throws Exception {
        final String newLabels = "LABEL1 LABEL2";
        final int expectedListSize = hudson.getNodes().size();

        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        //Takes the web client to "settings selector"-page.
        searchForAndSelectAllSlaves();

        HtmlForm form = currentPage.getFormByName("settingsForm");
        form.getInputByName("_labelString").setChecked(true);
        form.getInputByName("labelString").setValue(newLabels);
        //Takes the web client to "applied settings"-page.
        currentPage = submit(form);

        String pageAsText = currentPage.asText();

        //Checks that the page contains a confirmation with the changed label string
        assertTrue(pageAsText.contains(newLabels));

        //Checks that the page does not contain confirmation for other settings
        checkOnlyAppliedSetting(Change.SET_LABELS);

        //Checks that the slaves still exist and that their setting has changed
        assertEquals(expectedListSize, hudson.getNodes().size());
        for (int i = 0; i < expectedListSize; i++) {
            assertEquals(newLabels, hudson.getNodes().get(i).getLabelString());
        }
    }

    /**
     * Test for adding new labels for slaves by using the UI.
     * @throws Exception if so.
     */
    public void testConfigureAddLabelsChain() throws Exception {
        final String labelsToAdd = "LABEL1 LABEL2";
        final int expectedListSize = hudson.getNodes().size();

        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        //Takes the web client to "settings selector"-page.
        searchForAndSelectAllSlaves();

        HtmlForm form = currentPage.getFormByName("settingsForm");
        form.getInputByName("_addLabelString").setChecked(true);
        form.getInputByName("addLabelString").setValue(labelsToAdd);
        //Takes the web client to "applied settings"-page.
        currentPage = submit(form);

        String pageAsText = currentPage.asText();

        //Checks that the page contains a confirmation with the changed label string
        assertTrue(pageAsText.contains(labelsToAdd));

        //Checks that the page does not contain confirmation for other settings
        checkOnlyAppliedSetting(Change.ADD_LABELS);

        //Checks that the slaves still exist and that their setting has changed
        assertEquals(expectedListSize, hudson.getNodes().size());
        assertEquals("LABEL1 LABEL2", hudson.getNodes().get(0).getLabelString());
        assertEquals("LABEL1 LABEL3 LABEL2", hudson.getNodes().get(2).getLabelString());
    }

    /**
     * Test for removing labels from slaves by using the UI.
     * @throws Exception if so.
     */
    public void testConfigureRemoveLabelsChain() throws Exception {
        final String labelsToRemove = "LABEL1 LABEL2";
        createSlave("LABEL1 LABEL2 LABEL3 LABEL4", null);
        final int expectedListSize = hudson.getNodes().size();

        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        //Takes the web client to "settings selector"-page.
        searchForAndSelectAllSlaves();

        HtmlForm form = currentPage.getFormByName("settingsForm");
        form.getInputByName("_removeLabelString").setChecked(true);
        form.getInputByName("removeLabelString").setValue(labelsToRemove);
        //Takes the web client to "applied settings"-page.
        currentPage = submit(form);

        String pageAsText = currentPage.asText();

        //Checks that the page contains a confirmation with the changed label string
        assertTrue(pageAsText.contains(labelsToRemove));

        //Checks that the page does not contain confirmation for other settings
        checkOnlyAppliedSetting(Change.REMOVE_LABELS);

        //Checks that the slaves still exist and that their setting has changed
        assertEquals(expectedListSize, hudson.getNodes().size());
        assertEquals("LABEL3 LABEL4", hudson.getNodes().get(4).getLabelString());
    }

    /**
     * Test for changing usage mode for slaves by using the UI.
     * @throws Exception if so.
     */
    public void testConfigureModeChain() throws Exception {
        final Node.Mode newMode = EXCLUSIVE;
        final int expectedListSize = hudson.getNodes().size();

        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        //Takes the web client to "settings selector"-page.
        searchForAndSelectAllSlaves();

        HtmlForm form = currentPage.getFormByName("settingsForm");
        form.getInputByName("_mode").setChecked(true);
        HtmlSelect modeSelect = (HtmlSelect)currentPage.getElementById("mode");
        HtmlOption newModeOption = modeSelect.getOptionByValue(String.valueOf(newMode));
        modeSelect.setSelectedAttribute(newModeOption, true);
        //Takes the web client to "applied settings"-page.
        currentPage = submit(form);

        String pageAsText = currentPage.asText();

        //Checks that the page contains a confirmation with the changed mode
        assertTrue(pageAsText.contains(String.valueOf(newMode)));

        //Checks that the page does not contain confirmation for other settings
        checkOnlyAppliedSetting(Change.USAGE_MODE);

        //Checks that the slaves still exist and that their setting has changed
        assertEquals(expectedListSize, hudson.getNodes().size());
        assertEquals(newMode, hudson.getNodes().get(0).getMode());
        assertEquals(newMode, hudson.getNodes().get(1).getMode());
    }

    /**
     * Test for changing computer launcher on slaves by using the UI.
     * @throws Exception if so.
     */
    public void testConfigureLauncherChain() throws Exception {
        final String launchCommand = "$NAME.run";
        final int expectedListSize = hudson.getNodes().size();

        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        //Takes the web client to "settings selector"-page.
        searchForAndSelectAllSlaves();

        HtmlForm form = currentPage.getFormByName("settingsForm");
        form.getInputByName("_launcher").setChecked(true);
        HtmlSelect launcherSelect = (HtmlSelect)currentPage.getElementById("launcherId");
        HtmlOption commandLauncherOption = launcherSelect.getOptionByValue("hudson.slaves.CommandLauncher");
        launcherSelect.setSelectedAttribute(commandLauncherOption, true);
        form.getInputByName("_.command").setValue(launchCommand);
        //Takes the web client to "applied settings"-page.
        currentPage = submit(form);

        String pageAsText = currentPage.asText();

        //Checks that the page contains a confirmation with the changed launcher
        assertTrue(pageAsText.contains(launchCommand));

        //Checks that the page does not contain confirmation for other settings
        checkOnlyAppliedSetting(Change.LAUNCH_METHOD);

        //Checks that the slaves still exist and that their setting has changed
        assertEquals(expectedListSize, hudson.getNodes().size());
        assertEquals("slave0.run", ((CommandLauncher)((DumbSlave)hudson.getNodes().get(0)).getLauncher()).getCommand());
        assertEquals("slave1.run", ((CommandLauncher)((DumbSlave)hudson.getNodes().get(1)).getLauncher()).getCommand());
    }

    /**
     * Test for changing retention strategy on slaves by using the UI.
     * @throws Exception if so.
     */
    public void testConfigureRetentionStrategyChain() throws Exception {
        final int inDemandDelay = 10;
        final int idleDelay = 20;
        final int expectedListSize = hudson.getNodes().size();

        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        //Takes the web client to "settings selector"-page.
        searchForAndSelectAllSlaves();

        HtmlForm form = currentPage.getFormByName("settingsForm");
        form.getInputByName("_retentionStrategy").setChecked(true);
        HtmlSelect strategySelect = (HtmlSelect)currentPage.getElementById("retentionStrategyId");
        HtmlOption onDemandOption = strategySelect.getOptionByValue("hudson.slaves.RetentionStrategy$Demand");
        strategySelect.setSelectedAttribute(onDemandOption, true);
        form.getInputByName("retentionStrategy.inDemandDelay").setValue(String.valueOf(inDemandDelay));
        form.getInputByName("retentionStrategy.idleDelay").setValue(String.valueOf(idleDelay));
        //Takes the web client to "applied settings"-page.
        currentPage = submit(form);

        String pageAsText = currentPage.asText();

        //Checks that the page contains a confirmation with the changed retention strategy settings
        assertTrue(pageAsText.contains(String.valueOf(inDemandDelay)));
        assertTrue(pageAsText.contains(String.valueOf(idleDelay)));

        //Checks that the page does not contain confirmation for other settings
        checkOnlyAppliedSetting(Change.AVAILABILITY);

        //Checks that the slaves still exist and that their setting has changed
        assertEquals(expectedListSize, hudson.getNodes().size());
        assertEquals(inDemandDelay, ((RetentionStrategy.Demand)((DumbSlave)hudson.getNodes().get(0)).
                getRetentionStrategy()).getInDemandDelay());
        assertEquals(idleDelay, ((RetentionStrategy.Demand)((DumbSlave)hudson.getNodes().get(0)).
                getRetentionStrategy()).getIdleDelay());
        assertEquals(inDemandDelay, ((RetentionStrategy.Demand)((DumbSlave)hudson.getNodes().get(1))
                .getRetentionStrategy()).getInDemandDelay());
        assertEquals(idleDelay, ((RetentionStrategy.Demand)((DumbSlave)hudson.getNodes().get(1))
                .getRetentionStrategy()).getIdleDelay());
    }

    /**
     * Test for changing all settings at the same time on slaves by using the UI.
     * @throws Exception if so.
     */
    public void testConfigureAllSettingsChain() throws Exception {
        final int expectedListSize = hudson.getNodes().size();
        final String changedDescription = "This description is more describable...";
        final int changedExecutors = 2;
        final String changedRemoteFS = "/newHome";
        final String newLabels = "LABEL1 LABEL2";
        final Node.Mode newMode = EXCLUSIVE;
        final String launchCommand = "$NAME.run";
        final int inDemandDelay = 10;
        final int idleDelay = 20;

        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        //Takes the web client to "settings selector"-page.
        searchForAndSelectAllSlaves();

        HtmlForm form = currentPage.getFormByName("settingsForm");
        form.getInputByName("_description").setChecked(true);
        form.getInputByName("_numExecutors").setChecked(true);
        form.getInputByName("_remoteFS").setChecked(true);
        form.getInputByName("_labelString").setChecked(true);
        form.getInputByName("_mode").setChecked(true);
        form.getInputByName("_launcher").setChecked(true);
        form.getInputByName("_retentionStrategy").setChecked(true);

        form.getInputByName("description").setValue(changedDescription);
        form.getInputByName("numExecutors").setValue(String.valueOf(changedExecutors));
        form.getInputByName("remoteFS").setValue(changedRemoteFS);
        form.getInputByName("labelString").setValue(newLabels);

        HtmlSelect modeSelect = (HtmlSelect)currentPage.getElementById("mode");
        HtmlOption exclusiveOption = modeSelect.getOptionByValue(String.valueOf(newMode));
        modeSelect.setSelectedAttribute(exclusiveOption, true);

        HtmlSelect launcherSelect = (HtmlSelect)currentPage.getElementById("launcherId");
        HtmlOption commandLauncherOption = launcherSelect.getOptionByValue("hudson.slaves.CommandLauncher");
        launcherSelect.setSelectedAttribute(commandLauncherOption, true);
        form.getInputByName("_.command").setValue(launchCommand);

        HtmlSelect strategySelect = (HtmlSelect)currentPage.getElementById("retentionStrategyId");
        HtmlOption onDemandOption = strategySelect.getOptionByValue("hudson.slaves.RetentionStrategy$Demand");
        strategySelect.setSelectedAttribute(onDemandOption, true);
        form.getInputByName("retentionStrategy.inDemandDelay").setValue(String.valueOf(inDemandDelay));
        form.getInputByName("retentionStrategy.idleDelay").setValue(String.valueOf(idleDelay));
        //Takes the web client to "applied settings"-page.
        currentPage = submit(form);

        String pageAsText = currentPage.asText();

        //Checks that the page contains a confirmation with the changed settings
        assertTrue(pageAsText.contains(changedDescription));
        assertTrue(pageAsText.contains(String.valueOf(changedExecutors)));
        assertTrue(pageAsText.contains(changedRemoteFS));
        assertTrue(pageAsText.contains(newLabels));
        assertTrue(pageAsText.contains(String.valueOf(newMode)));
        assertTrue(pageAsText.contains(String.valueOf(inDemandDelay)));
        assertTrue(pageAsText.contains(String.valueOf(idleDelay)));

        //Checks that the slaves still exist and that their settings have changed
        assertEquals(expectedListSize, hudson.getNodes().size());
        assertEquals(changedDescription, hudson.getNodes().get(0).getNodeDescription());
        assertEquals(changedExecutors, hudson.getNodes().get(0).getNumExecutors());
        assertEquals(changedRemoteFS, ((DumbSlave)hudson.getNodes().get(0)).getRemoteFS());
        assertEquals(newLabels, hudson.getNodes().get(0).getLabelString());
        assertEquals(newMode, hudson.getNodes().get(0).getMode());
        assertEquals("slave0.run", ((CommandLauncher)((DumbSlave)hudson.getNodes().get(0)).getLauncher()).getCommand());
        assertEquals(inDemandDelay, ((RetentionStrategy.Demand)((DumbSlave)hudson.getNodes().get(0)).
                getRetentionStrategy()).getInDemandDelay());
        assertEquals(idleDelay, ((RetentionStrategy.Demand)((DumbSlave)hudson.getNodes().get(0)).
                getRetentionStrategy()).getIdleDelay());
    }


    /**
     * Test for deleting all slaves by using the UI.
     * @throws Exception if so.
     */
    public void testDeleteAllChainSelectYes() throws Exception {
        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(DELETE);

        //Takes the web client to "delete confirmation"-page.
        searchForAndSelectAllSlaves();

        //Are you sure you want to delete these slaves? pressing yes.
        currentPage.getElementByName("deleteSlavesInput").click();

        //checks that the slaves is successfully deleted.
        assertTrue(hudson.getNodes().isEmpty());
    }

    /**
     * Tests that no slaves are being deleted if you press no on the delete confirmation page by using the UI.
     * @throws Exception if so.
     */
    public void testDeleteAllChainSelectNo() throws Exception {
        final int slaveCountsBefore = hudson.getNodes().size();

        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(DELETE);

        //Takes the web client to "delete confirmation"-page.
        searchForAndSelectAllSlaves();

        //Are you sure you want to delete these slaves? pressing no.
        currentPage.getElementByName("homeRedirectInput").click();

        //Checks that nothing is deleted.
        assertEquals(slaveCountsBefore, hudson.getNodes().size());
    }

    /**
     * Tests that the user can search by all search fields and that right slave is returned.
     * @throws Exception if so.
     */
    public void testSearchByAll() throws Exception {
        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(DELETE);

        NodeManageLink link = NodeManageLink.getInstance();

        HtmlForm form = currentPage.getFormByName("viewerForm");

        form.getInputByName("name").setValue("slave2");
        form.getInputByName("description").setValue("This is the description on dumbSlave1");
        form.getInputByName("remoteFS").setValue("HOME/slave2");
        form.getInputByName("executors").setValue("2");
        form.getInputByName("label").setValue("LABEL1");

        //Search.
        currentPage = submit(form);

        String currentSessionId = link.userMode.keySet().iterator().next();

        assertEquals(1, link.getNodeList(currentSessionId).size());
        assertEquals("slave2", link.getNodeList(currentSessionId).get(0).getNodeName());
    }

    /**
     * Tests that the user can search by label and that right slaves are listed.
     * @throws Exception if so.
     */
    public void testSearchByName() throws Exception {
        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(DELETE);

        NodeManageLink link = NodeManageLink.getInstance();

        HtmlForm form = currentPage.getFormByName("viewerForm");

        form.getInputByName("name").setValue("slave2");
        //Search.
        currentPage = submit(form);

        String currentSessionId = link.userMode.keySet().iterator().next();

        assertEquals(1, link.getNodeList(currentSessionId).size());
        assertEquals("slave2", link.getNodeList(currentSessionId).get(0).getNodeName());
    }

    /**
     * Tests that the user can search by label and that right slaves are listed.
     * @throws Exception if so.
     */
    public void testSearchByLabel() throws Exception {
        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(DELETE);

        NodeManageLink link = NodeManageLink.getInstance();

        HtmlForm form = currentPage.getFormByName("viewerForm");

        form.getInputByName("label").setValue("label1");
        //Search.
        currentPage = submit(form);

        String currentSessionId = link.userMode.keySet().iterator().next();

        assertEquals(2, link.getNodeList(currentSessionId).size());
        assertEquals("LABEL1 LABEL3", link.getNodeList(currentSessionId).get(0).getLabelString());
        assertEquals("label1", link.getNodeList(currentSessionId).get(1).getLabelString());
    }

    /**
     * Tests that the user can search by description and that right slaves are listed.
     * @throws Exception if so.
     */
    public void testSearchByDescription() throws Exception {
        NodeManageLink link = NodeManageLink.getInstance();
        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(DELETE);

        HtmlForm form = currentPage.getFormByName("viewerForm");

        form.getInputByName("description").setValue("DUMBSLAVE1");
        //Search.
        currentPage = submit(form);

        String currentSessionId = link.userMode.keySet().iterator().next();

        assertEquals(1, link.getNodeList(currentSessionId).size());
        assertEquals("This is the description on dumbSlave1",
                link.getNodeList(currentSessionId).get(0).getNodeDescription());
    }

    /**
     * Tests that the user can search by FS root and that right slaves are listed.
     * @throws Exception if so.
     */
    public void testSearchByFSRoot() throws Exception {
        NodeManageLink link = NodeManageLink.getInstance();
        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(DELETE);

        HtmlForm form = currentPage.getFormByName("viewerForm");

        form.getInputByName("remoteFS").setValue("home/slave2");
        //Search.
        currentPage = submit(form);

        String currentSessionId = link.userMode.keySet().iterator().next();

        assertEquals(1, link.getNodeList(currentSessionId).size());
        assertEquals("HOME/slave2", ((DumbSlave)link.getNodeList(currentSessionId).get(0)).getRemoteFS());
    }

    /**
     * Tests that the user can search by FS root and that right slaves are listed.
     * @throws Exception if so.
     */
    public void testSearchByNbrOfExecutors() throws Exception {
        NodeManageLink link = NodeManageLink.getInstance();
        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(DELETE);

        HtmlForm form = currentPage.getFormByName("viewerForm");

        form.getInputByName("executors").setValue("2");
        //Search.
        currentPage = submit(form);

        String currentSessionId = link.userMode.keySet().iterator().next();

        assertEquals(1, link.getNodeList(currentSessionId).size());
        assertEquals(2, (link.getNodeList(currentSessionId).get(0)).getNumExecutors());
    }

    /**
     * Tests that the user can create new slaves in an interval.
     * @throws Exception if so.
     */
    public void testAddSlavesBySpan() throws Exception {
        final int slaveCountsAfter = hudson.getNodes().size() + 3;
        //Takes the web client to "add slaves"-page.
        clickLinkOnCurrentPage(ADD);

        //Fill in information about the new slaves.
        HtmlForm form = currentPage.getFormByName("createSlavesForm");
        form.getInputByName("mode").setValue("newSlave");
        form.getInputByName("slaveName").setValue("slave");
        form.getInputByName("first").setValue("06");
        form.getInputByName("last").setValue("08");
        //Takes the web client to "settings selector"-page.
        currentPage = submit(form);

        //Creating the slaves.
        form = currentPage.getFormByName("settingsForm");
        currentPage = submit(form);

        //Checks that the slaves are added and nothing more.
        assertNotNull(hudson.getNode("slave06"));
        assertNotNull(hudson.getNode("slave07"));
        assertNotNull(hudson.getNode("slave08"));
        assertEquals(slaveCountsAfter, hudson.getNodes().size());
    }

    /**
     * Tests that the user can create new slaves in an interval..
     * @throws Exception if so.
     */
    public void testAddSlavesUniqueNames() throws Exception {
        int slaveCountsAfter = hudson.getNodes().size() + 3;
        clickLinkOnCurrentPage(ADD);

        //Fill in information about the new slaves.
        HtmlForm form = currentPage.getFormByName("createSlavesForm");
        form.getInputByName("slaveNames").setValue("slave10 slave11 slave12");
        form.getInputByName("mode").setValue("newSlave");
        //Takes the web client to "settings selector"-page.
        currentPage = submit(form);

        //Creating the slaves.
        form = currentPage.getFormByName("settingsForm");
        currentPage = submit(form);

        //Checks that the slaves are added and  nothing more.
        assertNotNull(hudson.getNode("slave10"));
        assertNotNull(hudson.getNode("slave11"));
        assertNotNull(hudson.getNode("slave12"));
        assertEquals(slaveCountsAfter, hudson.getNodes().size());
    }

    /**
     * Tests that user can't create two or more slaves with same name.
     * @throws Exception if so.
     */
    public void testAddRedundantNames() throws Exception {
        int slaveCountsAfter = hudson.getNodes().size() + 1;
        clickLinkOnCurrentPage(ADD);

        //Fill in information about the new slaves.
        HtmlForm form = currentPage.getFormByName("createSlavesForm");
        form.getInputByName("slaveNames").setValue("slave10 slave10");
        form.getInputByName("mode").setValue("newSlave");
        //Takes the web client to "settings selector"-page.
        currentPage = submit(form);

        //Creating the slaves.
        form = currentPage.getFormByName("settingsForm");
        currentPage = submit(form);

        //Checks that the single slave is added and nothing more.
        assertNotNull(hudson.getNode("slave10"));
        assertEquals(slaveCountsAfter, hudson.getNodes().size());
    }

    /**
     * Tests that the user can copy the settings from one slave to some new ones.
     * @throws Exception if so.
     */
    public void testAddSlavesCopy() throws Exception {
        int expectedSlaveCount = hudson.getNodes().size() + 3;
        submit(prepareCopySlaves());

        //Checks that the slaves are added and that nothing more happens.
        assertEquals(2, hudson.getNode("slave13").getNumExecutors());
        assertEquals("This is the description on dumbSlave1", hudson.getNode("slave14").getNodeDescription());
        assertEquals("HOME/slave15", ((DumbSlave)hudson.getNode("slave15")).getRemoteFS());
        assertEquals(expectedSlaveCount, hudson.getNodes().size());
    }

    /**
     * Makes sure that {@link hudson.slaves.NodeProperty}s are copied to the new slave
     * when cloning slaves if they are not manually removed.
     * @throws Exception if something goes wrong
     */
    public void testCopiedSlavesKeepNodeProperties() throws Exception {
        Node originalSlave = hudson.getNode("slave2");
        Entry propertyEntry = new Entry("key", "value");
        EnvironmentVariablesNodeProperty property = new EnvironmentVariablesNodeProperty(propertyEntry);
        originalSlave.getNodeProperties().add(property);

        submit(prepareCopySlaves());

        assertEquals("value", ((EnvironmentVariablesNodeProperty)hudson.getNode("slave13")
                .getNodeProperties().get(0)).getEnvVars().get("key"));
    }

    /**
     * Makes sure that {@link hudson.slaves.NodeProperty}s are not copied to the new slave
     * when cloning slaves if they are manually removed.
     * @throws Exception if something goes wrong
     */
    public void testCopySlavesRemoveNodeProperties() throws Exception {
        Node originalSlave = hudson.getNode("slave2");
        Entry propertyEntry = new Entry("key", "value");
        EnvironmentVariablesNodeProperty property = new EnvironmentVariablesNodeProperty(propertyEntry);
        originalSlave.getNodeProperties().add(property);

        HtmlForm settings = prepareCopySlaves();

        //Clear all Node Property settings:
        settings.getElementsByAttribute("div", "name", "addOrChangeProperties").get(0).remove();
        submit(settings);

        assertTrue(hudson.getNode("slave13").getNodeProperties().isEmpty());
    }

    /**
     * Prepares copying of slaves.
     * @return settings form that can be submitted to execute the copying
     * @throws Exception if something goes wrong
     */
    private HtmlForm prepareCopySlaves() throws Exception {
        clickLinkOnCurrentPage(ADD);

        //Fill in information about the new slaves.
        HtmlForm form = currentPage.getFormByName("createSlavesForm");
        form.getInputByName("slaveNames").setValue("slave13 slave14 slave15");
        form.getInputByName("mode").setValue("copySlave");
        form.getInputByName("copyFrom").setValue("slave2");
        //Takes the web client to "settings selector"-page.
        currentPage = submit(form);

        return currentPage.getFormByName("settingsForm");
    }

    /**
     * Checks that only the setting that been changed is listed on the applied settings page.
     * @param change the setting to look for.
     */
    private void checkOnlyAppliedSetting(Enum change) {
        String pageAsText = currentPage.asText();

        if (change != DESCRIPTION) {
            assertFalse(pageAsText.contains("Description"));
        }
        if (change != NBR_OF_EXECUTORS) {
            assertFalse(pageAsText.contains("# of executors"));
        }
        if (change != REMOTE_FS) {
            assertFalse(pageAsText.contains("Remote FS root"));
        }
        if (change != SET_LABELS) {
            assertFalse(pageAsText.contains("Set labels"));
        }
        if (change != ADD_LABELS) {
            assertFalse(pageAsText.contains("Add labels"));
        }
        if (change != REMOVE_LABELS) {
            assertFalse(pageAsText.contains("Remove labels"));
        }
        if (change != USAGE_MODE) {
            assertFalse(pageAsText.contains("Usage mode"));
        }
        if (change != LAUNCH_METHOD) {
            assertFalse(pageAsText.contains("Launch method"));
        }
        if (change != AVAILABILITY) {
            assertFalse(pageAsText.contains("Availability"));
        }
    }

    /**
     * Sets up a common {@link NodeProperty} for all slaves.
     * @throws Exception if something goes wrong
     */
    private void setUpNodeProperties() throws Exception {
        //Takes the web client to "search for slaves"-page.
        clickLinkOnCurrentPage(CONFIGURE);

        searchForAndSelectAllSlaves();

        // Instead of requesting the page directly we create a WebRequestSettings object
        WebRequestSettings requestSettings = new WebRequestSettings(
                webClient.createCrumbedUrl(NodeManageLink.URL + "/apply"), HttpMethod.POST);

        // Then we set the request parameters
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("json", "{\"addOrChangeProperties\": {\"env\": { \"key\": \"FOODPREF\",\"value\": "
                + "\"BURGERS\"},\"stapler-class\": \"hudson.slaves.EnvironmentVariablesNodeProperty\",\"kind\": "
                + "\"hudson.slaves.EnvironmentVariablesNodeProperty\"}}"));
        params.add(new NameValuePair("Submit", "Save"));

        requestSettings.setRequestParameters(params);

        webClient.getPage(requestSettings);
    }

    /**
     * These match the settings that can be changed.
     */
    public enum Change {
        /**
         *The description setting that can be changed with this plugin.
         */
        DESCRIPTION,

        /**
         *The nbr of executors setting that can be changed with this plugin.
         */
        NBR_OF_EXECUTORS,

        /**
         *The remote FS setting that can be changed with this plugin.
         */
        REMOTE_FS,

        /**
         *The set labels setting that can be changed with this plugin.
         */
        SET_LABELS,

        /**
         *The add labels setting that can be changed with this plugin.
         */
        ADD_LABELS,

        /**
         *The remove labels setting that can be changed with this plugin.
         */
        REMOVE_LABELS,

        /**
         *The usage mode setting that can be changed with this plugin.
         */
        USAGE_MODE,

        /**
         *The launch method setting that can be changed with this plugin.
         */
        LAUNCH_METHOD,

        /**
         *The availability setting that can be changed with this plugin.
         */
        AVAILABILITY
    }
}


