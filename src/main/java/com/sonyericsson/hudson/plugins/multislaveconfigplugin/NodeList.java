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
import hudson.model.Failure;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.os.windows.ManagedWindowsServiceLauncher;
import hudson.slaves.CommandLauncher;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SimpleScheduledRetentionStrategy;
import hudson.util.DescribableList;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.Stapler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.Setting.IN_DEMAND_DELAY;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.Setting.IDLE_DELAY;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.Setting.UPTIME_MINS;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.Setting.START_TIME_SPEC;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.Setting.KEEP_UP_WHEN_ACTIVE;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.Setting.USERNAME;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.Setting.PASSWORD_STRING;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.Setting.LAUNCH_COMMAND;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.Setting.TUNNEL;
import static com.sonyericsson.hudson.plugins.multislaveconfigplugin.Setting.VM_ARGS;

/**
 * Manages a list of nodes.
 * @author Fredrik Persson &lt;fredrik4.persson@sonyericsson.com&gt;
 * @author Nicklas Nilsson &lt;nicklas3.nilsson@sonyericsson.com&gt;
 */
public class NodeList extends ArrayList<Node> {

    private static final Logger logger = Logger.getLogger(NodeList.class.getName());

    /**
     * Constructor to help adding existing nodes to a new list.
     * @param list the nodes to be added
     */
    public NodeList(List<Node> list) {
        super();
        addAll(list);
    }

    /**
     * Redirecting to the super class constructor.
     */
    public NodeList() {
        super();
    }

    /**
     * Checks if there are any DumbSlaves in the list instead of any Node.
     * @return if are any slaves in the list
     */
    public boolean isEmpty() {
        if (super.isEmpty()) {
            return true;
        }
        for (Node node : this) {
            if (node instanceof DumbSlave) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sorts this list by name.
     * @return sorted list.
     */
    public NodeList sortByName() {
        Collections.sort(this, new Comparator<Node>() {
            public int compare(Node n1, Node n2) {
                if (n1 == null && n2 == null) {
                    return 0;
                }
                if (n1 == null) {
                    return -1;
                }
                if (n2 == null) {
                    return 1;
                }
                return n1.getNodeName().compareTo(n2.getNodeName());
            }
        });
        return this;
    }

    /**
     * Represents this list as a String using the node names separated with space.
     * @return list as String
     */
    public String toString() {
        StringBuilder nodeNames = new StringBuilder();
        for (Node node : this) {
            nodeNames.append(node.getNodeName()).append(" ");
        }
        return nodeNames.toString().trim();
    }

    /**
     * Represents this list as a JSONArray.
     * @return JSONArray with matching slave-representations from this NodeList.
     */
    public JSONArray toJSONArray() {
        JSONArray slaveJSONArray = new JSONArray();
        for (Node node : this) {
            if (node instanceof DumbSlave) {
                DumbSlave slave = (DumbSlave)node;
                JSONObject slaveRepresentation = new JSONObject();

                slaveRepresentation.put("name", slave.getNodeName());
                slaveRepresentation.put("icon", slave.getComputer().getIcon());
                slaveRepresentation.put("iconAltText", slave.getComputer().getIconAltText());
                slaveRepresentation.put("labels", slave.getLabelString());
                slaveRepresentation.put("executors", slave.getNumExecutors());
                slaveRepresentation.put("remoteFS", slave.getRemoteFS());
                slaveRepresentation.put("description", slave.getNodeDescription());
                slaveJSONArray.add(slaveRepresentation);
            }
        }
        return slaveJSONArray;
    }

    /**
     * Gets the first Slave in this list.
     * @return the first Slave in the list if there is any
     */
    protected DumbSlave getFirstSlave() {
        if (super.isEmpty()) {
            return null;
        }
        for (Node node : this) {
            if (node instanceof DumbSlave) {
                return (DumbSlave)node;
            }
        }
        return null;
    }

    /**
     * Checks if the slaves in this list still exist in the master nodelist.
     * @return if all the slaves still exist or not
     */
    protected boolean slavesStillExist() {
        Hudson app = Hudson.getInstance();
        for (Node node : this) {
            if (app.getNode(node.getNodeName()) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Changes the settings for all nodes in the list.
     * @param settings the settings to make as a hashmap
     * @return The changed list
     */
    protected synchronized NodeList changeSettings(HashMap settings) {
        //The nodes that are not in the newNodeList
        List<Node> complementaryNodes = getComplementaryNodes();
        List<Node> newNodeList = new ArrayList<Node>(complementaryNodes);

        for (Node node : this) {
            String newDescription = (String)settings.get("description");
            String newRemoteFS = (String)settings.get("remoteFS");
            String newNumExecutors = (String)settings.get("numExecutors");
            String newSetLabels = (String)settings.get("setLabelString");
            String newLabelsToAdd = (String)settings.get("addLabelString");
            String newLabelsToRemove = (String)settings.get("removeLabelString");
            Node.Mode newMode = (Node.Mode)settings.get("mode");
            ComputerLauncher newLauncher = (ComputerLauncher)settings.get("launcher");
            RetentionStrategy newRetentionStrategy = (RetentionStrategy)settings.get("retentionStrategy");
            List<NodeProperty<?>> newProperties = (List<NodeProperty<?>>)settings.get("addOrChangeProperties");
            List<String> removeProperties = (List<String>)settings.get("removeProperties");

            if (node instanceof DumbSlave) {
                DumbSlave slave = (DumbSlave)node;

                if (newDescription == null) {
                    newDescription = slave.getNodeDescription();
                }
                if (newRemoteFS == null) {
                    newRemoteFS = slave.getRemoteFS();
                }
                if (newNumExecutors == null) {
                    newNumExecutors = String.valueOf(slave.getNumExecutors());
                }
                if (newMode == null) {
                    newMode = slave.getMode();
                }
                if (newSetLabels == null) {
                    newSetLabels = slave.getLabelString();
                }
                if (newLauncher == null) {
                    newLauncher = slave.getLauncher();
                }
                if (newRetentionStrategy == null) {
                    newRetentionStrategy = slave.getRetentionStrategy();
                }

                DescribableList<NodeProperty<?>, NodePropertyDescriptor> describableList = slave.getNodeProperties();
                List<NodeProperty<?>> oldProperties = describableList.toList();
                newProperties = getNewProperties(newProperties, oldProperties, removeProperties);

                newLabelsToAdd = EnvironmentVariables.fromVariables(slave, newLabelsToAdd);
                newLabelsToRemove = EnvironmentVariables.fromVariables(slave, newLabelsToRemove);
                newSetLabels = addLabels(newLabelsToAdd, newSetLabels);
                newSetLabels = removeLabels(newLabelsToRemove, newSetLabels);

                DumbSlave changedSlave;

                try {
                    changedSlave = new DumbSlave(slave.getNodeName(), newDescription, newRemoteFS, newNumExecutors,
                            newMode, newSetLabels, newLauncher, newRetentionStrategy, newProperties);
                } catch (Descriptor.FormException e) {
                    logger.log(Level.WARNING, "Failed to edit slave " + slave.getNodeName()
                            + " cause: " + e.getMessage());
                    throw new Failure(Messages.FailedToEditSlave(slave.getNodeName()));
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to edit slave " + slave.getNodeName()
                            + " cause: " + e.getMessage());
                    throw new Failure(Messages.FailedToEditSlave(slave.getNodeName()));
                }
                changedSlave = EnvironmentVariables.fromVariables(changedSlave);
                try {
                    newNodeList.add(changedSlave);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to edit slave " + changedSlave.getNodeName()
                            + " cause: " + e.getMessage());
                    throw new Failure(Messages.FailedToEditSlave(changedSlave.getNodeName()));
                }
            } else { //Not Dumbslave, dont't touch it.
                newNodeList.add(node);
            }
        }
        try {
            Hudson.getInstance().setNodes(newNodeList);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to edit nodelist!");
            throw new Failure(Messages.FailedToEditNodeList());
        }
        newNodeList.removeAll(complementaryNodes);
        return new NodeList(newNodeList);
    }

    /**
     * The precedence of the operations are: Old, remove, new.
     * Remove will only affect current properties, new will replace old properties with new.
     * @param newProperties the properties to add to the slave
     * @param oldProperties the properties the slave currently has
     * @param removeProperties the properties to remove from the slave
     * @return a property list matching the precedence.
     */
    private List<NodeProperty<?>> getNewProperties(List<NodeProperty<?>> newProperties,
                                                   List<NodeProperty<?>> oldProperties,
                                                   List<String> removeProperties) {
        List<NodeProperty<?>> ret = new LinkedList<NodeProperty<?>>();
        if (oldProperties != null) {
            ret.addAll(oldProperties);
        }

        if (removeProperties != null) {
            for (String propertyToRemove : removeProperties) {
                for (NodeProperty<?> property : ret) {
                    if (property.getDescriptor().getClass().getName().equals(propertyToRemove)) {
                        ret.remove(property);
                        break;
                    }
                }
            }
        }

        if (newProperties != null) {
            for (NodeProperty<?> newProperty : newProperties) {
                for (NodeProperty<?> property : ret) {
                    if (property.getClass() == newProperty.getClass()) {
                        ret.remove(property);
                        break;
                    }
                }

                ret.add(newProperty);
            }
        }

        return ret;
    }

    /**
     * Adds one or more labels to an existing String. Makes sure each label appears only once.
     * @param labelsToAdd the labels to be added
     * @param oldLabels the already existing label string
     * @return the new label string
     */
    protected String addLabels(String labelsToAdd, String oldLabels) {
        if (labelsToAdd == null) {
            return oldLabels;
        }
        String[] labelsToAddArray = labelsToAdd.split("\\s+");
        List<String> oldLabelsList = Arrays.asList(oldLabels.split("\\s+"));
        StringBuilder newLabelsBuilder = new StringBuilder(oldLabels).append(" ");

        for (String currentToken : labelsToAddArray) {
            if (!oldLabelsList.contains(currentToken)) {
                newLabelsBuilder.append(currentToken).append(" ");
            }
        }
        return newLabelsBuilder.toString();
    }

    /**
     * Removes one or more labels from an existing String.
     * @param labelsToRemove the labels to be removed
     * @param oldLabels the already existing label string
     * @return the new label string
     */
    protected String removeLabels(String labelsToRemove, String oldLabels) {
        if (labelsToRemove == null) {
            return oldLabels;
        }
        String[] oldLabelsArray = oldLabels.split("\\s+");
        List<String> labelsToRemoveList = Arrays.asList(labelsToRemove.split("\\s+"));
        StringBuilder newLabelsBuilder = new StringBuilder();

        for (String currentToken : oldLabelsArray) {
            if (!labelsToRemoveList.contains(currentToken)) {
                newLabelsBuilder.append(currentToken).append(" ");
            }
        }
        return newLabelsBuilder.toString();
    }

    /**
     *  Returns the Jenkins registered nodes that are not in the given list.
     * @return the complementary list
     */
    protected List<Node> getComplementaryNodes() {
        List<Node> complementaryNodes = new ArrayList<Node>();
        for (Node node : Hudson.getInstance().getNodes()) {
            if (node != null && !this.contains(node)) {
                complementaryNodes.add(node);
            }
        }
        return complementaryNodes;
    }

    /**
     * Checks all the labels in the parameter to make sure each of them exist on at least one slave in this list.
     * @param labels the labels to check.
     * @return true/false if they existed.
     */
    protected boolean hasLabels(String labels) {
        if (labels == null) {
            return true;
        }
        String[] labelsArray = labels.split("\\s+");

        for (String currentToken : labelsArray) {
            boolean tokenExists = false;
            for (Node node : this) {
                String[] nodeLabelsArray = node.getLabelString().split("\\s+");
                List<String> nodeLabelsList = Arrays.asList(nodeLabelsArray);
                if (nodeLabelsList.contains(currentToken)) {
                    tokenExists = true;
                    break;
                }
            }
            if (!tokenExists) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts submitted form (JSON) to a HashMap containing only the settings to change.
     * Using Strings and not enum Setting as keys since the jelly scripts don't seem to get enum keys correctly.
     * @param json to interpret
     * @return the hash map
     */
    protected static HashMap interpretJSON(JSONObject json) {
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        // prefix "_" represents the checkbox for the given option
        if (json.has("_description") && (Boolean)json.get("_description")) {
            hashMap.put("description", json.get("description"));
        }
        if (json.has("_remoteFS") && (Boolean)json.get("_remoteFS")) {
            hashMap.put("remoteFS", json.get("remoteFS"));
        }
        if (json.has("_numExecutors") && (Boolean)json.get("_numExecutors")) {
            hashMap.put("numExecutors", json.get("numExecutors"));
        }
        if (json.has("_mode") && (Boolean)json.get("_mode")) {
            String modeS = (String)json.get("mode");
            if (modeS.equals("NORMAL")) {
                hashMap.put("mode", Node.Mode.NORMAL);
            } else if (modeS.equals("EXCLUSIVE")) {
                hashMap.put("mode", Node.Mode.EXCLUSIVE);
            } else {
                throw new Failure(Messages.UndefinedMode());
            }
        }
        if (json.has("_labelString") && (Boolean)json.get("_labelString")) {
            hashMap.put("setLabelString", json.get("labelString"));
        }
        if (json.has("_addLabelString") && (Boolean)json.get("_addLabelString")) {
            hashMap.put("addLabelString", json.get("addLabelString"));
        }
        if (json.has("_removeLabelString") && (Boolean)json.get("_removeLabelString")) {
            hashMap.put("removeLabelString", json.get("removeLabelString"));
        }
        if (json.has("_launcher") && (Boolean)json.get("_launcher")) {
            hashMap.put("launcher", Stapler.getCurrentRequest().bindJSON(ComputerLauncher.class,
                    (JSONObject)json.get("launcher")));
        }
        if (json.has("_retentionStrategy") && (Boolean)json.get("_retentionStrategy")) {
            hashMap.put("retentionStrategy", Stapler.getCurrentRequest().bindJSON(RetentionStrategy.class,
                    (JSONObject)json.get("retentionStrategy")));
        }

        if (json.has("addOrChangeProperties")) {
            List<NodeProperty<?>> addOrChangeProperties = extractAddOrChangeProperties(
                    json.get("addOrChangeProperties"));
            if (!addOrChangeProperties.isEmpty()) {
                hashMap.put("addOrChangeProperties", addOrChangeProperties);
            }
        }

        if (json.has("removeProperties")) {
            List<String> removeProperties = extractRemoveProperties(json.get("removeProperties"));
            if (!removeProperties.isEmpty()) {
                hashMap.put("removeProperties", removeProperties);
            }
        }

        return hashMap;
    }

    /**
     * Fetches the properties that were added or changed by the user.
     * @param addOrChangeProperties the JSON data submitted with the form
     * @return the list of Node Properties that were added or changed
     */
    private static List<NodeProperty<?>> extractAddOrChangeProperties(Object addOrChangeProperties) {
        List<NodeProperty<?>> ret = new ArrayList<NodeProperty<?>>();
        JSONArray array = new JSONArray();

        if (addOrChangeProperties instanceof JSONArray) { // Several Properties selected by the user
            array = (JSONArray)addOrChangeProperties;
        } else if (addOrChangeProperties instanceof JSONObject) { // Exactly one property selected by the user
            array.add(addOrChangeProperties);
        }

        for (Object elem : array) {
            NodeProperty<?> property = Stapler.getCurrentRequest().bindJSON(NodeProperty.class, (JSONObject)elem);
            ret.add(property);
        }

        return ret;

    }

    /**
     * Fetches the properties that were removed by the user.
     * @param removeProperties the JSON data submitted with the form
     * @return the list of Node Properties that were removed
     */
    private static List<String> extractRemoveProperties(Object removeProperties) {
        List<String> ret = new ArrayList<String>();
        JSONArray array = new JSONArray();

        if (removeProperties instanceof JSONArray) { // Several Properties selected by the user
            array = (JSONArray)removeProperties;
        } else if (removeProperties instanceof JSONObject) { // Exactly one property selected by the user
            array.add(removeProperties);
        }

        for (Object elem : array) {
            String className = ((JSONObject)elem).getString("kind");
            ret.add(className);
        }

        return ret;
    }

    /**
     * Method for getting common settings of slaves by using a String as type.
     * Needed since jelly doesn't seem to work with enum.
     * @param type the setting type as a String
     * @return the common setting string if there was one
     */
    public String getCommon(String type) {
        return getCommon(Setting.valueOf(type));
    }

    /**
     * Gets the common setting of given type for all slaves in this list. Returns null if no common setting is available.
     * @param type the setting type to get
     * @return the common setting string if there was any
     */
    protected String getCommon(Setting type) {
        if (isEmpty()) {
            throw new Failure(Messages.EmptyNodeList());
        }

        String environmentFirstString;
        String comparableString;
        String environmentComparableString;

        String firstString = type.getSettingString(getFirstSlave());
        environmentFirstString = EnvironmentVariables.toVariables(getFirstSlave(), firstString);

        boolean exactSameStrings = true;
        boolean environmentSameStrings = true;

        for (Node node : this) {
            if (node instanceof DumbSlave) {
                DumbSlave slave = (DumbSlave)node;
                comparableString = type.getSettingString(slave);
                environmentComparableString = EnvironmentVariables.toVariables(slave, comparableString);

                if (!comparableString.equals(firstString)) {
                    exactSameStrings = false;
                }
                if (!environmentComparableString.equals(environmentFirstString)) {
                    environmentSameStrings = false;
                }
                if (!exactSameStrings && !environmentSameStrings) {
                    return null;
                }
            }
        }
        if (exactSameStrings) {
            return firstString;
        }
        return environmentFirstString;
    }

    /**
     * Gets the common mode setting for all slaves in this list. Returns null if no common mode setting is available.
     * @return the common mode if there is any
     */
    public Node.Mode getMode() {
        if (isEmpty()) {
            throw new Failure(Messages.EmptyNodeList());
        }

        Node.Mode firstMode = getFirstSlave().getMode();
        for (Node node : this) {
            if (!node.getMode().equals(firstMode) && node instanceof DumbSlave) {
                return null;
            }
        }
        return firstMode;
    }

    /**
     * Gets the common ComputerLauncher for all slaves in this list. Returns null if no common launcher is available.
     * If the launcher types are the same, it will also compare the specific settings,
     * for example the launch command if all were CommandLaunchers. If all were CommandLaunchers with different
     * launch commands, the returned launcher will have an empty launch command. However,
     * if they had the same launch command, it will return a launcher with that common command.
     * @return the common ComputerLauncher
     */
    public ComputerLauncher getLauncher() {
        ComputerLauncher firstLauncher = (getFirstSlave()).getLauncher();

        //Returns null if one launcher type differs from the others
        for (Node node : this) {
            if (node instanceof DumbSlave) {
                if (!((DumbSlave)node).getLauncher().getDescriptor().equals(firstLauncher.getDescriptor())) {
                    return null;
                }
            }
        }

        //Equivalent to all launchers are ManagedWindowsServiceLaunchers
        if (firstLauncher instanceof ManagedWindowsServiceLauncher) {
            return new ManagedWindowsServiceLauncher(getCommon(USERNAME), getCommon(PASSWORD_STRING));
        } else if (firstLauncher instanceof CommandLauncher) {
            return new CommandLauncher(getCommon(LAUNCH_COMMAND));
        } else if (firstLauncher instanceof JNLPLauncher) {
            return new JNLPLauncher(getCommon(TUNNEL), getCommon(VM_ARGS));
        }

        return firstLauncher;
    }

    /**
     * Gets the common RetentionStrategy for all nodes in this list. Returns null if no common strategy is available.
     * If the strategy types are the same, it will also compare the specific settings,
     * for example the startup schedule if all were SimpleScheduledRetentionStrategies.
     * If all were SimpleScheduledRetentionStrategies with different startup schedule settings,
     * the returned RetentionStrategy will have an empty startup schedule. However,
     * if they had the same startup schedule, it will return a launcher with that startup schedule.
     * @return the common RetentionStrategy
     */
    public RetentionStrategy getRetentionStrategy() {
        RetentionStrategy firstRetentionStrategy = (getFirstSlave()).getRetentionStrategy();

        for (Node node : this) {
            if (node instanceof DumbSlave) {
                if (!((DumbSlave)node).getRetentionStrategy().getDescriptor().
                        equals(firstRetentionStrategy.getDescriptor())) {
                    return null;
                }
            }
        }

        if (firstRetentionStrategy instanceof RetentionStrategy.Demand) {
            long commonInDemandDelay;
            long commonIdleDelay;
            try {
                commonInDemandDelay = Long.parseLong(getCommon(IN_DEMAND_DELAY));
            } catch (NumberFormatException e) {
                commonInDemandDelay = 0;
            }
            try {
                commonIdleDelay = Long.parseLong(getCommon(IDLE_DELAY));
            } catch (NumberFormatException e) {
                commonIdleDelay = 1;
            }
            return new RetentionStrategy.Demand(commonInDemandDelay,
                    commonIdleDelay);

        } else if (firstRetentionStrategy instanceof SimpleScheduledRetentionStrategy) {
            int upTimeMins;
            try {
                upTimeMins = Integer.parseInt(getCommon(UPTIME_MINS));
            } catch (NumberFormatException e) {
                upTimeMins = 1;
            }
            String commonStartTimeSpec = getCommon(START_TIME_SPEC);
            if (commonStartTimeSpec == null) {
                commonStartTimeSpec = "";
            }
            String commonKeepUpWhenActive = getCommon(KEEP_UP_WHEN_ACTIVE);
            if (commonKeepUpWhenActive == null) {
                commonKeepUpWhenActive = "true";
            }
            try {
                return new SimpleScheduledRetentionStrategy(commonStartTimeSpec, upTimeMins,
                        Boolean.parseBoolean(commonKeepUpWhenActive));
            } catch (ANTLRException e) {
                throw new Failure(Messages.FailedToCreateRetentionStrategy());
            }
        }
        return firstRetentionStrategy;
    }

    /**
     * Returns a description to be used by the jelly scripts if the launcher settings differs.
     * @return generated description
     */
    public String getLauncherDescription() {
        if (getLauncher() == null) {
            return Messages.DifferentLaunchMethods();
        }
        if (getFirstSlave().getLauncher() instanceof ManagedWindowsServiceLauncher) {
            String commonUserName = getCommon(USERNAME);
            String commonPassword = getCommon(PASSWORD_STRING);
            if (commonUserName == null && commonPassword == null) {
                return Messages.DifferentUsernamePassword();
            } else if (commonUserName == null) {
                return Messages.DifferentUsername();
            } else if (commonPassword == null) {
                return Messages.DifferentPassword();
            } else {
                return ""; //The settings were same
            }
        } else if (getFirstSlave().getLauncher() instanceof CommandLauncher) {
            if (getCommon(LAUNCH_COMMAND) == null) {
                return Messages.DifferentLaunchCommand();
            } else {
                return "";
            }
        } else if (getFirstSlave().getLauncher() instanceof JNLPLauncher) {
            String commonVmargs = getCommon(VM_ARGS);
            String commonTunnel = getCommon(TUNNEL);
            if (commonVmargs == null && commonTunnel == null) {
                return Messages.DifferentVMArgTunnel();
            } else if (commonVmargs == null) {
                return Messages.DifferentVMArg();
            } else if (commonTunnel == null) {
                return Messages.DifferentTunnel();
            } else {
                return "";
            }
        }
        return Messages.UnableToCompareLaunchMethods();
    }

    /**
     *  Returns a description to be used by the jelly scripts if the retention strategy settings differs.
     * @return the description
     */
    public String getRetentionDescription() {
        if (getRetentionStrategy() == null) {
            return Messages.DifferentRetentionStrategies();
        }

        if (getFirstSlave().getRetentionStrategy() instanceof RetentionStrategy.Demand) {
            String commonInDemandDelay = getCommon(IN_DEMAND_DELAY);
            String commonIdleDelay = getCommon(IDLE_DELAY);
            if (commonInDemandDelay == null && commonIdleDelay == null) {
                return Messages.DifferentInDemandDelayIdleDelay();
            } else if (commonInDemandDelay == null) {
                return Messages.DifferentInDemandDelay();
            } else if (commonIdleDelay == null) {
                return Messages.DifferentIdleDelay();
            } else {
                //Settings were the same
                return "";
            }
        } else if (getFirstSlave().getRetentionStrategy() instanceof SimpleScheduledRetentionStrategy) {
            String commonStartTimeSpec = getCommon(START_TIME_SPEC);
            String commonUpTimeMins = getCommon(UPTIME_MINS);
            String commonKeepUpWhenActive = getCommon(KEEP_UP_WHEN_ACTIVE);
            if (commonStartTimeSpec == null && commonUpTimeMins == null
                    && commonKeepUpWhenActive == null) {
                return Messages.DifferentStartupScheduleScheduledUptimeCheckboxValue();
            } else if (commonStartTimeSpec == null && commonUpTimeMins == null) {
                return Messages.DifferentStartupScheduleScheduledUptime();
            } else if (commonUpTimeMins == null && commonKeepUpWhenActive == null) {
                return Messages.DifferentScheduledUptimeCheckboxValue();
            } else if (commonStartTimeSpec == null && commonKeepUpWhenActive == null) {
                return Messages.DifferentStartupScheduleCheckboxValue();
            } else if (commonStartTimeSpec == null) {
                return Messages.DifferentStartupSchedule();
            } else if (commonUpTimeMins == null) {
                return Messages.DifferentScheduledUptime();
            } else if (commonKeepUpWhenActive == null) {
                return Messages.DifferentCheckboxValues();
            } else {
                return "";
            }
        } else if (getFirstSlave().getRetentionStrategy() instanceof RetentionStrategy.Always) {
            return "";
        }
        return Messages.UnableToCompareRetentionStrategies();
    }
}
