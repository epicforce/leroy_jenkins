/*
 * The MIT License
 * 
 * Copyright (c) 2004-2011, Sun Microsystems, Inc., Kohsuke Kawaguchi,
 * Jorg Heymans, Stephen Connolly, Tom Huybrechts
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkins.plugins.leroy;

import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.DependencyGraph;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Items;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.ResourceActivity;
import hudson.model.SCMedItem;
import hudson.model.Saveable;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.plugins.copyartifact.BuildSelector;
import hudson.plugins.copyartifact.CopyArtifact;
import hudson.plugins.copyartifact.StatusBuildSelector;
import hudson.scm.SCM;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrappers;
import hudson.tasks.Builder;
import hudson.tasks.Fingerprinter;
import hudson.tasks.Publisher;
import hudson.tasks.Maven;
import hudson.tasks.Maven.ProjectWithMaven;
import hudson.tasks.Maven.MavenInstallation;
import hudson.triggers.Trigger;
import hudson.util.DescribableList;
import hudson.util.ListBoxModel;
import java.io.File;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jenkins.plugins.leroy.util.XMLParser;
import org.kohsuke.stapler.bind.JavaScriptMethod;

/**
 * Buildable software project.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class NewProject<P extends NewProject<P,B>,B extends NewBuild<P,B>>
     extends AbstractProject<P,B>  implements SCMedItem, Saveable, ProjectWithMaven, BuildableItemWithBuildWrappers {

    /**
     * List of active {@link Builder}s configured for this project.
     */
    private volatile DescribableList<Builder,Descriptor<Builder>> builders;
    private static final AtomicReferenceFieldUpdater<NewProject,DescribableList> buildersSetter
            = AtomicReferenceFieldUpdater.newUpdater(NewProject.class,DescribableList.class,"builders");

    /**
     * List of active {@link Publisher}s configured for this project.
     */
    private volatile DescribableList<Publisher,Descriptor<Publisher>> publishers;
    private static final AtomicReferenceFieldUpdater<NewProject,DescribableList> publishersSetter
            = AtomicReferenceFieldUpdater.newUpdater(NewProject.class,DescribableList.class,"publishers");

    /**
     * List of active {@link BuildWrapper}s configured for this project.
     */
    private volatile DescribableList<BuildWrapper,Descriptor<BuildWrapper>> buildWrappers;
    private static final AtomicReferenceFieldUpdater<NewProject,DescribableList> buildWrappersSetter
            = AtomicReferenceFieldUpdater.newUpdater(NewProject.class,DescribableList.class,"buildWrappers");

    /**
     * Creates a new project.
     */
    public NewProject(ItemGroup parent,String name) throws IOException {
        super(parent,name);
        
    }

   
    public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
        super.onLoad((ItemGroup)parent, name);
        getBuildersList().setOwner(this);
        getPublishersList().setOwner(this);
        getBuildWrappersList().setOwner(this);
    }

    public AbstractProject<?, ?> asProject() {
        return this;
    }

    public List<Builder> getBuilders() {
        LeroyBuilder  a = new LeroyBuilder("","","");
        
        CopyArtifact copyartifact = null;
        try {
             copyartifact = new CopyArtifact("", "", new StatusBuildSelector(true), "", LeroyBuilder.getLeroyhome()+"/artifacts/",false, false, true);
        } catch (InterruptedException ex) {
            Logger.getLogger(NewProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(NewProject.class.getName()).log(Level.SEVERE, null, ex);
        }
        List<Builder> temp =  getBuildersList().toList();
        List<Builder> temp1 =  new ArrayList<Builder>();
       
        ListIterator<Builder> ite = temp.listIterator();
        boolean check = false;
        boolean hasCopyartifact = false;
        while(ite.hasNext())
        {
            Builder ele = ite.next();
                temp1.add(ele);
            
            if(ele instanceof LeroyBuilder)
                check = true;
            if(ele instanceof CopyArtifact)
                hasCopyartifact = true;
        }
        
        if(!hasCopyartifact)
            temp1.add((Builder)copyartifact);
        
        if(!check)
            temp1.add((Builder)a);
        
        return temp1;
        
    }

    /**
     * @deprecated as of 1.463
     *      We will be soon removing the restriction that only one instance of publisher is allowed per type.
     *      Use {@link #getPublishersList()} instead.
     */
    public Map<Descriptor<Publisher>,Publisher> getPublishers() {
        return getPublishersList().toMap();
    }

    public DescribableList<Builder,Descriptor<Builder>> getBuildersList() {
        if (builders == null) {
            buildersSetter.compareAndSet(this,null,new DescribableList<Builder,Descriptor<Builder>>(this));
        }
        return builders;
    }
    
    public DescribableList<Publisher,Descriptor<Publisher>> getPublishersList() {
        if (publishers == null) {
            publishersSetter.compareAndSet(this,null,new DescribableList<Publisher,Descriptor<Publisher>>(this));
        }
        return publishers;
    }

    public Map<Descriptor<BuildWrapper>,BuildWrapper> getBuildWrappers() {
        return getBuildWrappersList().toMap();
    }

    public DescribableList<BuildWrapper, Descriptor<BuildWrapper>> getBuildWrappersList() {
        if (buildWrappers == null) {
            buildWrappersSetter.compareAndSet(this,null,new DescribableList<BuildWrapper,Descriptor<BuildWrapper>>(this));
        }
        return buildWrappers;
    }

    @Override
    protected Set<ResourceActivity> getResourceActivities() {
        final Set<ResourceActivity> activities = new HashSet<ResourceActivity>();

        activities.addAll(super.getResourceActivities());
        activities.addAll(Util.filter(getBuildersList(),ResourceActivity.class));
        activities.addAll(Util.filter(getPublishersList(),ResourceActivity.class));
        activities.addAll(Util.filter(getBuildWrappersList(),ResourceActivity.class));

        return activities;
    }

    /**
     * Adds a new {@link BuildStep} to this {@link Project} and saves the configuration.
     *
     * @deprecated as of 1.290
     *      Use {@code getPublishersList().add(x)}
     */
    public void addPublisher(Publisher buildStep) throws IOException {
        getPublishersList().add(buildStep);
    }

    /**
     * Removes a publisher from this project, if it's active.
     *
     * @deprecated as of 1.290
     *      Use {@code getPublishersList().remove(x)}
     */
    public void removePublisher(Descriptor<Publisher> descriptor) throws IOException {
        getPublishersList().remove(descriptor);
    }

    public Publisher getPublisher(Descriptor<Publisher> descriptor) {
        for (Publisher p : getPublishersList()) {
            if(p.getDescriptor()==descriptor)
                return p;
        }
        return null;
    }

    protected void buildDependencyGraph(DependencyGraph graph) {
        getPublishersList().buildDependencyGraph(this,graph);
        getBuildersList().buildDependencyGraph(this,graph);
        getBuildWrappersList().buildDependencyGraph(this,graph);
    }

    @Override
    public boolean isFingerprintConfigured() {
        return getPublishersList().get(Fingerprinter.class)!=null;
    }

    public MavenInstallation inferMavenInstallation() {
        Maven m = getBuildersList().get(Maven.class);
        if (m!=null)    return m.getMaven();
        return null;
    }

//
//
// actions
//
//
    @Override
    protected void submit( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException, FormException {
        super.submit(req,rsp);
  
        JSONObject json = req.getSubmittedForm();
        
        getBuildWrappersList().rebuild(req,json, BuildWrappers.getFor(this));
        getBuildersList().rebuildHetero(req,json, Builder.all(), "builder");
        getPublishersList().rebuildHetero(req, json, Publisher.all(), "publisher");
    }

    @Override
    protected List<Action> createTransientActions() {
        List<Action> r = super.createTransientActions();

        for (BuildStep step : getBuildersList())
            r.addAll(step.getProjectActions(this));
        for (BuildStep step : getPublishersList())
            r.addAll(step.getProjectActions(this));
        for (BuildWrapper step : getBuildWrappers().values())
            r.addAll(step.getProjectActions(this));
        for (Trigger trigger : triggers())
            r.addAll(trigger.getProjectActions());

        return r;
    }
    
    @Override
    public void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException, FormException {
        
       
        String worflow = "Workflow";
        
        List<String> choiceslist = getWorkflowItems();
        String[] choices = new String[choiceslist.size()];
        choiceslist.toArray(choices);
        
       
        String description ="";
        ChoiceParameterDefinition test = new ChoiceParameterDefinition( worflow, choices,  description);
        List<ParameterDefinition> paramsl = new ArrayList<ParameterDefinition>();
        
        String env = "Environment";
        choiceslist = getEnvrnItems();
        choices = new String[choiceslist.size()];
        choiceslist.toArray(choices);
       
        description ="";
        ChoiceParameterDefinition test1 = new ChoiceParameterDefinition( env, choices,  description);
        
        paramsl.add(test);
        paramsl.add(test1);
      
        this.getProperty(ParametersDefinitionProperty.class);
       
        super.addProperty(new ParametersDefinitionProperty(paramsl));
        save();
        super.doConfigSubmit(req,rsp);
    
//        updateTransientActions();
//
//        Set<AbstractProject> upstream = Collections.emptySet();
//        if(req.getParameter("pseudoUpstreamTrigger")!=null) {
//            upstream = new HashSet<AbstractProject>(Items.fromNameList(getParent(),req.getParameter("upstreamProjects"),AbstractProject.class));
//        }
//
//        convertUpstreamBuildTrigger(upstream);
//
//        // notify the queue as the project might be now tied to different node
        Jenkins.getInstance().getQueue().scheduleMaintenance();
//
//        // this is to reflect the upstream build adjustments done above
        Jenkins.getInstance().rebuildDependencyGraphAsync();
    }
    
     public List<String> getEnvrnItems() {
            List<String> items = new ArrayList<String>();
            
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
                items.add(envs);
            }
            
            
            return items;
            
        }
        
        public List<String> getWorkflowItems() {
            List<String> items = new ArrayList<String>();
            
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
                    if (file.isDirectory()) {
                       
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
                items.add(role);
            }
            return items;
        }
      
    /**
         * get workflow and environment from scm 
         * 
         * @return 
         */
//        @JavaScriptMethod 
//        public boolean getWo() 
//                throws IOException, ServletException {
//                 ByteArrayOutputStream output = new ByteArrayOutputStream();
//               
//            try {
//                Launcher launcher = Hudson.getInstance().createLauncher(TaskListener.NULL);
//                Writer writer = null;
//
//                SCM scm = this.getScm();
//                
//                //check what if file doesn't exists
//                FilePath checkoutdir = new FilePath(new File(Hudson.getInstance().getRootDir()+"/plugins/leroy/temp/"));
//                 boolean check = scm.checkout(new NewFreeStyleBuild(this), launcher, checkoutdir, null, null);
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
//                 return check;
                
//            } catch (Exception e) {
//                return false;
////                return FormValidation.error("Client error : "+e.getMessage());
//            }
//            
//        }
}
