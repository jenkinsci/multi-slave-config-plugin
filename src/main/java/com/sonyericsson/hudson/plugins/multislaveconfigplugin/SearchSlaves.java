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

import java.util.ArrayList;
import java.util.Collections;

/**
 * Class for searching and adding dumb slaves to a list.
 * @author Nicklas Nilsson &lt;nicklas3.nilsson@sonyericsson.com&gt;
 * @author Fredrik Persson &lt;fredrik4.persson@sonyericsson.com&gt;
 */
public class SearchSlaves {

    /**
     * Not allowing to create instances of this class.
     */
    protected SearchSlaves() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets a list of DumbSlaves that matches the search parameters.
     * @param searchParameters submitted form containing what to search for.
     * @return list of matching slaves
     */
    public static NodeList getNodes(JSONObject searchParameters) {
        NodeList returnList = new NodeList(Hudson.getInstance().getNodes());
        NodeList masterList = new NodeList(Hudson.getInstance().getNodes());

        for (Node node : masterList) {
            if ((node instanceof DumbSlave)) {

                DumbSlave slave = (DumbSlave)node;

                String searchString = (String)searchParameters.get("description");
                if (!hasSearchHit(slave, searchString, slave.getNodeDescription())) {
                    returnList.remove(node);
                }
                try {
                    int searchInt = Integer.parseInt((String)searchParameters.get("executors"));
                    if (node.getNumExecutors() != searchInt) {
                        returnList.remove(node);
                    }
                //CS IGNORE EmptyBlock FOR NEXT 1 LINES. REASON: Don't need to catch anything.
                } catch (NumberFormatException ignored) { }

                searchString = (String)searchParameters.get("remoteFS");
                if (!hasSearchHit(slave, searchString, slave.getRemoteFS())) {
                    returnList.remove(node);
                }
                searchString = (String)searchParameters.get("labels");
                if (!hasSearchHit(slave, searchString, slave.getLabelString())) {
                    returnList.remove(node);
                }
                searchString = (String)searchParameters.get("name");
                if (!hasSearchHit(slave, searchString, slave.getNodeName())) {
                    returnList.remove(node);
                }
            } else { returnList.remove(node); }
        }
        return returnList;
    }

    /**
     * Method that searches for a specific parameter on a slave and returns true/false if it's found.
     * @param slave the slave to search for environment variables on.
     * @param searchParameter the parameter to search for on the slave.
     * @param slaveParameter  the current slaves parameter to compare with.
     * @return true if the search parameters is found.
     */
    public static boolean hasSearchHit(DumbSlave slave, String searchParameter, String slaveParameter) {
        if (searchParameter == null || searchParameter.isEmpty()) {
            return true;
        }
        if (slaveParameter.isEmpty()) {
            return false;
        }

        searchParameter = makeSearchable(searchParameter);
        String environmentSlaveParameter = makeSearchable(EnvironmentVariables.toVariables(slave, slaveParameter));
        slaveParameter = makeSearchable(slaveParameter);

        ArrayList<String> searchParameters = new ArrayList<String>();
        Collections.addAll(searchParameters, searchParameter.split("\\s+"));

        ArrayList<String> environmentSlaveParameters = new ArrayList<String>();
        Collections.addAll(environmentSlaveParameters, environmentSlaveParameter.split("\\s+"));

        ArrayList<String> slaveParameters = new ArrayList<String>();
        Collections.addAll(slaveParameters, slaveParameter.split("\\s+"));

        for (String currentSearchParameter : searchParameters) {
            boolean anySlaveParameterContained = false;
            for (int i = 0; i < slaveParameters.size(); i++) {
                if (slaveParameters.get(i).contains(currentSearchParameter)) {
                    anySlaveParameterContained = true;
                    break;
                } else if (currentSearchParameter.contains("$")) {
                    if (environmentSlaveParameters.get(i).contains(currentSearchParameter)) {
                        anySlaveParameterContained = true;
                        break;
                    }
                }
            }
            if (!anySlaveParameterContained) {
                return false;
            }
        }
        return true;
    }

    /**
     * Formatting a string to ignore case sensitiveness and trimming leading and trailing whitespaces.
     * @param string the string to make search friendly.
     * @return search friendly string.
     */
    public static String makeSearchable(String string) {
        if (string == null) {
            return "";
        }
        return string.toLowerCase().trim();
    }
}
