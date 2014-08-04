package org.jenkins.plugins.leroy;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.jenkins.plugins.leroy.util.Constants;
import org.jenkins.plugins.leroy.util.LeroyUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * Builder class for Leroy Configuration build step
 */
public class LeroyConfigurationBuilder extends AbstractLeroyBuilder {

    private String leroyNode;

    @DataBoundConstructor
    public LeroyConfigurationBuilder(String leroyNode) {
        this.leroyNode = leroyNode;
    }

    public String getLeroyNode() {
        return leroyNode;
    }

    public void setLeroyNode(String leroyNode) {
        this.leroyNode = leroyNode;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        PrintStream log = listener.getLogger();

        EnvVars envs = build.getEnvironment(listener);
        FilePath leroyHome = new FilePath(launcher.getChannel(), envs.get(Constants.LEROY_HOME));
        log.println(Constants.LEROY_HOME + ": " + leroyHome.getName());

        int returnCode = 0;

        // first copy files from workspace to LEROY_HOME folder
        build.getWorkspace().copyRecursiveTo(leroyHome);
        FilePath ws = build.getWorkspace();
        log.println("Files are copied from " + ws + " to " + leroyHome);

        // if LEROY_HOME/temp-generated_configs folder exists - delete it
        FilePath generatedConfigs = new FilePath(leroyHome, "temp-generated_configs");
        generatedConfigs.deleteRecursive();
        log.println("Old configs are deleted");

        // run "controller --generate-configs configurations.xml"
        returnCode = launcher.launch().envs(envs).pwd(leroyHome).cmds(leroyHome + "/controller", "--generate-configs", "configurations.xml").stdout(listener).join();
        if (returnCode == 0) {
            // archive
            generatedConfigs.zip(new FilePath(ws, "configurations.zip"));
            Map<String, String> files = LeroyUtils.listFiles(ws, "configurations.zip", "");
            build.getArtifactManager().archive(ws, launcher, listener, files);
            log.println("Configurations are copied to artifacts successfully!");
        }
        return returnCode == 0;
    }


    /**
     * Descriptor for {@link LeroyConfigurationBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
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


        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            if (LeroyConfigurationProject.class.isAssignableFrom(aClass)) {
                return true;
            }
            return false;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Configure Leroy"; //TODO externalize
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }
    }

}

