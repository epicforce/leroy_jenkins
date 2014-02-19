/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkins.plugins.leroy;

import hudson.Extension;
import hudson.Functions;
import hudson.model.AbstractProject;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.ItemGroup;
import hudson.model.JobPropertyDescriptor;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.TopLevelItem;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 *
 * @author Yunus
 */
public class LeroyConfigurationProject extends ConfigurationProject<LeroyConfigurationProject,LeroyConfigurationBuild> implements TopLevelItem {
   
    
    /**
     * @deprecated as of 1.390
     */
    public LeroyConfigurationProject(Jenkins parent, String name) throws IOException {
        super(parent, name);
//        
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
//         
//        //change image
//       // String jenkinhome = Hudson.getInstance().getRootUrl() + "/war/images";
//       // File jenkinsfile = new File(getIconPath());
//       // Path from = jenkinsfile.toPath(); //convert from File to Path
//       // Path to = Paths.get(jenkinhome); //convert from String to Path
//        //Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
//        
//         
//         
       
 }
    

    public LeroyConfigurationProject(ItemGroup parent, String name) throws IOException {
          super(parent, name);
          //Functions.getEnvVs();
          
//          String scmtype = (String) Functions.getEnvVars().get("SCM_TYPE");
//          this.setScm(null);
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
//        File jenkinsfile = new File(getIconPath());
//        
//        Path from = jenkinsfile.toPath(); //convert from File to Path
//        Path to = Paths.get(jenkinhome); //convert from String to Path
//        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
//       
    }
   @Override
    protected Class<LeroyConfigurationBuild> getBuildClass() {
        return LeroyConfigurationBuild.class;
    }

    public LeroyConfigurationProject.DescriptorImpl getDescriptor() {
        return (LeroyConfigurationProject.DescriptorImpl)Jenkins.getInstance().getDescriptorOrDie(getClass());
    }
    
    public String getIconPath() 
    {
        String path = Hudson.getInstance().getRootDir() + "/plugins/leroy/jenkins.png";
       // path = path.replaceFirst("\\.", "");
        //PluginWrapper wrapper = Hudson.getInstance().getPluginManager().getPlugin();
        return path;
    }
   
   
    
     /**
     * Descriptor is instantiated as a field purely for backward compatibility.
     * Do not do this in your code. Put @Extension on your DescriptorImpl class instead.
     */
    @Restricted(NoExternalUse.class)
    @Extension(ordinal=1000)
    public final static LeroyConfigurationProject.DescriptorImpl DESCRIPTOR = new LeroyConfigurationProject.DescriptorImpl();


    public static class DescriptorImpl extends AbstractProject.AbstractProjectDescriptor  {
       
        public String getDisplayName() {
            return "Leroy Configuration Job";
        }

        public LeroyConfigurationProject newInstance(ItemGroup parent, String name) {
            try {
                return new LeroyConfigurationProject(parent,name);
            } catch (IOException ex) {
                Logger.getLogger(LeroyConfigurationProject.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

    }

}
