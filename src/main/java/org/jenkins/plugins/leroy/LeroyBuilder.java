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
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.JobPropertyDescriptor;
import hudson.model.TaskListener;
import hudson.plugins.copyartifact.CopyArtifact;
import hudson.plugins.copyartifact.StatusBuildSelector;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ListBoxModel;
import java.io.BufferedWriter;
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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jenkins.plugins.leroy.util.XMLParser;
import org.kohsuke.stapler.bind.JavaScriptMethod;

/**
 * <p>
 * Leory builder to perform deploy step 
 * </p>
 * @author Yunus Dawji
 */
public class LeroyBuilder extends Builder {

    private  String envrn;
    
    private String workflow;
    
    private String projectname;
    
    private String checkoutstrategy;
    
    private List<String> envrnlist;
    
    private List<String> workflowlist;
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public LeroyBuilder(String leroyhome, String envrn, String workflow, String projectname, String checkoutstrategy) {
        this.envrn = envrn;
        this.workflow = workflow;
        this.projectname = projectname;
        this.checkoutstrategy = checkoutstrategy;
        
    }

    
    /**
     * Get LEROY_HOME
     */
    public static String  getLeroyhome() throws InterruptedException, IOException {
        Jenkins jenkins = Jenkins.getInstance();
        Computer[] computers = jenkins.getComputers();
        
        for(int i = 0; i < computers.length; i++)
        {
             EnvVars envs = computers[i].buildEnvironment(TaskListener.NULL); 
             if(envs.containsKey("IS_LEROY_NODE"))
             {
                 return envs.get("LEROY_HOME");
             }
        }
        
        return null;
    }
    
    /**
     * Get Workflow
     * @return 
     */
    public String getWorkflow() {
        return workflow;
    }
   
    public String getCheckoutstrategy() {
        return checkoutstrategy;
    }
    /**
     * Get Environment
     * @return 
     */
    public String getEnvrn() {
        return envrn;
    }
  
    public String getProjectname() {
        return projectname;
    }
   
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, OutOfMemoryError {
        List<JobPropertyDescriptor> jobPropertyDescriptors = Functions.getJobPropertyDescriptors(NewProject.class);
        EnvVars envs = build.getEnvironment(listener);
        FilePath projectRoot = build.getWorkspace();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        String leroypath = envs.expand(this.getLeroyhome());
        String envrn = envs.get("Environment");
        String workflow = envs.get("Workflow");
        
        
        listener.getLogger().println("LEROY_HOME: " + leroypath);
        
        int returnCode;
     
        if(launcher.isUnix())
        {   
            returnCode = launcher.launch().envs(envs).cmds(Hudson.getInstance().getRootDir() + "/plugins/leroy/preflightcheck.sh", leroypath , workflow, envrn).stdout(output).pwd(projectRoot).join();
            listener.getLogger().println(output.toString().trim());
            if(getCheckoutstrategy()=="scm") {
                if(returnCode==0){
                    returnCode = launcher.launch().envs(envs).cmds("cp" ,"-fR",".", leroypath).stdout(output).pwd(projectRoot).join();
                    listener.getLogger().println(output.toString().trim());

                    if(returnCode==0){
                        returnCode = launcher.launch().envs(envs).cmds("sh", Hudson.getInstance().getRootDir() + "/plugins/leroy/deploy.sh", leroypath ,workflow, envrn).stdout(listener.getLogger()).pwd(projectRoot).join();
                        listener.getLogger().println(output.toString().trim());
                    }
                }
            }
            else{
                CopyArtifact copyartifact = null;
                String workspacepath = projectRoot.toURI().getPath()+"/temp_artifacts";
                   
                try {
                    String leroybuilderpath = LeroyBuilder.getLeroyhome();
                     
                    File tempfolder = new File(workspacepath);

                    if(!tempfolder.exists()){
                         tempfolder.mkdir();                
                    }
                    else{
                        delete(tempfolder);
                        tempfolder.mkdir();  
                    }
                 
                    copyartifact = new CopyArtifact(build.getProject().getName(), "", new StatusBuildSelector(true), "", workspacepath,false, false, true);
                
                } catch (InterruptedException ex) {
                    Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                copyartifact.perform(build, launcher, listener);
                returnCode = launcher.launch().envs(envs).cmds("cp" ,"-fR",workspacepath, leroypath).stdout(output).pwd(projectRoot).join();
                listener.getLogger().println(output.toString().trim());
                
                if(returnCode==0){
                    returnCode = launcher.launch().envs(envs).cmds("sh", Hudson.getInstance().getRootDir() + "/plugins/leroy/deploy.sh", leroypath ,workflow, envrn).stdout(listener.getLogger()).pwd(projectRoot).join();
                    listener.getLogger().println(output.toString().trim());
                }
            }            
        }
        else
        { 
            returnCode = launcher.launch().envs(envs).cmds(Hudson.getInstance().getRootDir() + "/plugins/leroy/preflightcheck.bat", leroypath , workflow, envrn).stdout(output).pwd(projectRoot).join();
            listener.getLogger().println(output.toString().trim());
            
            if(getCheckoutstrategy()=="scm") {
                 if(returnCode==0){
                    returnCode = launcher.launch().envs(envs).cmds(Hudson.getInstance().getRootDir() + "/plugins/leroy/deploy.bat", ".", workflow, envrn, leroypath).stdout(output).pwd(projectRoot).join();
                    listener.getLogger().println(output.toString().trim());
                }
            }
            else {
                CopyArtifact copyartifact = null;
                String workspacepath = projectRoot.toURI().getPath().substring(1)+"/temp_artifacts";
                   
                try {
                    String leroybuilderpath = LeroyBuilder.getLeroyhome();
                     
                    File tempfolder = new File(workspacepath);

                    if(!tempfolder.exists()){
                         tempfolder.mkdir();                
                    }
                    else{
                        delete(tempfolder);
                        tempfolder.mkdir();  
                    }
                 
                    copyartifact = new CopyArtifact(build.getProject().getName(), "", new StatusBuildSelector(true), "", workspacepath,false, false, true);
                
                } catch (InterruptedException ex) {
                    Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                copyartifact.perform(build, launcher, listener);
                
                try{
                //perform deploy
                    returnCode = launcher.launch().envs(envs).cmds(Hudson.getInstance().getRootDir() + "/plugins/leroy/deploy.bat", workspacepath, workflow, envrn, leroypath).stdout(output).pwd(projectRoot).join();
                    listener.getLogger().println(output.toString().trim());
                }catch(OutOfMemoryError e){
                    throw e;
                }
                
                
            }
        }
      
        return returnCode==0;
    }
    public static void delete(File file)
    	throws IOException{
 
    	if(file.isDirectory()){
 
    		//directory is empty, then delete it
    		if(file.list().length==0){
 
    		   file.delete();
    		   System.out.println("Directory is deleted : " 
                                                 + file.getAbsolutePath());
 
    		}else{
 
    		   //list all the directory contents
        	   String files[] = file.list();
 
        	   for (String temp : files) {
        	      //construct the file structure
        	      File fileDelete = new File(file, temp);
 
        	      //recursive delete
        	     delete(fileDelete);
        	   }
 
        	   //check the directory again, if empty then delete it
        	   if(file.list().length==0){
           	     file.delete();
        	     System.out.println("Directory is deleted : " 
                                                  + file.getAbsolutePath());
        	   }
    		}
 
    	}else{
    		//if file, then delete it
    		file.delete();
    		System.out.println("File is deleted : " + file.getAbsolutePath());
    	}
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
				
				System.out.println("Adding file: " + files[i].getName());

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
        
        
        public ListBoxModel doFillEnvrnItems() {
            ListBoxModel items = new ListBoxModel();
            
            String envspath = "";
            
            try {
                envspath = LeroyBuilder.getLeroyhome() + "/environments.xml";
            } catch (InterruptedException ex) {
                Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            List<String> envsroles = XMLParser.getEnvironment(new File(envspath));
            
            for (String envs : envsroles) {
                items.add(envs,envs);
            }
            
            
            return items;
            
        }
        
        public ListBoxModel doFillCheckoutstrategyItems(){
            ListBoxModel items = new ListBoxModel();
            
            items.add("SCM", "scm");
            items.add("Last Build", "lastbuild");
            
            return items;
        }
        
        public ListBoxModel doFillWorkflowItems() {
            ListBoxModel items = new ListBoxModel();
            
            String workflowpath = "";
            
            try {
                workflowpath = LeroyBuilder.getLeroyhome()+"/workflows/";
            } catch (InterruptedException ex) {
                Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //get file names
            List<String> results = new ArrayList<String>();
            File[] files = new File(workflowpath).listFiles();

            if(files.length > 0)
            {
                for (File file : files) {
                    if (file.isFile() && file.getName().contains(".xml")) {
                        results.add(file.getName().substring(0, file.getName().length()-4));
                    }
                    if (file.isDirectory() && !(file.isHidden()) && file.getName().charAt(0)=='.') {
                       
                        File[] files1 = new File(workflowpath).listFiles();
                        if(files.length > 0)
                        {
                            for (File file1 : files1) {
                                if (file1.isFile() && file1.getName().contains(".xml")) {
                                    results.add(file.getName()+"/"+file1.getName().substring(0, file1.getName().length()-4));
                                }
                            }
                        }
                    }
                }
            }

            for (String role : results) {
                items.add(role,role);
            }
            return items;
        }
      
       
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Leroy";
        }
        
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();            
            return super.configure(req,formData);
        }

    }
}

