package org.jenkins.plugins.leroy;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.copyartifact.BuildSelector;
import hudson.plugins.copyartifact.CopyArtifact;
import hudson.plugins.copyartifact.StatusBuildSelector;
import hudson.slaves.NodeProperty;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.jenkins.plugins.leroy.jaxb.beans.EnvironmentBean;
import org.jenkins.plugins.leroy.util.Constants;
import org.jenkins.plugins.leroy.util.JsonUtils;
import org.jenkins.plugins.leroy.util.LeroyUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * <p>
 * Leroy builder to perform deploy step
 * </p>
 *
 * @author Yunus Dawji
 * @author Dzmitry Bahdanovich
 */
public class LeroyBuilder extends AbstractLeroyBuilder {

    private static final Logger LOGGER = Logger.getLogger(LeroyBuilder.class.getName());

    private String projectname;

    private List<Target> targets;

    private String leroyNode;

    private List<String> workflows; // from SCM

    private boolean useLastBuildWithSameTarget;

    @DataBoundConstructor
    public LeroyBuilder(String projectname, List<Target> targets, String leroyNode, boolean useLastBuildWithSameTarget) {
        this.projectname = projectname;
        this.targets = targets;
        this.leroyNode = leroyNode;
        this.useLastBuildWithSameTarget = useLastBuildWithSameTarget;
    }

    public String getProjectname() {
        return projectname;
    }

    public String getLeroyNode() {
        return leroyNode;
    }

    public List<Target> getTargets() {
        return targets;
    }

    public void setWorkflows(List<String> workflows) {
        this.workflows = workflows;
    }

    public boolean isUseLastBuildWithSameTarget() {
        return useLastBuildWithSameTarget;
    }

    public void setUseLastBuildWithSameTarget(boolean useLastBuildWithSameTarget) {
        this.useLastBuildWithSameTarget = useLastBuildWithSameTarget;
    }

    /**
     * This method is used to find Leroy-specific build parameters aamong all build parameters
     * @param b is a build
     * @return the list of Leroy parameters
     */
    private List<ParameterValue> findLeroyParameterValues(AbstractBuild b) {
        ParametersAction paramAction = b.getAction(ParametersAction.class);
        List<ParameterValue> values = new ArrayList<ParameterValue>();
        for (ParameterValue v : paramAction.getParameters()) {
            if (v instanceof LeroyPasswordParameterValue || v instanceof LeroyStringParameterValue) {
                values.add(v);
            }
        }
        return values;
    }

    /**
     * This will create $WORKSPACE/leroy/artifacts/configurations.zip
     * and requires the existence of at least 1 file in $WORKSPACE/leroy/resources/
     *
     * $LEROY_HOME/controller --leroy-configuration-root=$WORKSPACE/leroy --generate-configurations --workflow=configurations.xml
     */
    private boolean configure(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        PrintStream log = listener.getLogger();

        EnvVars envs = build.getEnvironment(listener);
        FilePath leroyHome = new FilePath(launcher.getChannel(), envs.get(Constants.LEROY_HOME));
        log.println(Constants.LEROY_HOME + ": " + leroyHome.getName());

        int returnCode = 0;

        // $WORKSPACE/leroy/resources/ should contain at least 1 file
        FilePath resFolder = build.getWorkspace().child("leroy/resources/");
        if (resFolder.exists() && resFolder.isDirectory()) {
            List<FilePath> children = resFolder.list();
            if (children == null || children.size() == 0 ) {
                listener.error("Folder '{0}' is empty", resFolder);
                return false;
            }
        }

        // run "controller --generate-configs configurations.xml"
        returnCode = launcher.launch().envs(envs).pwd(leroyHome)
                .cmds(leroyHome + "/controller",
                        "--leroy-configuration-root",
                        "$WORKSPACE/leroy",
                        "--generate-configurations",
                        "--workflow=configurations.xml"
                        ).stdout(listener).join();
        if (returnCode != 0) {
            listener.error("Cannot create configurations; error code = {0}", returnCode);
            return false;
        }

        log.println("Configurations are created successfully");
        return true;
    }

    private boolean deploy(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        EnvVars envs = build.getEnvironment(listener);
        PrintStream log = listener.getLogger();

        // get build target
        String targetParam = envs.get(Constants.TARGET_CONFIGURATION);
        final Target target = JsonUtils.getTargetFromBuildParameter(targetParam);
        Constants.ConfigSource configSource = Constants.ConfigSource.valueOf(target.configSource);

        FilePath ws = build.getWorkspace();
        FilePath leroyHome = new FilePath(launcher.getChannel(), envs.get(Constants.LEROY_HOME));
        log.println("LEROY_HOME: " + leroyHome);

        int returnCode = 0;

        // clear up configs from LEROY HOME
        new FilePath(leroyHome, "commands").deleteRecursive();
        new FilePath(leroyHome, "workflows").deleteRecursive();
        new FilePath(leroyHome, "properties").deleteRecursive();
        new FilePath(leroyHome, "resources").deleteRecursive();

        // if we take configurations from the last build then we need to "prepare" LEROY_HOME
        if (configSource == Constants.ConfigSource.LAST_SUCCESS) {
            // first remove "control files" from workspace
            new FilePath(ws, "commands").deleteRecursive();
            new FilePath(ws, "workflows").deleteRecursive();
            new FilePath(ws, "properties").deleteRecursive();
            new FilePath(ws, "resources").deleteRecursive();
            new FilePath(ws, "environments.xml").delete();
            log.println("Remove old config files from workspace - success!");

            // now copy artifact from LAST BUILD to workspace
            // we have 2 possible source builds here: last stable and last stable with the same "target" (workflow/environment combination)
            CopyArtifact copyFromBuildToWks = null;
            if (useLastBuildWithSameTarget) {
                copyFromBuildToWks = new CopyArtifact(build.getProject().getName(), "", new BuildSelector() {
                    @Override
                    protected boolean isSelectable(Run<?, ?> run, EnvVars env) {
                        String buildname = target.environment + "_" + target.workflow;
                        if (run.getResult().isBetterOrEqualTo(Result.UNSTABLE) && buildname.equals(run.getDisplayName())) {
                            return true;
                        }
                        return false;
                    }
                }, "", ws.getRemote(), false, false, true);
            } else {
                // or copy artifacts from the latest stable build
                copyFromBuildToWks = new CopyArtifact(build.getProject().getName(), "", new StatusBuildSelector(true), "", ws.getRemote(), false, false, true);
            }
            boolean success = copyFromBuildToWks.perform(build, launcher, listener);
            if (!success) {
                return false;
            }
            log.println("Copy configs from last build to workspace - success!");
        }

        // copy artifacts from workspace to LEROY HOME
        ws.copyRecursiveTo(leroyHome);
        log.println("Copy files from " + ws + " to " + leroyHome + " - success!");

        // deploy
        List<String> cmds = new ArrayList<String>();
        cmds.add(leroyHome + "/controller");
        cmds.add("--workflow");
        cmds.add(target.workflow);
        cmds.add("--environment");
        cmds.add(target.environment);
        List<ParameterValue> leroyParameValues = findLeroyParameterValues(build);
        for (ParameterValue leroyParamValue : leroyParameValues) {
            cmds.add("--add-global-property");
            String key = leroyParamValue.getName();
            String value = (String) build.getBuildVariables().get(key);
            cmds.add(key + "=" + value);
        }

        returnCode = launcher.launch().pwd(leroyHome).envs(envs).cmds(cmds).stdout(listener).join();
        if (returnCode != 0) {
            return false;
        }
        log.println("Deploy - success!");

        // archive configurations
        Map<String, String> files = LeroyUtils.listFiles(leroyHome, "commands/**,workflows/**,properties/**,environments/**,*.xml,*.key,*.pem,*.crt", "");
        build.getArtifactManager().archive(leroyHome, launcher, listener, files);
        log.println("Archive artifacts - success!");
        return returnCode == 0;
    }

    /**
     * This method performs the deployment via leroy.
     * @param build
     * @param launcher
     * @param listener
     * @return true if succeeded
     * @throws IOException
     * @throws InterruptedException
     * @throws OutOfMemoryError
     */
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, OutOfMemoryError {
        EnvVars envs = build.getEnvironment(listener);
        PrintStream log = listener.getLogger();

        // get build target
        String targetParam = envs.get(Constants.TARGET_CONFIGURATION);
        final Target target = JsonUtils.getTargetFromBuildParameter(targetParam);
        Constants.ConfigSource configSource = Constants.ConfigSource.valueOf(target.configSource);

        FilePath ws = build.getWorkspace();
        FilePath leroyHome = new FilePath(launcher.getChannel(), envs.get(Constants.LEROY_HOME));
        log.println("LEROY_HOME: " + leroyHome);

        int returnCode = 0;

        // clear up configs from LEROY HOME
        new FilePath(leroyHome, "commands").deleteRecursive();
        new FilePath(leroyHome, "workflows").deleteRecursive();
        new FilePath(leroyHome, "properties").deleteRecursive();
        new FilePath(leroyHome, "resources").deleteRecursive();

        // if we take configurations from the last build then we need to "prepare" LEROY_HOME
        if (configSource == Constants.ConfigSource.LAST_SUCCESS) {
            // first remove "control files" from workspace
            new FilePath(ws, "commands").deleteRecursive();
            new FilePath(ws, "workflows").deleteRecursive();
            new FilePath(ws, "properties").deleteRecursive();
            new FilePath(ws, "resources").deleteRecursive();
            new FilePath(ws, "environments.xml").delete();
            log.println("Remove old config files from workspace - success!");

            // now copy artifact from LAST BUILD to workspace
            // we have 2 possible source builds here: last stable and last stable with the same "target" (workflow/environment combination)
            CopyArtifact copyFromBuildToWks = null;
            if (useLastBuildWithSameTarget) {
                copyFromBuildToWks = new CopyArtifact(build.getProject().getName(), "", new BuildSelector() {
                    @Override
                    protected boolean isSelectable(Run<?, ?> run, EnvVars env) {
                        String buildname = target.environment + "_" + target.workflow;
                        if (run.getResult().isBetterOrEqualTo(Result.UNSTABLE) && buildname.equals(run.getDisplayName())) {
                            return true;
                        }
                        return false;
                    }
                }, "", ws.getRemote(), false, false, true);
            } else {
                // or copy artifacts from the latest stable build
                copyFromBuildToWks = new CopyArtifact(build.getProject().getName(), "", new StatusBuildSelector(true), "", ws.getRemote(), false, false, true);
            }
            boolean success = copyFromBuildToWks.perform(build, launcher, listener);
            if (!success) {
                return false;
            }
            log.println("Copy configs from last build to workspace - success!");
        }

        // copy artifacts from workspace to LEROY HOME
        ws.copyRecursiveTo(leroyHome);
        log.println("Copy files from " + ws + " to " + leroyHome + " - success!");

        // deploy
        List<String> cmds = new ArrayList<String>();
        cmds.add(leroyHome + "/controller");
        cmds.add("--workflow");
        cmds.add(target.workflow);
        cmds.add("--environment");
        cmds.add(target.environment);
        List<ParameterValue> leroyParameValues = findLeroyParameterValues(build);
        for (ParameterValue leroyParamValue : leroyParameValues) {
            cmds.add("--add-global-property");
            String key = leroyParamValue.getName();
            String value = (String) build.getBuildVariables().get(key);
            cmds.add(key + "=" + value);
        }

        returnCode = launcher.launch().pwd(leroyHome).envs(envs).cmds(cmds).stdout(listener).join();
        if (returnCode != 0) {
            return false;
        }
        log.println("Deploy - success!");

        // archive configurations
        Map<String, String> files = LeroyUtils.listFiles(leroyHome, "commands/**,workflows/**,properties/**,environments/**,*.xml,*.key,*.pem,*.crt", "");
        build.getArtifactManager().archive(leroyHome, launcher, listener, files);
        log.println("Archive artifacts - success!");
        return returnCode == 0;
    }

    /**
     * @return the descriptor of Leroy Build Step
     */
    @Override
    public DescriptorImpl getDescriptor() {
        DescriptorImpl descr = (DescriptorImpl) super.getDescriptor();
        descr.setLeroyNode(leroyNode);
        descr.setWorkflows(workflows);
        return descr;
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private List<String> workflows;
        private String leroyNode;

        public DescriptorImpl() {
            load();
        }

        public void setLeroyNode(String leroyNode) {
            this.leroyNode = leroyNode;
        }

        public void setWorkflows(List<String> workflows) {
            this.workflows = workflows;
        }

        public ListBoxModel doFillConfigSourceItems() {
            ListBoxModel items = new ListBoxModel();
            items.add(Constants.ConfigSource.SCM.getValue(), Constants.ConfigSource.SCM.name());
            items.add(Constants.ConfigSource.LAST_SUCCESS.getValue(), Constants.ConfigSource.LAST_SUCCESS.name());
            return items;
        }

        public ListBoxModel doFillEnvironmentItems() {
            ListBoxModel items = new ListBoxModel();
            Node node = LeroyUtils.findNodeByName(leroyNode);
            List<EnvironmentBean> envs = new ArrayList<EnvironmentBean>();
            if (node != null) {
                for (NodeProperty<?> nodeProperty : node.getNodeProperties()) {
                    if (nodeProperty instanceof LeroyNodeProperty) {
                        envs = ((LeroyNodeProperty) nodeProperty).getEnvironments();
                    }
                }
            }
            if (CollectionUtils.isEmpty(envs)) {
                LOGGER.severe("No environments found for node '" + leroyNode + "'");
            }
            for (EnvironmentBean env : envs) {
                items.add(env.getName(), env.getName());
            }
            return items;
        }

        public ListBoxModel doFillLeroyNodeItems() {
            ListBoxModel items = new ListBoxModel();
            try {
                List<Computer> leroyNodes = LeroyUtils.getLeroyNodes();
                if (!CollectionUtils.isEmpty(leroyNodes)) {
                    for (Computer comp : leroyNodes) {
                        // handle master node separately
                        if (comp instanceof Hudson.MasterComputer) {
                            items.add(Constants.MASTER_NODE, Constants.MASTER_NODE);
                        } else {
                            items.add(comp.getName(), comp.getName());
                        }
                    }
                }
            } catch (Exception e) {
                // omit; //TODO handle
            }
            return items;
        }

        /**
         * Get the list of workflows and fill workflow select list in Leroy Build step
         * Called from main.jelly
         *
         * @return
         */
        public ListBoxModel doFillWorkflowItems() {
            ListBoxModel items = new ListBoxModel();
            if (!CollectionUtils.isEmpty(workflows)) {
                for (String wf : workflows) {
                    items.add(wf, wf);
                }
            }
            return items;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Leroy"; //TODO externalize
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }
    }

    public static class Target {
        public String environment;
        public String workflow;
        public String configSource;
        public boolean autoDeploy;

        @DataBoundConstructor
        public Target(String environment, String workflow, String configSource, boolean autoDeploy) {
            this.environment = environment;
            this.workflow = workflow;
            this.configSource = configSource;
            this.autoDeploy = autoDeploy;
        }

        public Target() {
        }

        @Override
        public String toString() {
            return "Target{" +
                    "environment='" + environment + '\'' +
                    ", workflow='" + workflow + '\'' +
                    ", configSource='" + configSource + '\'' +
                    ", autoDeploy=" + autoDeploy +
                    '}';
        }
    }

}

