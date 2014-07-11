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

import hudson.Extension;
import hudson.Functions;
import hudson.Util;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Computer;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Failure;
import hudson.model.Hudson;
import hudson.model.ManagementLink;
import hudson.model.Node;
import hudson.model.User;
import hudson.os.windows.ManagedWindowsServiceLauncher;
import hudson.security.Permission;
import hudson.slaves.CommandLauncher;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.NodePropertyDescriptor;
import hudson.slaves.OfflineCause;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SimpleScheduledRetentionStrategy;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink.UserMode.ADD;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink.UserMode.CONFIGURE;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink.UserMode.DELETE;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.NodeManageLink.UserMode.MANAGE;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

/**
 * Registers the plugin to be recognized by Jenkins as a management link and controls the main attributes of the plugin.
 * @author Nicklas Nilsson &lt;nicklas3.nilsson@sonyericsson.com&gt;
 * @author Fredrik Persson &lt;fredrik4.persson@sonyericsson.com&gt;
 */
@Extension
public class NodeManageLink extends ManagementLink implements Describable<NodeManageLink> {

    private static final Logger logger = Logger.getLogger(NodeManageLink.class.getName());
    /**
     * URL to the plugin.
     */
    protected static final String URL = "multi-slave-config-plugin";

    /**
     * Icon used by this plugin.
     */
    protected static final String ICON = "/plugin/" + URL + "/images/computers.png";

    /**
     * Hashmap with user and what userMode that currently is active.
     */
    protected HashMap<String, UserMode> userMode = new HashMap<String, UserMode>();
    private HashMap<String, NodeList> nodeListMap = new HashMap<String, NodeList>();
    private HashMap<String, HashMap> lastChangedSettings = new HashMap<String, HashMap>();
    private HashMap<String, Boolean> hadLabels = new HashMap<String, Boolean>();
    private static NodeManageLink instance;

    /**
     * Gets the descriptor.
     * @return descriptor.
     */
    public Descriptor<NodeManageLink> getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);
    }

    /**
     * Descriptor is only used for auto completion.
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<NodeManageLink> {
        @Override
        public String getDisplayName() {
            return null; //Not used.
        }

        /**
         * Returns a list of auto completion candidates.
         * Used in the "copy existing slave"-textbox at the slave creation page.
         * @param value to search for
         * @return candidates
         */
        public AutoCompletionCandidates doAutoCompleteNames(@QueryParameter String value) {
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            List<Node> masterNodeList = Hudson.getInstance().getNodes();
            for (Node node : masterNodeList) {
                if (node instanceof DumbSlave && node.getNodeName().toLowerCase().startsWith(value.toLowerCase())) {
                    candidates.add(node.getNodeName());
                }
            }
            return candidates;
        }
    }

    /**
     * Different user modes, depending on which wizard the user is running.
     */
    public enum UserMode {
        /**
         * Usermode when adding slaves.
         */
        ADD,
        /**
         * Usermode when configuring slaves.
         */
        CONFIGURE,
        /**
         * Usermode when deleting slaves.
         */
        DELETE,
        /**
         * Usermode when changing online and connected statuses of slaves.
         */
        MANAGE
    }

    /**
     * Gets the icon for this plugin.
     * @return icon url
     */
    public String getIconFileName() {
        return ICON;
    }

    /**
     * The URL of this plugin.
     * @return url
     */
    public String getUrlName() {
        return URL;
    }

    /**
     * Gets the name of this plugin.
     * @return plugin name
     */
    public String getDisplayName() {
        return Messages.Name();
    }

    /**
     * Gets the description of this plugin.
     * @return description
     */
    public String getDescription() {
        return Messages.Description();
    }

    /**
     * Returns required permission to use this plugin.
     * @return Hudson administer permission.
     */
    public Permission getRequiredPermission() {
        return Hudson.ADMINISTER;
    }

    /**
     * Returns the instance of NodeManageLink.
     * @return instance the NodeManageLink.
     */
    public static NodeManageLink getInstance() {
        List<ManagementLink> list = Hudson.getInstance().getManagementLinks();
        for (ManagementLink link : list) {
            if (link instanceof NodeManageLink) {
                instance = (NodeManageLink)link;
                break;
            }
        }
        return instance;
    }

    /**
     * Gets the active nodelist of a specific sessionid. Mostly used within the jelly pages.
     * @param sessionId the session id to get the list from
     * @return the active nodelist
     */
    public NodeList getNodeList(String sessionId) {
        return nodeListMap.get(sessionId);
    }

    /**
     * Used for letting the jelly scripts find out what changes have been applied.
     * @param sessionId which session id to get the settings from
     * @return the last changed settings
     */
    public HashMap getLastChangedSettings(String sessionId) {
        return lastChangedSettings.get(sessionId);
    }

    /**
     * Checks if argument is instance of ManagedWindowsServiceLauncher.
     * @param candidate RetentionStrategy to check
     * @return if it was an instance of ManagedWindowsServiceLauncher
     */
    public boolean isManagedWindowsServiceLauncher(ComputerLauncher candidate) {
        return candidate instanceof ManagedWindowsServiceLauncher;
    }

    /**
     * Checks if argument is instance of CommandLauncher.
     * @param candidate RetentionStrategy to check
     * @return if it was an instance of CommandLauncher
     */
    public boolean isCommandLauncher(ComputerLauncher candidate) {
        return candidate instanceof CommandLauncher;
    }

    /**
     * Checks if argument is instance of JNLPLauncher.
     * @param candidate RetentionStrategy to check
     * @return if it was an instance of JNLPLauncher
     */
    public boolean isJNLPLauncher(ComputerLauncher candidate) {
        return candidate instanceof JNLPLauncher;
    }

    /**
     * Checks if argument is instance of RetentionStrategy.Always.
     * @param candidate RetentionStrategy to check
     * @return if it was an instance of RetentionStrategy.Always
     */
    public boolean isRetentionStrategyAlways(RetentionStrategy candidate) {
        return candidate instanceof RetentionStrategy.Always;
    }

    /**
     * Checks if argument is instance of RetentionStrategy.Demand.
     * @param candidate RetentionStrategy to check
     * @return if it was an instance of RetentionStrategy.Demand
     */
    public boolean isRetentionStrategyDemand(RetentionStrategy candidate) {
        return candidate instanceof RetentionStrategy.Demand;
    }

    /**
     * Checks if argument is instance of SimpleScheduledRetentionStrategy.
     * @param candidate RetentionStrategy to check
     * @return if it was an instance of SimpleScheduledRetentionStrategy
     */
    public boolean isSimpleScheduledRetentionStrategy(RetentionStrategy candidate) {
        return candidate instanceof SimpleScheduledRetentionStrategy;
    }

    /**
     * Checks if current used mode was UserMode Configure.
     * @return true/false if last used mode was UserMode Configure
     */
    public boolean isConfigureMode() {
        String currentSessionId = Stapler.getCurrentRequest().getSession().getId();
        return userMode.get(currentSessionId) == CONFIGURE;
    }

    /**
     * Checks if current used mode was UserMode Delete.
     * @return true/false if last used mode was UserMode Delete
     */
    public boolean isDeleteMode() {
        String currentSessionId = Stapler.getCurrentRequest().getSession().getId();
        return userMode.get(currentSessionId) == DELETE;
    }

    /**
     * Checks if current used mode was UserMode Add.
     * @return true/false if last used mode was UserMode Add.
     */
    public boolean isAddMode() {
        String currentSessionId = Stapler.getCurrentRequest().getSession().getId();
        return userMode.get(currentSessionId) == ADD;
    }

    /**
     * Checks if current used mode was UserMode Manage.
     * @return true/false if last used mode was UserMode Manage.
     */
    public boolean isManageMode() {
        String currentSessionId = Stapler.getCurrentRequest().getSession().getId();
        return userMode.get(currentSessionId) == MANAGE;
    }


    /**
     * Gets all Jenkins registered nodes. Used for the jelly scripts
     * @return a list with all nodes
     */
    public List<Node> getAllNodes() {
        return Hudson.getInstance().getNodes();
    }

    /**
     * Generates a string of given length filled with stars characters.
     * @param length how many stars it should contain
     * @return the generated string
     */
    public String generateStars(int length) {
        StringBuffer stars = new StringBuffer();
        for (int i = 0; i < length; i++) {
            stars.append("*");
        }
        return stars.toString();
    }

    /**
     * Redirects to the configure-wizard, also setting usermode to configure.
     * @param req StaplerRequest
     * @param rsp StaplerResponse to redirect with
     * @throws IOException if redirection goes wrong
     */
    public void doConfigureRedirect(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Hudson app = Hudson.getInstance();
        // Throws exception on failure. This is handled at a higher level.
        app.checkPermission(getRequiredPermission());
        userMode.put(req.getSession().getId(), CONFIGURE);
        if (app.getNodes().isEmpty()) {
            throw new Failure(Messages.EmptyNodeList());
        }
        rsp.sendRedirect2("slavefilter");
    }

    /**
     * Redirects to the add slaves-wizard, also setting usermode to add.
     * @param req StaplerRequest
     * @param rsp StaplerResponse to redirect with
     * @throws IOException if redirection goes wrong
     */
    public void doAddRedirect(StaplerRequest req, StaplerResponse rsp) throws IOException {
        // Throws exception on failure. This is handled at a higher level.
        Hudson.getInstance().checkPermission(getRequiredPermission());
        userMode.put(req.getSession().getId(), ADD);
        rsp.sendRedirect2("createslaves");
    }

    /**
     * Redirects to Slaves management, also setting usermode to MANAGE.
     * @param req StaplerRequest
     * @param rsp StaplerResponse to redirect with
     * @throws IOException if redirection goes wrong
     */
    public void doManageRedirect(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Hudson app = Hudson.getInstance();
        // Throws exception on failure. This is handled at a higher level.
        app.checkPermission(getRequiredPermission());
        userMode.put(req.getSession().getId(), MANAGE);
        if (app.getNodes().isEmpty()) {
            throw new Failure(Messages.EmptyNodeList());
        }
        rsp.sendRedirect2("slavefilter");
    }



    //TODO: Add doLaunchRedirect that sets the usermode for the specific session and redirects to the slavefilter page

    /**
     * Redirects to the delete-wizard, also setting usermode to delete.
     * @param req StaplerRequest
     * @param rsp StaplerResponse to redirect with
     * @throws IOException if redirection goes wrong
     */
    public void doDeleteRedirect(StaplerRequest req, StaplerResponse rsp)
            throws IOException {
        Hudson app = Hudson.getInstance();
        // Throws exception on failure. This is handled at a higher level.
        app.checkPermission(getRequiredPermission());
        userMode.put(req.getSession().getId(), DELETE);
        if (app.getNodes().isEmpty()) {
            throw new Failure(Messages.EmptyNodeList());
        }
        rsp.sendRedirect2("slavefilter");
    }

    /**
     * Redirects to home.
     * @param req StaplerRequest
     * @param rsp StaplerResponse to redirect with
     * @throws IOException if redirection goes wrong
     */
    public void doHomeRedirect(StaplerRequest req, StaplerResponse rsp) throws IOException {
        rsp.sendRedirect2("");
    }

    /**
     * Searches for slaves.
     * Also saves the results as a nodelist bound to the sessionid.
     * @param sessionId the current session ID to to place the nodeList with.
     * @param searchParameters JSONObject with information on what to search for.
     * @return JSONArray with matching slave-representations from the search.
     */
    @JavaScriptMethod
    public synchronized JSONArray doSearch(String sessionId, JSONObject searchParameters) {
        // Throws exception on failure. This is handled at a higher level.
        Hudson.getInstance().checkPermission(getRequiredPermission());
        NodeList nodeList = SearchSlaves.getNodes(searchParameters);
        nodeList.sortByName();
        nodeListMap.put(sessionId, nodeList);
        return nodeList.toJSONArray();
    }

    /**
     * Adds all slaves that are checked on the slavefilter page to a NodeList.
     * @param rsp StaplerRequest
     * @param req StaplerRequest
     * @throws IOException if redirection goes wrong
     * @throws Failure
     */
    public synchronized void doSelectSlaves(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Hudson app = Hudson.getInstance();
        // Throws exception on failure. This is handled at a higher level.
        app.checkPermission(getRequiredPermission());
        NodeList newList = new NodeList();
        JSONObject json;
        try {
            json = req.getSubmittedForm();
        } catch (ServletException e) {
            throw new Failure(Messages.InvalidSubmittedForm());
        }

        if (json.get("selectedSlaves") == null) {
            throw new Failure(Messages.NoSelectedSlaves());
        }
        //if more than one slave is selected
        try {
            JSONArray selectedSlaves = json.getJSONArray("selectedSlaves");
            for (Object selectedSlave : selectedSlaves) {
                String name = selectedSlave.toString();
                Node node = app.getNode(name);
                if (node != null) {
                    newList.add(node);
                }
            }
            //else its just one slave selected
        } catch (JSONException e) {
            Node slave = app.getNode(json.getString("selectedSlaves"));
            if (slave != null) {
                newList.add(slave);
            }
        }
        String currentSessionId = req.getSession().getId();
        nodeListMap.put(currentSessionId, newList);
        if (userMode.get(currentSessionId) == CONFIGURE) {
            rsp.sendRedirect2("settingsselector");
        } else if (userMode.get(currentSessionId) == DELETE) {
            rsp.sendRedirect2("deleteconfirmation");
        } else if (userMode.get(currentSessionId) == MANAGE) {
            rsp.sendRedirect2("manageoptions");
        } else {
            //Redirect to home, so that a user mode can be set
            rsp.sendRedirect2("");
        }
    }

    /**
     * Applies the settings on the current slaves.
     * @param rsp StaplerRequest.
     * @param req StaplerRequest.
     * @throws IOException if redirection goes wrong.
     * @throws ServletException if something is wrong with the submitted form.
     */
    public synchronized void doApply(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        // Throws exception on failure. This is handled at a higher level.
        Hudson.getInstance().checkPermission(getRequiredPermission());
        String currentSessionId = req.getSession().getId();
        NodeList nodeList = getNodeList(currentSessionId);

        HashMap settings;
        try {
            settings = NodeList.interpretJSON(req.getSubmittedForm());
        } catch (ServletException e) {
            logger.log(Level.WARNING, "Invalid submitted form after editing settings on slaves");
            throw new Failure(Messages.InvalidSubmittedForm());
        }
        UserMode currentUsermode = userMode.get(currentSessionId);

        if (currentUsermode == null) {
            //Redirect to home, so that a user mode can be set
            rsp.sendRedirect2("");
        }

        if (settings.isEmpty() && currentUsermode == CONFIGURE) {
            throw new Failure(Messages.NoSelectedSettings());
        }
        if ((nodeList.slavesStillExist() && currentUsermode == CONFIGURE)
                || currentUsermode == ADD) {
            //Checks if the labels to remove existed before applying the change,
            //so that the confirmation page can show if the remove was successful:
            hadLabels.put(req.getSession().getId(), nodeList.hasLabels((String)settings.get("removeLabelString")));
            nodeList = nodeList.changeSettings(settings);
            nodeListMap.put(currentSessionId, nodeList);
            lastChangedSettings.put(currentSessionId, settings);
            if (currentUsermode == CONFIGURE) {
                //TODO: Structure up these logging messages
                logger.log(Level.INFO, "User configured the following slaves: " + nodeList.toString() + "\n"
                        + "with the following submitted form containing new settings: " + req.getSubmittedForm());
                rsp.sendRedirect2("applied");
            } else if (currentUsermode == ADD) {
                logger.log(Level.INFO, "User added the following slaves: " + nodeList.toString() + "\n"
                        + "with the following submitted form containing new settings: " + req.getSubmittedForm());
                rsp.sendRedirect2("added");
            }
        } else {
            throw new Failure(Messages.SlaveDeleted());
        }
    }

    /**
     * Checks if the used nodelist (by searching for session id) contained the labels to remove before removing them.
     * Used for the confirmation page
     * @param sessionId the session id to get the state from
     * @return true/false
     */
    public boolean hadLabels(String sessionId) {
        return hadLabels.get(sessionId);
    }

    /**
     * Deletes the slaves in this list.
     * @param req StaplerRequest
     * @param rsp StaplerResponse
     * @throws IOException if redirection goes wrong
     */
    public synchronized void doDeleteSlaves(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Hudson app = Hudson.getInstance();
        // Throws exception on failure. This is handled at a higher level.
        app.checkPermission(getRequiredPermission());
        String currentSessionId = req.getSession().getId();
        NodeList nodeList = getNodeList(currentSessionId);

        StringBuffer failedSlavesBuffer = new StringBuffer();
        NodeList failedSlavesList = new NodeList();
        for (Node node : nodeList) {
            try {
                app.removeNode(node);
            } catch (IOException e) {
                failedSlavesList.add(node);
                failedSlavesBuffer.append(node.getNodeName()).append(" cause: ");
                failedSlavesBuffer.append(e.getMessage()).append(" ");
            }
        }
        String failedSlaves = failedSlavesBuffer.toString();
        if (!failedSlavesList.isEmpty()) {
            logger.log(Level.WARNING, Messages.CouldNotDelete(failedSlaves));
            throw new Failure(Messages.CouldNotDelete(failedSlavesList.toString()));
        }
        logger.log(Level.CONFIG, "User deleted the following slaves: " + nodeList.toString());
        rsp.sendRedirect2("deleted");
    }

    /**
     * Adds the slaves to create to the current NodeList.
     * @param rsp StaplerResponse.
     * @param req StaplerRequest.
     * @param slaveNames string with several names separated by space.
     * @param slaveName string with a single name, used when automatically creating a span of slaves.
     * @param mode different kinds of create nodes.
     * @param first the first number of the span.
     * @param last the last number of the span.
     * @param copyFrom a string containing the name of the node to copy.
     * @param extendedEnvInterpretation if extended environment variable interpretation should be used.
     * @throws IOException if slave creation goes wrong.
     * @throws Descriptor.FormException if slave creation goes wrong.
     * @throws Failure
     */
    public synchronized void doCreateSlaves(StaplerRequest req, StaplerResponse rsp,
                                            @QueryParameter String slaveNames, @QueryParameter String slaveName,
                                            @QueryParameter String mode, @QueryParameter String first,
                                            @QueryParameter String last, @QueryParameter String copyFrom,
                                            @QueryParameter boolean extendedEnvInterpretation)
            throws IOException, Descriptor.FormException {
        Hudson app = Hudson.getInstance();
        // Throws exception on failure. This is handled at a higher level.
        app.checkPermission(getRequiredPermission());
        NodeList nodeList = new NodeList();
        String currentSessionId = req.getSession().getId();

        HashSet<String> names = getSlaveNames(slaveNames, slaveName, first, last);

        if (names == null || names.isEmpty()) {
            throw new Failure(Messages.EmptyNameList());
        }

        if (mode != null && mode.equals("newSlave")) {
            for (String currentName : names) {
                DumbSlave slave = new DumbSlave(currentName, "", "", "", Node.Mode.NORMAL, "",
                        new ManagedWindowsServiceLauncher("", ""), new RetentionStrategy.Always(),
                        Collections.EMPTY_LIST);
                nodeList.add(slave);
            }
        } else if (mode != null && mode.equals("copySlave")) {
            Node src = app.getNode(copyFrom);
            if (src == null) {
                if (Util.fixEmpty(copyFrom) == null) {
                    throw new Failure(Messages.EmptyCopyString());
                } else {
                    throw new Failure(Messages.NoSlaveFound(copyFrom));
                }
            }
            if (extendedEnvInterpretation) {
                src = EnvironmentVariables.toVariables((DumbSlave)src);
            }
            for (String currentName : names) {
                DumbSlave copiedSlave = new DumbSlave(currentName, src.getNodeDescription(),
                        ((DumbSlave)src).getRemoteFS(), String.valueOf(src.getNumExecutors()), src.getMode(),
                        src.getLabelString(), ((DumbSlave)src).getLauncher(), ((DumbSlave)src).getRetentionStrategy(),
                        src.getNodeProperties().toList());
                copiedSlave = EnvironmentVariables.fromVariables(copiedSlave);
                nodeList.add(copiedSlave);
            }
        } else {
            rsp.sendError(SC_BAD_REQUEST);
            return;
        }
        nodeListMap.put(currentSessionId, nodeList);
        rsp.sendRedirect2("settingsselector");
    }

    /**
     * Calculates all new node names and returns them as a set.
     * @param slaveNames specific node names that is separated with space.
     * @param nodeName  contains a node name in a specific interval.
     * @param first the first node in a interval
     * @param last the last node in a interval
     * @return the names separated in an array
     * @throws Failure
     */
    public static HashSet<String> getSlaveNames(String slaveNames, String nodeName, String first, String last) {
        HashSet<String> names = new HashSet<String>();

        if (slaveNames != null && !slaveNames.isEmpty()) {
            //Throws Failure if not good names:
            Hudson.checkGoodName(slaveNames);
            String[] uniqueNames = slaveNames.split("\\s+");
            Collections.addAll(names, uniqueNames);
        }
        if (nodeName != null && !nodeName.isEmpty()) {
            Hudson.checkGoodName(nodeName);
            //The length before parsing to int with zeros at the beginning of the string counted
            int preLength = first.length();
            int firstInt;
            int lastInt;

            try {
                firstInt = Integer.parseInt(first);
                lastInt = Integer.parseInt(last);
            } catch (NumberFormatException e) {
                throw new Failure(Messages.WrongIntervalString(nodeName, first, last));
            }
            if (firstInt > lastInt) {
                throw new Failure(Messages.WrongIntervalString(nodeName, first, last));
            }

            for (int i = firstInt; i <= lastInt; i++) {
                //Add the new name and the missing zeros from the parsed int
                names.add(nodeName + String.format("%0" + preLength + "d", i));
            }
        }

        StringBuffer existingNames = new StringBuffer();
        for (String name : names) {
            if (Hudson.getInstance().getNode(name) != null) {
                existingNames.append(name).append(" ");
            }
        }
        if (existingNames.length() > 0) {
            throw new Failure(Messages.SlaveAlreadyExist(existingNames));
        }
        return names;
    }

    /**
     * Requests the selected slaves to go online.
     * @param rsp StaplerResponse.
     * @param req StaplerRequest.
     * @return false if no nodes were selected, otherwise true
     */
    @JavaScriptMethod
    public boolean takeOnline(StaplerRequest req, StaplerResponse rsp) {
        // Throws exception on failure. This is handled at a higher level.
        Hudson.getInstance().checkPermission(getRequiredPermission());
        String currentSessionId = req.getSession().getId();
        NodeList nodeList = getNodeList(currentSessionId);

        boolean result = false;
        if (nodeList != null) {
            result = true;
            for (Node node : nodeList) {
                Computer computer = node.toComputer();
                if (computer != null) {
                    computer.setTemporarilyOffline(false, null);
                }

            }
        }

        return result;
    }

    /**
     * Requests the selected slaves to go offline.
     * @param reason String.
     * @param rsp StaplerResponse.
     * @param req StaplerRequest.
     * @return false if no nodes were selected, otherwise true
     */
    @JavaScriptMethod
    public boolean takeOffline(String reason, StaplerRequest req, StaplerResponse rsp) {
        // Throws exception on failure. This is handled at a higher level.
        Hudson.getInstance().checkPermission(getRequiredPermission());
        String currentSessionId = req.getSession().getId();
        NodeList nodeList = getNodeList(currentSessionId);

        boolean result = false;
        if (nodeList != null) {
            result = true;
            reason = Util.fixEmptyAndTrim(reason);
            for (Node node : nodeList) {
                Computer computer = node.toComputer();
                if (computer != null) {
                    if (reason == null) {
                        computer.setTemporarilyOffline(true, null);
                    } else {
                        OfflineCause cause = new OfflineCause.UserCause(User.current(), reason);
                        computer.setTemporarilyOffline(true, cause);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Connects (not forced) to selected slaves.
     * @param rsp StaplerResponse.
     * @param req StaplerRequest.
     * @return false if no nodes were selected, otherwise true
     */
    @JavaScriptMethod
    public boolean connectSlaves(StaplerRequest req, StaplerResponse rsp) {
        // Throws exception on failure. This is handled at a higher level.
        Hudson.getInstance().checkPermission(getRequiredPermission());
        String currentSessionId = req.getSession().getId();
        NodeList nodeList = getNodeList(currentSessionId);

        boolean result = false;
        if (nodeList != null) {
            result = true;
            for (Node node : nodeList) {
                Computer computer = node.toComputer();
                if (computer != null && computer.getChannel() == null) {
                    computer.connect(false);
                }
            }
        }

        return result;
    }

    /**
     * Disconnects from selected slaves.
     * @param reason the reason for disconnecting
     * @param rsp StaplerResponse.
     * @param req StaplerRequest.
     * @return false if no nodes were selected, otherwise true
     */
    @JavaScriptMethod
    public boolean disconnectSlaves(String reason, StaplerRequest req, StaplerResponse rsp) {
        // Throws exception on failure. This is handled at a higher level.
        Hudson.getInstance().checkPermission(getRequiredPermission());
        String currentSessionId = req.getSession().getId();
        NodeList nodeList = getNodeList(currentSessionId);

        boolean result = false;
        if (nodeList != null) {
            result = true;
            reason = Util.fixEmptyAndTrim(reason);
            for (Node node : nodeList) {
                Computer computer = node.toComputer();
                if (computer != null) {
                    if (reason == null) {
                        computer.disconnect(null);
                    } else {
                        OfflineCause cause = new OfflineCause.UserCause(User.current(), reason);
                        computer.disconnect(cause);
                    }
                }
            }
        }

        return result;
    }

    /**
     * This is needed by settingsselector.jelly to list the Node properties the user can add or change.
     * @return the {@link NodePropertyDescriptor} for {@link hudson.slaves.DumbSlave}
     */
    public List<NodePropertyDescriptor> getNodePropertyDescriptors() {
        List<NodePropertyDescriptor> descriptors = Functions.getNodePropertyDescriptors(DumbSlave.class);
        return descriptors;
    }

    /**
     * This is needed by settingsselector.jelly to list the Node properties the user can remove.
     * @return the {@link NodePropertyDescriptor} for {@link hudson.slaves.DumbSlave}
     */
    public List<RemovePropertyDescriptor> getRemovePropertyDescriptor() {
        List<NodePropertyDescriptor> descriptors = Functions.getNodePropertyDescriptors(DumbSlave.class);
        List<RemovePropertyDescriptor> removeProperyDescriptors = new LinkedList<RemovePropertyDescriptor>();
        for (NodePropertyDescriptor descriptor : descriptors) {
            removeProperyDescriptors.add(new RemovePropertyDescriptor(descriptor));
        }

        return removeProperyDescriptors;
    }

    /**
     * Maps a class name to a real descriptor name. This is to have something readable to show the user when
     * properties are removed.
     * @param className the class name to match with a descriptor name.
     * @return the readable name match
     */
    public String mapClassToDescriptorName(String className) {
        String descriptorName = "";

        List<NodePropertyDescriptor> descriptors = Functions.getNodePropertyDescriptors(DumbSlave.class);
        for (NodePropertyDescriptor descriptor : descriptors) {
            if (descriptor.getClass().getName().equals(className)) {
                descriptorName = descriptor.getDisplayName();
                break;
            }
        }

        return descriptorName;
    }

    /**
     * Descriptor to show in the removal list for node properties.
     * The point is that it should only have a name that can be used to select properties to remove
     * without showing the whole descriptor.
     */
    public static class RemovePropertyDescriptor extends Descriptor {

        /**
         * The real descriptor this instance is bound to.
         */
        private final Descriptor propertyDescriptor;

        /**
         * Default constructor. sends the class of the bounded propertyDescriptor to Descriptor base class
         * which then sets it as clazz attribute for this class.
         * @param propertyDescriptor the bounded ordinary descriptor of the node property
         */
        public RemovePropertyDescriptor(Descriptor propertyDescriptor) {
            super(propertyDescriptor.getClass());
            this.propertyDescriptor = propertyDescriptor;
        }

        @Override
        public String getDisplayName() {
            return propertyDescriptor.getDisplayName();
        }
    }
}
