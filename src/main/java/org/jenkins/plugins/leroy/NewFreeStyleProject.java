/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkins.plugins.leroy;

import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.Hudson;
import hudson.model.ItemGroup;
import hudson.model.JobPropertyDescriptor;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.scm.SCM;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.bind.JavaScriptMethod;

/**
 *
 * @author Yunus
 */
public class NewFreeStyleProject extends NewProject<NewFreeStyleProject,NewFreeStyleBuild> implements TopLevelItem {
    
    
    /**
     * @deprecated as of 1.390
     */
    public NewFreeStyleProject(Jenkins parent, String name) throws IOException {
        super(parent, name);
        
//        String worflow = "Workflow";
//        String[] choices = new String[4];
//        choices[0] = "intialize";
//        choices[1] = "test";
//        choices[2] = "deploy";
//        choices[3] = "restart-services";
//       
//        String description ="";
//        ChoiceParameterDefinition test = new ChoiceParameterDefinition( worflow, choices,  description);
//        List<ParameterDefinition> paramsl = new ArrayList<ParameterDefinition>();
//        
//        String env = "Environment";
//        choices = new String[4];
//        choices[0] = "dev";
//        choices[1] = "test";
//        choices[2] = "stag";
//        choices[3] = "prod";
//        
//        description ="";
//        ChoiceParameterDefinition test1 = new ChoiceParameterDefinition( env, choices,  description);
//        
//        paramsl.add(test);
//        paramsl.add(test1);
//        
//        super.addProperty(new ParametersDefinitionProperty(paramsl));
//        
         
        //change image
       // String jenkinhome = Hudson.getInstance().getRootUrl() + "/war/images";
       // File jenkinsfile = new File(getIconPath());
       // Path from = jenkinsfile.toPath(); //convert from File to Path
       // Path to = Paths.get(jenkinhome); //convert from String to Path
        //Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        
         
         
       
 }
    

    public NewFreeStyleProject(ItemGroup parent, String name) throws IOException {
        super(parent, name);
        
//        String worflow = "Workflow";
//        String[] choices = new String[4];
//        choices[0] = "intialize";
//        choices[1] = "test";
//        choices[2] = "deploy";
//        choices[3] = "restart-services";
//       
//        String description ="";
//        ChoiceParameterDefinition test = new ChoiceParameterDefinition( worflow, choices,  description);
//        List<ParameterDefinition> paramsl = new ArrayList<ParameterDefinition>();
//        
//        String env = "Environment";
//        choices = new String[4];
//        choices[0] = "dev";
//        choices[1] = "test";
//        choices[2] = "stag";
//        choices[3] = "prod";
//        
//        description ="";
//        ChoiceParameterDefinition test1 = new ChoiceParameterDefinition( env, choices,  description);
//        
//        paramsl.add(test);
//        paramsl.add(test1);
//      
//        this.getProperty(ParametersDefinitionProperty.class);
//       
//        super.addProperty(new ParametersDefinitionProperty(paramsl));
//
//        //change image
//        String jenkinhome = Hudson.getInstance().getRootDir()+ "/war/images/jenkins.png";
//     //   jenkinhome = jenkinhome.replaceFirst("\\.", "");
//            Logger.getLogger(NewFreeStyleProject.class.getName()).log(Level.SEVERE, null, jenkinhome);
//            Logger.getLogger(NewFreeStyleProject.class.getName()).log(Level.SEVERE, null, getIconPath());
//           
//       
////        File jenkinsfile = new File(getIconPath());
////        
////        Path from = jenkinsfile.toPath(); //convert from File to Path
////        Path to = Paths.get(jenkinhome); //convert from String to Path
////        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
//       
    }
   @Override
    protected Class<NewFreeStyleBuild> getBuildClass() {
        return NewFreeStyleBuild.class;
    }

    public NewFreeStyleProject.DescriptorImpl getDescriptor() {
        return (NewFreeStyleProject.DescriptorImpl)Jenkins.getInstance().getDescriptorOrDie(getClass());
    }
    
    public String getIconPath() 
    {
        String path = Hudson.getInstance().getRootDir() + "/plugins/leroy/jenkins.png";
       // path = path.replaceFirst("\\.", "");
        //PluginWrapper wrapper = Hudson.getInstance().getPluginManager().getPlugin();
        return path;
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
                FilePath checkoutdir = new FilePath(new File(Hudson.getInstance().getRootDir()+"/plugins/leroy/temp/"));
               // boolean check = true;
//                getProject().getScmCheckoutStrategy().preCheckout(.this, launcher, this.listener);
//                getProject().getScmCheckoutStrategy().checkout(this);
                File tempfile = new File(Hudson.getInstance().getRootDir()+"/plugins/leroy/temp/temp1.txt");
                File tempfile1 = new File(Hudson.getInstance().getRootDir()+"/plugins/leroy/temp/temp2.txt");
                
                AbstractBuild build = new NewFreeStyleBuild(this,tempfile );
                StreamBuildListener stream = new StreamBuildListener(new FileOutputStream(tempfile1));         
               boolean check = this.checkout(build, launcher, stream, tempfile);
//                
//                try {
//                    writer = new BufferedWriter(new OutputStreamWriter(
//                          new FileOutputStream(leroyhome+"/agentdata.txt"), "utf-8"));
//                    if(launcher.isUnix())
//                        writer.write("1\n"+agentname+"\n");
//                       
//                    else
//                        writer.write("3\r\n"+agentname+"\r\n");
//                } catch (IOException ex) {
//                  return FormValidation.error("error creating file: "+ex.getMessage());
//                } finally {
//                   try {writer.close();} catch (Exception ex) {}
//                }
//              
//                int returnCode = 0;
//                EnvVars envs = new EnvVars(Functions.getEnvVars()); 
//                launcher.decorateByEnv(envs);
//
//                if(launcher.isUnix())
//                {                
//                    returnCode = launcher.launch().cmds("sh", Hudson.getInstance().getRootDir() + "/plugins/leroy/addagent.sh", leroyhome, agentname).pwd(leroyhome).stdout(output).join();
//                }
//                else
//                    returnCode = launcher.launch().envs(Functions.getEnvVars()).cmds(Hudson.getInstance().getRootDir() + "/plugins/leroy/addagent.bat" , leroyhome, leroyhome+"/controller.exe" ).join();
//                
//                int returnCode1 = 0;
//              
//                if(check)
//                {
//                    return FormValidation.ok("Success");
//                }
//                
//                return FormValidation.error("Failed to add agent" + output);
//          
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
    @Extension(ordinal=1000)
    public final static NewFreeStyleProject.DescriptorImpl DESCRIPTOR = new NewFreeStyleProject.DescriptorImpl();

    

    public static class DescriptorImpl extends AbstractProjectDescriptor  {
       
        public String getDisplayName() {
            return "Leroy Jenkins Job";
        }

        
        
        public NewFreeStyleProject newInstance(ItemGroup parent, String name) {
            try {
                return new NewFreeStyleProject(parent,name);
            } catch (IOException ex) {
                Logger.getLogger(NewFreeStyleProject.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

    }

}
