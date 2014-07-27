package org.jenkins.plugins.leroy;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.copyartifact.BuildSelector;
import hudson.plugins.copyartifact.CopyArtifact;
import hudson.plugins.copyartifact.StatusBuildSelector;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jenkins.plugins.leroy.util.Constants;
import org.jenkins.plugins.leroy.util.JsonUtils;
import org.jenkins.plugins.leroy.util.LeroyUtils;
import org.jenkins.plugins.leroy.util.XMLParser;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.*;
import java.util.logging.Level;
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

    private String projectname;

    private List<Target> targets;

    private String leroyNode;

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

    public boolean isUseLastBuildWithSameTarget() {
        return useLastBuildWithSameTarget;
    }

    public void setUseLastBuildWithSameTarget(boolean useLastBuildWithSameTarget) {
        this.useLastBuildWithSameTarget = useLastBuildWithSameTarget;
    }


    private List<ParameterValue> findLeroyParameterValues(AbstractBuild b) {
        ParametersAction paramAction = b.getAction(ParametersAction.class);
        List<ParameterValue> values = new ArrayList<ParameterValue>();
        for (ParameterValue v : paramAction.getParameters()) {
            if (v instanceof LeroyPasswordParameterValue || v instanceof LeroyStringParameterValue ) {
                values.add(v);
            }
        }
        return values;
    }


    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, OutOfMemoryError {
        EnvVars envs = build.getEnvironment(listener);
        PrintStream log = listener.getLogger();

        // get build target
        String targetParam = envs.get(Constants.TARGET_CONFIGURATION);
        final Target target = JsonUtils.getTargetFromBuildParameter(targetParam);
        Constants.ConfigSource configSource = Constants.ConfigSource.valueOf(target.configSource);

        String leroyHome = envs.expand(LeroyUtils.getLeroyHome(launcher));
        log.println("LEROY_HOME: " + leroyHome);

        File workspaceFile = new File( build.getWorkspace().toURI().getPath());
        File leroyHomeFile = new File(leroyHome);

        int returnCode = 0;

        // clear up configs from LEROY HOME
        FileUtils.deleteDirectory(new File(leroyHomeFile, "/commands"));
        FileUtils.deleteDirectory(new File(leroyHomeFile, "/workflows"));
        FileUtils.deleteDirectory(new File(leroyHomeFile, "/properties"));
        FileUtils.deleteDirectory(new File(leroyHomeFile, "/resources"));

        // if we take configurations from the last build then we need to "prepare" LEROY_HOME
        if (configSource == Constants.ConfigSource.LAST_BUILD) {
            // first remove "control files" from workspace
            FileUtils.deleteDirectory(new File(workspaceFile, "/commands"));
            FileUtils.deleteDirectory(new File(workspaceFile, "/workflows"));
            FileUtils.deleteDirectory(new File(workspaceFile, "/properties"));
            FileUtils.deleteDirectory(new File(workspaceFile, "/resources"));
            FileUtils.deleteQuietly(new File(workspaceFile, "/environments.xml"));

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
                }, "", workspaceFile.getAbsolutePath(), false, false, true);
            } else {
                // or copy artifacts from the latest stable build
                copyFromBuildToWks = new CopyArtifact(build.getProject().getName(), "", new StatusBuildSelector(true), "", workspaceFile.getAbsolutePath(), false, false, true);
            }
            boolean success = copyFromBuildToWks.perform(build, launcher, listener);
            if (!success) {
                return false;
            }
            log.println("Copy configs from last build to workspace - success!");
        }

        // copy artifacts from workspace to LEROY HOME
        LeroyUtils.copyDirectoryQuietly(workspaceFile, leroyHomeFile);
        log.println("Copy files from " + workspaceFile.toString() + " to " + leroyHomeFile.toString() + " - success!");

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
            String value = (String)build.getBuildVariables().get(key);
            cmds.add(key + "=" + value);
        }

        returnCode = launcher.launch().pwd(leroyHomeFile).envs(envs).cmds(cmds).stdout(listener).join();
        if (returnCode != 0) {
            return false;
        }
        log.println("Deploy - success!");

        // archive configurations
        File archiveFile = build.getArtifactsDir();
        LeroyUtils.copyDirectoryQuietly(new File(leroyHomeFile, "commands"), new File(archiveFile, "commands"));
        LeroyUtils.copyDirectoryQuietly(new File(leroyHomeFile, "workflows"), new File(archiveFile, "workflows"));
        LeroyUtils.copyDirectoryQuietly(new File(leroyHomeFile, "properties"), new File(archiveFile, "properties"));
        LeroyUtils.copyDirectoryQuietly(new File(leroyHomeFile, "environments"), new File(archiveFile, "environments"));
        File[] filesToCopy = leroyHomeFile.listFiles(new FilenameFilter() {
            private String[] extensions = new String[]{".xml",".key", ".pem", ".crt"};
            @Override
            public boolean accept(File dir, String name) {
                String lowerName = name.toLowerCase();
                for (String ext : extensions) {
                    if (lowerName.endsWith(ext)) {
                        return true;
                    }
                }
                return false;
            }
        });
        for (File file : filesToCopy) {
            LeroyUtils.copyFileToDirectoryQuietly(file, archiveFile);
        }
        log.println("Archive artifacts - success!");
        return returnCode == 0;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }

        public ListBoxModel doFillConfigSourceItems() {
            ListBoxModel items = new ListBoxModel();
            items.add(Constants.ConfigSource.SCM.getValue(), Constants.ConfigSource.SCM.name());
            items.add(Constants.ConfigSource.LAST_BUILD.getValue(), Constants.ConfigSource.LAST_BUILD.name());
            return items;
        }

        public ListBoxModel doFillEnvironmentItems() {
            ListBoxModel items = new ListBoxModel();
            try {
                String envspath = LeroyUtils.getEnvironmentsXml();
                List<String> envsroles = XMLParser.getEnvironment(new File(envspath));
                for (String envs : envsroles) {
                    items.add(envs, envs);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }
            return items;
        }

        public ListBoxModel doFillLeroyNodeItems() {
            ListBoxModel items = new ListBoxModel();
            try {
                List<Computer> leroyNodes = LeroyUtils.getLeroyNodes();
                for (Computer comp : leroyNodes) {
                    // handle master node separately
                    if (comp instanceof Hudson.MasterComputer) {
                        items.add(Constants.MASTER_NODE, Constants.MASTER_NODE);
                    } else {
                        items.add(comp.getName(), comp.getName());
                    }
                }
            } catch (Exception e) {
                // omit; //TODO handle
            }
            return items;
        }

        public ListBoxModel doFillWorkflowItems() {
        ListBoxModel items = new ListBoxModel();
            try {
                String workflowPath = LeroyUtils.getWorkflowsFolder();
                //get file names
                IOFileFilter workflowFileFilter = new AbstractFileFilter() {
                    @Override
                    public boolean accept(File file) {
                        try {
                            if (LeroyUtils.isWorkflow(file)) {
                                return true;
                            }
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                        return false;
                    }
                };
                Iterator<File> fileIterator = FileUtils.iterateFiles(new File(workflowPath), workflowFileFilter, TrueFileFilter.INSTANCE);
                if (fileIterator != null) {
                    URI workFlowsBase = new File(workflowPath).toURI();
                    while (fileIterator.hasNext()) {
                        // get relative path using workflow folder as a base and remove extension
                        File wf = fileIterator.next();
                        String relative = workFlowsBase.relativize(wf.toURI()).getPath();
                        String woExtension = relative.substring(0, relative.lastIndexOf('.'));
                        items.add(woExtension, woExtension);
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }

            return items;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            if (NewFreeStyleProject.class.isAssignableFrom(aClass)) {
                return true;
            }
            return false;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Leroy"; //TODO externalize
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }
    }

    public static class Target{
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
    }

}

