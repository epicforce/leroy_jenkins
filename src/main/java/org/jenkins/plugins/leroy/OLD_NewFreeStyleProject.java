/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkins.plugins.leroy;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.model.Node;
import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.leroy.util.Constants;
import org.jenkins.plugins.leroy.util.JsonUtils;
import org.jenkins.plugins.leroy.util.LeroyUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yunus
 */

// Leroy deployment job
public class OLD_NewFreeStyleProject {

    private static Logger LOGGER = Logger.getLogger(OLD_NewFreeStyleProject.class.getName());


    /**
     * update build with leroy parameters
     *
     * @param req
     * @param rsp
     * @throws IOException
     * @throws ServletException
     * @throws hudson.model.Descriptor.FormException
     */
    @Override
    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, Descriptor.FormException {

        JSONObject json = req.getSubmittedForm();
        String jsonStr = json.toString();
        LOGGER.fine("Submitted form: " + jsonStr);

        List<LeroyBuilder.Target> targets = JsonUtils.getTargets(jsonStr);

        LOGGER.fine("Got " + (targets == null ? "null" : targets.size()) + " targets from JSON form.");

        // now figure out a default target and enabled targets configurations
        if (!CollectionUtils.isEmpty(targets)) {
            // default target is a target which is chosen if the job is run by some trigger
            LeroyBuilder.Target defaultTarget = null;
            for (LeroyBuilder.Target t : targets) {
                if (t.autoDeploy == true) {
                    defaultTarget = t;
                    break;
                }
            }

            // move default target to the first place - in this case Jenkins will select it as default
            LOGGER.fine("Default target configuration is :" + (defaultTarget == null ? "null" : defaultTarget.toString()));
            if (defaultTarget != null) {
                targets.remove(defaultTarget);
                targets.add(0, defaultTarget);
            }

            // targets -> build parameters
            List<String> buildParamsTargets = new ArrayList<String>();
            for (LeroyBuilder.Target t : targets) {
                String param = Constants.ENVIRONMENT_PARAM + "=" + t.environment
                        + ", " + Constants.WORKFLOW_PARAM + "=" + t.workflow
                        + ", " + Constants.CONFIG_SOURCE_PARAM + "=" + t.configSource;
                buildParamsTargets.add(param);
                LOGGER.fine("Target Configuration : " + param);
            }

            // add parameters to request
            String targetConfigurations = StringUtils.join(buildParamsTargets, "\\n");
            JSONObject properties = json.getJSONObject("properties");
            LOGGER.fine("JSONObject: properties :" + properties);
            if (properties.size() != 0) {
                JSONObject paramDefProp = properties.getJSONObject("hudson-model-ParametersDefinitionProperty");
                LOGGER.fine("JSONObject: hudson-model-ParametersDefinitionProperty :" + paramDefProp);
                JSONObject parameterized = null;
                if (paramDefProp.size() != 0) {
                    parameterized = paramDefProp.getJSONObject("parameterized");
                    LOGGER.fine("JSONObject: parameterized :" + parameterized);
                    JSON parameter = null;
                    try {
                        parameter = parameterized.getJSONObject("parameter");
                    } catch (Exception e) {
                        parameter = parameterized.getJSONArray("parameter");
                    }
                    LOGGER.fine("JSONObject: parameter :" + parameter);
                    JSONArray arr = null;
                    if (parameter.size() != 0) {
                        String paramJson = "{\"name\":\"" + Constants.TARGET_CONFIGURATION + "\",\"choices\":\"" + targetConfigurations + "\",\"description\":\"\",\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"}";
                        if (parameter instanceof JSONObject) {
                            LOGGER.fine("parameter is JSONObject");
                            if (((JSONObject) parameter).getString("name").equals(Constants.TARGET_CONFIGURATION)) {
                                arr = JSONArray.fromObject("[" + paramJson + "]");
                            } else {
                                arr = JSONArray.fromObject("[" + paramJson + "," + parameter.toString() + "]");
                            }
                            LOGGER.fine("JSONObject: arr :" + arr);
                        } else if (parameter instanceof JSONArray) {
                            LOGGER.fine("parameter is JSONArray");
                            arr = ((JSONArray) parameter);
                            JSONObject itemToDelete = null;
                            for (Object obj : arr) {
                                if (((JSONObject) obj).getString("name").equals(Constants.TARGET_CONFIGURATION)) {
                                    itemToDelete = (JSONObject) obj;
                                }
                            }
                            if (itemToDelete != null) {
                                arr.remove(itemToDelete);
                            }
                            arr.add(0, JSONObject.fromObject(paramJson));
                            LOGGER.fine("JSONObject: arr :" + arr);
                        }
                        parameterized.put("parameter", arr);
                    }

                } else {
                    String value = "{\"parameter\":[{\"name\":\"" + Constants.TARGET_CONFIGURATION + "\",\"choices\":\"" + targetConfigurations + "\",\"description\":\"\",\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"}]}";
                    paramDefProp.put("parameterized", JSONObject.fromObject(value));
                }
                properties.put("hudson-model-ParametersDefinitionProperty", paramDefProp);
                LOGGER.fine("JSONObject: properties after adding parameters :" + properties);
                req.bindJSON(req, properties);
            }
            super.doConfigSubmit(req, rsp);
        }

        // set assigned node
        String assignedNodeName = JsonUtils.getAssignedNode(jsonStr);
        Node n = LeroyUtils.findNodeByName(assignedNodeName);
        if (n != null) {
            setAssignedNode(n);
            LOGGER.fine("Assigned node :" + assignedNodeName);
        }
        save();
    }

}
