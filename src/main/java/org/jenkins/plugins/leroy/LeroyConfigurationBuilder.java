package org.jenkins.plugins.leroy;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
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
import net.sf.json.JSONObject;
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
 * and a new {@link LeroyBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class LeroyConfigurationBuilder extends Builder {

    private  String leroyhome;
    
    private  String envrn;
    
    private String workflow;
    
    private static String parameters = "test,test1";
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public LeroyConfigurationBuilder(String leroyhome, String envrn, String workflow) {
        this.leroyhome = leroyhome;
        this.envrn = envrn;
        this.workflow = workflow;
        parameters = "test";
        parameters = parameters + "test1";
    }

    public static String getParameters() {
        return parameters;
    }
    /**
     * Get LEROY_HOME
     */
    public String getLeroyhome() {
        return leroyhome;
    }
    /**
     * Get Workflow
     * @return 
     */
    public String getWorkflow() {
        return workflow;
    }
    /**
     * Get Environment
     * @return 
     */
    public String getEnvrn() {
        return envrn;
    }
    
    public String getParameter(){
            String temp;
            return "test,test1";
    }

    
    
    
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        List<JobPropertyDescriptor> jobPropertyDescriptors = Functions.getJobPropertyDescriptors(NewProject.class);
        EnvVars envs = build.getEnvironment(listener);
        FilePath projectRoot = build.getWorkspace();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        String leroypath = envs.expand(this.leroyhome);
        String envrn = envs.get("environment");
        String workflow = envs.get("workflow");
        
        parameters = "test";
        parameters = parameters + "test1";
        
        
        this.workflow = workflow;
        this.envrn = envrn;
        
        listener.getLogger().println("LEROY_HOME: "+leroyhome);
        
        int returnCode = launcher.launch().envs(envs).cmds("xcopy" ,".", leroypath, "/E", "/R" ,"/Y").stdout(output).pwd(projectRoot).join();
        listener.getLogger().println(output.toString().trim());
        
     //   if(returnCode==0)
     //   {
         //   returnCode = launcher.launch().envs(envs).cmds("rm","-rf", leroypath+"/.git").stdout(output).pwd(projectRoot).join();
     //   }
        
        if(returnCode==0)
        {
            returnCode = launcher.launch().envs(envs).cmds(leroypath+"/controller.exe","--workflow", workflow,"--environment", envrn).stdout(listener.getLogger()).pwd(leroypath).join();
          //  listener.getLogger().println(output.toString().trim());
        }
        
        if(returnCode==0)
        {
            listener.getLogger().println("Building Artifacts");         
            Date currenttime = new Date();
            String build_number = envs.get("BUILD_NUMBER");
            String date = "";
            String month = "";
            
            if(currenttime.getDate() < 10)
            {
                date = "0" + currenttime.getDate();
            }
            else
            {
                date = ""+currenttime.getDate();
            }
            if(currenttime.getMonth()< 10)
            {
                month = "0" + currenttime.getMonth();
            }
            else
            {
                month = ""+currenttime.getMonth();
            }
            
            String zipFile = projectRoot.toString()+"\\zip\\" + build_number + "_" + workflow + "_" + envrn+ "_"
                    + date +month+currenttime.getYear()+" "+currenttime.getHours()+
                    "_"+currenttime.getMinutes()+"_"+currenttime.getSeconds()+".zip";
		
		String srcDir = projectRoot.toString();
		     projectRoot.zip(new FilePath(new File(zipFile)));

            listener.getLogger().println("Copying Artifacts");         
            
            returnCode = launcher.launch().envs(envs).cmds("xcopy" ,".\\zip", leroypath+"\\artifacts", "/E", "/R" ,"/Y").stdout(output).pwd(projectRoot).join();
            listener.getLogger().println(output.toString().trim());
            
            
                    
            //returnCode = launcher.launch().envs(envs).cmds(leroypath+"/controller.exe","--workflow", workflow,"--environment", envrn).stdout(listener.getLogger()).pwd(leroypath).join();
        }
        
        return returnCode==0;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
    
    
    
    private static void addDirToArchive(ZipOutputStream zos, File srcFile) {

		File[] files = srcFile.listFiles();

		System.out.println("Adding directory: " + srcFile.getName());

		for (int i = 0; i < files.length; i++) {
			
			// if the file is directory, use recursion
			if (files[i].isDirectory()) {
				addDirToArchive(zos, files[i]);
				continue;
			}

			try {
				
				System.out.println("tAdding file: " + files[i].getName());

				// create byte buffer
				byte[] buffer = new byte[1024];

				FileInputStream fis = new FileInputStream(files[i]);

				zos.putNextEntry(new ZipEntry(files[i].getName()));
				
				int length;

				while ((length = fis.read(buffer)) > 0) {
					zos.write(buffer, 0, length);
				}

				zos.closeEntry();

				// close the InputStream
				fis.close();

			} catch (IOException ioe) {
				System.out.println("IOException :" + ioe);
			}
			
		}

	}
    /**
     * Descriptor for {@link LeroyBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
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
        /**
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckWorkflow(@QueryParameter String value)
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

