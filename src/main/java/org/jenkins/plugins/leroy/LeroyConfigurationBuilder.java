package org.jenkins.plugins.leroy;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.jenkins.plugins.leroy.util.Constants;
import org.jenkins.plugins.leroy.util.LeroyUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

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
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        PrintStream log = listener.getLogger();

        // configuration jobs should be run on Leroy nodes only
        if (!LeroyUtils.isLeroyNode()) {
            listener.fatalError("Current node is not a Leroy Node");
            return false;
        }

        EnvVars envs = build.getEnvironment(listener);
        String leroyhome = envs.get(Constants.LEROY_HOME);
        log.println(Constants.LEROY_HOME + ": " + leroyhome);

        int returnCode = 0;

        // first copy files from workspace to LEROY_HOME folder
        File workspaceFile = new File( build.getWorkspace().toURI().getPath());
        File leroyHomeFile = new File(leroyhome);
        File generatedConfigsFile = new File(leroyhome + "/temp-generated_configs/");
        LeroyUtils.copyDirectoryQuietly(workspaceFile, leroyHomeFile);
        log.println("Files are copied from " + workspaceFile.toString() + " to " + leroyHomeFile.toString());

        // if LEROY_HOME/temp-generated_configs folder exists - delete it
        FileUtils.deleteDirectory(generatedConfigsFile);
        log.println("Old configs are deleted");

        // run "controller --generate-configs configurations.xml"
        returnCode = launcher.launch().envs(envs).pwd(leroyHomeFile).cmds(leroyHomeFile.getAbsolutePath() + "/controller", "--generate-configs", "configurations.xml").stdout(listener).join();;
        if (returnCode == 0) {
            // archive
            Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.ZIP);
//            archiver.create("configurations", workspaceFile, generatedConfigsFile);
//            log.println("Configurations are copied to workspace successfully!");
            archiver.create("configurations", build.getArtifactsDir(), generatedConfigsFile);
            log.println("Configurations are copied to artifacts successfully!");
        }
        return returnCode == 0;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }


    /**
     * Descriptor for {@link LeroyBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
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

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckLeroyhome(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please provide a path for leroy plugin");
            }
            return FormValidation.ok();
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

