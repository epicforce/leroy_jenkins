package org.jenkins.plugins.leroy;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.Hudson;
import hudson.model.JobPropertyDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import jenkins.model.Jenkins;
import jnr.constants.Constant;
import net.sf.json.JSONObject;
import org.jenkins.plugins.leroy.util.Constants;
import org.jenkins.plugins.leroy.util.LeroyUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import static java.nio.file.StandardCopyOption.*;

import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link LeroyConfigurationBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 */
public class LeroyConfigurationBuilder extends Builder {

    
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public LeroyConfigurationBuilder() {
       
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        // configuration jobs should be run on Leroy nodes only
        if (!LeroyUtils.isLeroyNode()) {
            listener.fatalError("Current node is not a Leroy Node");
            return false;
        }

        EnvVars envs = build.getEnvironment(listener);
        FilePath projectRoot = build.getWorkspace();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        String leroyhome = envs.get(Constants.LEROY_HOME);
        listener.getLogger().println(Constants.LEROY_HOME + ": " + leroyhome);
        
        
        int returnCode;
       
        if(launcher.isUnix())
        {   
                returnCode = launcher.launch().envs(envs).cmds("sh", Hudson.getInstance().getRootDir() + "/plugins/leroy/configuration.sh", leroyhome, projectRoot.toURI().getPath() ).pwd(projectRoot).stdout(output).join();
                listener.getLogger().println(output.toString().trim());
        }
        else
        { 
            String templeroyhome = leroyhome;
            if(leroyhome.charAt(leroyhome.length()-1)=='/' ||leroyhome.charAt(leroyhome.length()-1)=='\\')
                templeroyhome = leroyhome.substring(0, leroyhome.length()-1);    
            
            returnCode = launcher.launch().cmds(Hudson.getInstance().getRootDir() + "/plugins/leroy/configuration.bat", templeroyhome, projectRoot.toURI().getPath()).pwd(projectRoot).stdout(output).join();
            listener.getLogger().println(output.toString().trim());
        }    
          
        return returnCode==0;
      }
        
        
    

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
    
    
    
    /**
     * Descriptor for {@link LeroyBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
         public DescriptorImpl() {
             load();
            
        }
        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckLeroyhome(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please provide a path for leroy plugin");
            return FormValidation.ok();
        }
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Configure Leroy";
        }
        
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req,formData);
        }

    }

}

