/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkins.plugins.leroy;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.scm.SCM;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.leroy.util.Constants;
import org.jenkins.plugins.leroy.util.LeroyBuildHelper;
import org.jenkins.plugins.leroy.util.LeroyUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yunus
 */
public class NewFreeStyleProject extends NewProject<NewFreeStyleProject,NewFreeStyleBuild> implements TopLevelItem {

    /**
     * @deprecated as of 1.390
     */
    public NewFreeStyleProject(Jenkins parent, String name) throws IOException {
        super(parent, name);
    }


    public NewFreeStyleProject(ItemGroup parent, String name) throws IOException {
        super(parent, name);
    }

    @Override
    protected Class<NewFreeStyleBuild> getBuildClass() {
        return NewFreeStyleBuild.class;
    }

    @Override
    public NewFreeStyleProject.DescriptorImpl getDescriptor() {
        return (NewFreeStyleProject.DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    @Override
    public String getBuildNowText() {
        return "Deploy";
    }

    /**
     * new doConfigSubmit to update build with parameters
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
        List<LeroyBuilder.Target> targets = LeroyBuildHelper.getTargets(jsonStr);

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
            targets.remove(defaultTarget);
            targets.add(0, defaultTarget);

            // targets -> build parameters
            List<String> buildParamsTargets = new ArrayList<String>(targets.size());
            for (LeroyBuilder.Target t : targets) {
                String param = Constants.ENVIRONMENT_PARAM +"=" + t.environment
                        + ", " + Constants.WORKFLOW_PARAM + "=" + t.workflow
                        + ", " + Constants.CONFIG_SOURCE_PARAM + "=" + t.configSource;
                buildParamsTargets.add(param);
            }

            // add parameters to request
            String targetConfigurations = StringUtils.join(buildParamsTargets, "\\n");
            String parameterkey = "{\"parameterized\":{\"parameter\":[{\"name\":\"" + Constants.TARGET_CONFIGURATION + "\",\"choices\":\"" + targetConfigurations + "\",\"description\":\"\",\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"}]}}";
            JSONObject properties = json.getJSONObject("properties");
//            properties.remove("hudson-model-ParametersDefinitionProperty");
//            properties.put("org-jenkins-plugins-leroy-LeroyParametersDefinitionProperty", JSONObject.fromObject(parameterkey));
            properties.put("hudson-model-ParametersDefinitionProperty", JSONObject.fromObject(parameterkey));
            req.bindJSON(req, properties);
            super.doConfigSubmit(req, rsp);
        }

        // set assigned node
        String assignedNodeName = LeroyBuildHelper.getAssignedNode(jsonStr);
        Node n = LeroyUtils.findNodeByName(assignedNodeName);
        if (n != null) {
            setAssignedNode(n);
        }
        save();
    }


    /**
     * get workflow and environment from scm
     *
     * @return
     */
    @JavaScriptMethod
    public boolean getWo()
            throws IOException, ServletException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            Launcher launcher = Hudson.getInstance().createLauncher(TaskListener.NULL);
            Writer writer = null;

            SCM scm = this.getScm();

            //check what if file doesn't exists
            FilePath checkoutdir = new FilePath(new File(Hudson.getInstance().getRootDir() + "/plugins/leroy/temp/"));
            // boolean check = true;
//                getProject().getScmCheckoutStrategy().preCheckout(.this, launcher, this.listener);
//                getProject().getScmCheckoutStrategy().checkout(this);
            File tempfile = new File(Hudson.getInstance().getRootDir() + "/plugins/leroy/temp/temp1.txt");
            File tempfile1 = new File(Hudson.getInstance().getRootDir() + "/plugins/leroy/temp/temp2.txt");

            AbstractBuild build = new NewFreeStyleBuild(this, tempfile);
            StreamBuildListener stream = new StreamBuildListener(new FileOutputStream(tempfile1));
            boolean check = this.checkout(build, launcher, stream, tempfile);

            return check;

        } catch (Exception e) {
            return false;
//                return FormValidation.error("Client error : "+e.getMessage());
        }
    }

    /**
     * Descriptor is instantiated as a field purely for backward compatibility.
     * Do not do this in your code. Put @Extension on your DescriptorImpl class instead.
     */
    @Restricted(NoExternalUse.class)
    @Extension(ordinal = 1000)
    public final static NewFreeStyleProject.DescriptorImpl DESCRIPTOR = new NewFreeStyleProject.DescriptorImpl();


    public static class DescriptorImpl extends AbstractProjectDescriptor {

        @Override
        public String getDisplayName() {
            return "Leroy Deployment Job";
        }

        @Override
        public NewFreeStyleProject newInstance(ItemGroup parent, String name) {
            try {
                return new NewFreeStyleProject(parent, name);
            } catch (IOException ex) {
                Logger.getLogger(NewFreeStyleProject.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

    }

}
