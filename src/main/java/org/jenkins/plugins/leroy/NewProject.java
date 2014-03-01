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
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.DependencyGraph;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.ResourceActivity;
import hudson.model.SCMedItem;
import hudson.model.Saveable;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
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
import java.io.FileOutputStream;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.jenkins.plugins.leroy.util.XMLParser;

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

    
    private List<String> workflow;
    
    private List<String> environment;
    
    /**
     * Creates a new project.
     */
    public NewProject(ItemGroup parent,String name) throws IOException {
        super(parent,name);
        
    }

    @Override
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
        LeroyBuilder  a = new LeroyBuilder("","","",this.getName());
        
        CopyArtifact copyartifact = null;
        try {
            String leroybuilderpath = LeroyBuilder.getLeroyhome();
           
            if(leroybuilderpath.charAt(leroybuilderpath.length()-1)=='/'||leroybuilderpath.charAt(leroybuilderpath.length()-1)=='\\')
                leroybuilderpath = leroybuilderpath + "artifacts/";
            else
                leroybuilderpath = leroybuilderpath + "/artifacts/";
             copyartifact = new CopyArtifact("", "", new StatusBuildSelector(true), "", leroybuilderpath,false, false, true);
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
   
        JSONObject json = req.getSubmittedForm();

        super.submit(req,rsp);
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
    /**
     * new doConfigSubmit to update build with parameters
     * @param req
     * @param rsp
     * @throws IOException
     * @throws ServletException
     * @throws hudson.model.Descriptor.FormException 
     * 
     */
    @Override
    public void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException, FormException {
        
        JSONObject json = req.getSubmittedForm();
        
        JSONObject properties = json.getJSONObject("properties");
       
        properties.remove("hudson-model-ParametersDefinitionProperty");
        String worflow = "Workflow";
        
        doFillEnvrnItems();
        doFillWorkflowItems();
        List<String> choiceslist;
        if(workflow!=null)
            choiceslist  = workflow;
        else
            choiceslist = getWorkflowItems();
        String[] choices = new String[choiceslist.size()];
        choiceslist.toArray(choices);
        
        //choices to string
        Iterator<String> ite = choiceslist.iterator();
        String tempworkflow ="";
        while(ite.hasNext())
        {
            String tempp = ite.next();          
            tempworkflow = tempworkflow + tempp + "\\n";
        }
        
        if(tempworkflow.length()>2)
            tempworkflow = tempworkflow.substring(0,tempworkflow.length()-2);
       
        String description ="";
        ChoiceParameterDefinition test = new ChoiceParameterDefinition( worflow, choices,  description);
        List<ParameterDefinition> paramsl = new ArrayList<ParameterDefinition>();
        
        String env = "Environment";
        if(environment!=null)
            choiceslist  = environment;
        else
            choiceslist = getEnvrnItems();
        
        choices = new String[choiceslist.size()];
        choiceslist.toArray(choices);
        //choices to string
        ite = choiceslist.iterator();
        String tempenv ="";
        while(ite.hasNext())
        {
            String tempp = ite.next();
            tempenv = tempenv + tempp + "\\n";
        }
        
        if(tempenv.length()>2)
            tempenv = tempenv.substring(0,tempenv.length()-2);
       
        description ="";
        ChoiceParameterDefinition test1 = new ChoiceParameterDefinition( env, choices,  description);
        
        paramsl.add(test);
        paramsl.add(test1);
        
        //this is a hack(need to figureout a better a way)(IMP!!!)
        String parameterkey = "{\"parameterized\":{\"parameter\":[{\"name\":\"Workflow\",\"choices\":\""+tempworkflow+"\",\"description\":\"\",\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"},{\"name\":\"Environment\",\"choices\":\""+tempenv+"\",\"description\":\"\",\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"}]}}";
       
        //Gson gson = new Gson();
        properties.put("hudson-model-ParametersDefinitionProperty",JSONObject.fromObject(parameterkey));
        
        req.bindJSON(req,properties);
        
        super.addProperty(new ParametersDefinitionProperty(paramsl));
        
        save();
        super.doConfigSubmit(req,rsp);
    

        // notify the queue as the project might be now tied to different node
        Jenkins.getInstance().getQueue().scheduleMaintenance();
        // this is to reflect the upstream build adjustments done above
        Jenkins.getInstance().rebuildDependencyGraphAsync();
    }
    
     public List<String> getEnvrnItems() {
            List<String> items = new ArrayList<String>();
            
            String envspath = "";
            
            try {
                envspath = LeroyBuilder.getLeroyhome() + "/environments.xml";          
                List<String> envsroles = XMLParser.getEnvironment(new File(envspath));

                for (String envs : envsroles) {
                    items.add(envs);
                }

            } catch (InterruptedException ex) {
                Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return items;
            
        }
        
        public List<String> getWorkflowItems() {
            List<String> items = new ArrayList<String>();
            
            String workflowpath = "";
            
            try {
                workflowpath = LeroyBuilder.getLeroyhome()+"/workflows/";
            } 
            catch (InterruptedException ex) {
                Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, ex);
            } 
            catch (IOException ex) {
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
                    if (file.isDirectory()&& !(file.isHidden()) && file.getName().charAt(0)=='.') {
                       
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
        public ListBoxModel doFillEnvrnItems() {
            ListBoxModel listitems = new ListBoxModel();
            List<String> items = new ArrayList<String>();
            
            String envspath = "";
            
            envspath = Hudson.getInstance().getRootDir()+"/plugins/leroy/temp1/environments.xml";          
            List<String> envsroles = XMLParser.getEnvironment(new File(envspath));

            if(envsroles!=null){
                for (String envs : envsroles) {
                    items.add(envs);
                    listitems.add(envs,envs);
                }
            }
            environment = items;
            return listitems;
            
        }
        
        public ListBoxModel doFillWorkflowItems() {
            ListBoxModel listitems = new ListBoxModel();
            List<String> items = new ArrayList<String>();
            
            String workflowpath = Hudson.getInstance().getRootDir()+"/plugins/leroy/temp1/workflows/";
            
            //get file names
            List<String> results = new ArrayList<String>();
            File[] files = new File(workflowpath).listFiles();
            
            
            if(files!=null && files.length > 0)
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
                items.add(role);
                listitems.add(role,role);
            }
            //listitems.addAll(items);
            return listitems;
        }
        
        /**
         * get workflow and environment from scm 
         * 
         * @return 
         */
        public boolean doWo() 
        {                
            try {
                Launcher launcher = Hudson.getInstance().createLauncher(TaskListener.NULL);
                Writer writer = null;

                SCM scm = this.getScm();              
                //check what if file doesn't exists
                FilePath checkoutdir = new FilePath(new File(Hudson.getInstance().getRootDir()+"/plugins/leroy/temp1/"));
               
                String uuid = UUID.randomUUID().toString();
                String uuid1 = UUID.randomUUID().toString();
                
                
                //check if temp folder exists
                File tempfolder = new File(Hudson.getInstance().getRootDir()+"/plugins/leroy/temp/");
                
                if(!tempfolder.exists())
                {
                    tempfolder.mkdir();                
                }
                
                
                File tempfile = new File(Hudson.getInstance().getRootDir()+"/plugins/leroy/temp/"+uuid1+".txt");
                File tempfile1 = new File(Hudson.getInstance().getRootDir()+"/plugins/leroy/temp/"+uuid+".txt");
                
                String name = this.getName();
                
                StreamBuildListener stream = new StreamBuildListener(new FileOutputStream(tempfile));         
                boolean check=false;         
                try
                {
                    
                    check = scm.checkout(getLastBuild(), launcher,checkoutdir ,stream, tempfile1);
                }
                catch(IOException e)
                {
                    System.out.print(e.toString());
                    e.printStackTrace();
                    Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, e);

                }
                catch(InterruptedException e)
                {
                    System.out.print(e.toString());
                    e.printStackTrace();
                    Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, e);

                }
                catch(Exception e)
                {
                    System.out.printf(e.toString()); 
                    e.printStackTrace();
                    Logger.getLogger(LeroyBuilder.class.getName()).log(Level.SEVERE, null, e);

                }      
                doFillWorkflowItems();
                doFillEnvrnItems();
                        
                return check;
                
            } 
            catch (Exception e) {               
                e.printStackTrace();
                return false;
            }
            
        }
}
