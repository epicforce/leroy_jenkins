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

import com.ctc.wstx.util.StringUtil;
import com.jayway.jsonpath.JsonPath;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.model.Descriptor.FormException;
import hudson.plugins.copyartifact.CopyArtifact;
import hudson.plugins.copyartifact.StatusBuildSelector;
import hudson.scm.SCM;
import hudson.tasks.ArtifactArchiver;
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

import net.minidev.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jaxen.jdom.JDOMXPath;
import org.jenkins.plugins.leroy.beans.*;
import org.jenkins.plugins.leroy.beans.Environment;
import org.jenkins.plugins.leroy.util.ConfigurationHelper;
import org.jenkins.plugins.leroy.util.Constants;
import org.jenkins.plugins.leroy.util.LeroyBuildHelper;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.kohsuke.stapler.QueryParameter;

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
    
    private Map<String, String> envrnStratergyMap;
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
        LeroyBuilder  a = new LeroyBuilder("","","",this.getName(),"scm");
        
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
        
        ListIterator<Publisher> ite = publishers.listIterator();
        boolean check = false;
  
        while(ite.hasNext())
        {
            Publisher ele = ite.next();
            
            
            if(ele instanceof ArtifactArchiver)
                check = true;

            
        }

        if(!check) {
            LeroyArtifactArchiver artifactArchiver = new LeroyArtifactArchiver("*.xml,*.key,*.pem,*.crt,commands/**,workflows/**,properties/**,environments/**","",false);
            publishers.add((Publisher)artifactArchiver);
        }
        
        return publishers;
    }

    public List<Publisher> getVisiblePublishersList() {
        ListIterator<Publisher> it = getPublishersList().listIterator();
        List<Publisher> visiblePublishers = new ArrayList<Publisher>();
        while (it.hasNext()) {
            Publisher publisher = it.next();
            if (!(publisher instanceof Hidden)) {
                visiblePublishers.add(publisher);
            }
        }
        return visiblePublishers;
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

    private void updateConfig(String json) throws IOException {
        String configFilename = Hudson.getInstance().getRootDir()+"/plugins/leroy/configuration/"+getName()+".json";
        ConfigurationHelper config = new ConfigurationHelper(configFilename);

        List<String> enabledEnvs = LeroyBuildHelper.getEnabledEnvironments(json);
        config.setEnabledEnvironments(enabledEnvs);
        config.setDefaultEnvironment(LeroyBuildHelper.getDefaultEnvironment(json));
        config.setAutodeployEnvironments(LeroyBuildHelper.getAutodeployEnvironments(json));
        config.setDefaultWorkflow(LeroyBuildHelper.getDefaultWorkflow(json));
        config.putUsedConfigs(LeroyBuildHelper.getEnvUsedConfigs(json));
        config.flush();
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
        updateConfig(json.toString());

        String configFilename = Hudson.getInstance().getRootDir()+"/plugins/leroy/configuration/"+getName()+".json";
        ConfigurationHelper config = new ConfigurationHelper(configFilename);

//        if (config.getDefaultEnabledAutodeploy() == null) {
            // is not eligible to autodeploy - form parameters list

            // get default env and put it on the top of the list of enabled environments
            List<Environment> enabledEnvs = config.getEnabledEnvironments();
            Environment defEnv = config.getDefaultEnvironment();
            if (defEnv != null && enabledEnvs.contains(defEnv)) {
                enabledEnvs.remove(defEnv);
                enabledEnvs.add(0,defEnv);
            }

            // get default workflow and put it on the top of the list of workflows
            List<Workflow> wfs = config.getWorkflows();
            Workflow defWf = config.getDefaultWorkflow();
            if (defWf != null && wfs.contains(defWf)) {
                wfs.remove(defWf);
                wfs.add(0, defWf);
            }

            // create choises to pass it to JSON
            List<String> envNames = new ArrayList<String>();
            for (Environment env : enabledEnvs) {
                envNames.add(env.getName());
            }
            String envsChoises = StringUtils.join(envNames, "\\n");
            List<String> wfNames = new ArrayList<String>();
            for (Workflow wf : wfs) {
                wfNames.add(wf.getName());
            }
            String wfsChoises = StringUtils.join(wfNames, "\\n");

            // add to request
            String parameterkey = "{\"parameterized\":{\"parameter\":[{\"name\":\"Workflow\",\"choices\":\""+wfsChoises+"\",\"description\":\"\",\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"},{\"name\":\"Environment\",\"choices\":\""+envsChoises+"\",\"description\":\"\",\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"}]}}";
            JSONObject properties = json.getJSONObject("properties");
            properties.put("hudson-model-ParametersDefinitionProperty",JSONObject.fromObject(parameterkey));
            req.bindJSON(req,properties);

//        }

        save();
        super.doConfigSubmit(req,rsp);

        //set node
        Jenkins jenkins = Jenkins.getInstance();
        Computer[] computers = jenkins.getComputers();

        for(int i = 0; i < computers.length; i++)
        {
            EnvVars envs = null;
            try {
                envs = computers[i].buildEnvironment(TaskListener.NULL);
                String name = computers[i].getName();
                if(envs.containsKey(Constants.IS_LEROY_NODE))
                {
                    setAssignedNode(computers[i].getNode());
                }

            } catch (InterruptedException ex) {
                Logger.getLogger(NewProject.class.getName()).log(Level.SEVERE, null, ex);
            }



        }

        // notify the queue as the project might be now tied to different node
        Jenkins.getInstance().getQueue().scheduleMaintenance();
        // this is to reflect the upstream build adjustments done above
        Jenkins.getInstance().rebuildDependencyGraphAsync();

    }

    /*
    @Override
    public void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException, FormException {

        JSONObject json = req.getSubmittedForm();

        updateConfig(json.toString());

        JSONObject properties = json.getJSONObject("properties");
        // builder.hsa envtablediv[0].envtable enabled_envs[]
//        properties.remove("hudson-model-ParametersDefinitionProperty");
        String worflow = "Workflow";

        getName()

        doFillEnvrnItems();
        doFillWorkflowItems();
        List<String> choiceslist;
        if(workflow!=null) {
            choiceslist  = workflow;
        } else {
            choiceslist = getWorkflowItems();
        }
        String[] choices = new String[choiceslist.size()];
        choiceslist.toArray(choices);
        
        //choices to string
        String tempworkflow =StringUtils.join(choiceslist, "\\n");

        String description ="";
        ChoiceParameterDefinition test=null;
        if(choices.length>0)      
              test  = new ChoiceParameterDefinition( worflow, choices,  description);
        else
            tempworkflow="None";
        
//        List<ParameterDefinition> paramsl = new ArrayList<ParameterDefinition>();
        
        String env = "Environment";
        if(environment!=null)
            choiceslist  = environment;
        else
            choiceslist = getEnvrnItems();
        
        choices = new String[choiceslist.size()];
        choiceslist.toArray(choices);
        
        //choices to string
//        String tempenv = StringUtils.join(choiceslist, "\\n");
        String tempenv = StringUtils.join(LeroyBuildHelper.getEnabledEnvironments(json.toString()), "\\n");

//        ChoiceParameterDefinition test1=null;
//        if(choices.length>0)
//            test1 = new ChoiceParameterDefinition( env, choices,  description);
//        else
//            tempenv="None";
        if (choices.length <= 0) {
            tempenv="None";
        }
            
//        if(test!=null)
//            paramsl.add(test);
//
//        if(test1!=null)
//            paramsl.add(test1);
        
        //this is a hack(need to figureout a better a way)(IMP!!!)

        String parameterkey = "{\"parameterized\":{\"parameter\":[{\"name\":\"Workflow\",\"choices\":\""+tempworkflow+"\",\"description\":\"\",\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"},{\"name\":\"Environment\",\"choices\":\""+tempenv+"\",\"description\":\"\",\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"}]}}";
//        String parameterkey = "{\"parameterized\":{\"parameter\":[{\"name\":\"Workflow\",\"choices\":\""+tempworkflow+"\",\"defaultValue\":\"internal_test\",\"description\":\"\",\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"},{\"name\":\"Environment\",\"choices\":\""+tempenv+"\",\"description\":\"\",\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"}]}}";
        properties.put("hudson-model-ParametersDefinitionProperty",JSONObject.fromObject(parameterkey));
        
        req.bindJSON(req,properties);
        
        save();
        super.doConfigSubmit(req,rsp);
        
        //set node
        Jenkins jenkins = Jenkins.getInstance();
        Computer[] computers = jenkins.getComputers();

        for(int i = 0; i < computers.length; i++)
        {
            EnvVars envs = null; 
            try {
                envs = computers[i].buildEnvironment(TaskListener.NULL);
                String name = computers[i].getName();
                if(envs.containsKey(Constants.IS_LEROY_NODE))
                {    
                    setAssignedNode(computers[i].getNode());
                }            

            } catch (InterruptedException ex) {
                Logger.getLogger(NewProject.class.getName()).log(Level.SEVERE, null, ex);
            }

            

        }

        // notify the queue as the project might be now tied to different node
        Jenkins.getInstance().getQueue().scheduleMaintenance();
        // this is to reflect the upstream build adjustments done above
        Jenkins.getInstance().rebuildDependencyGraphAsync();
    }
*/
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

        public String doGetConfig() {
            String configfilename = Hudson.getInstance().getRootDir()+"/plugins/leroy/configuration/"+getName()+".json";
            String res = "";
            try {
                ConfigurationHelper configurationHelper = new ConfigurationHelper(configfilename);
                return configurationHelper.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }

        public ListBoxModel doFillConfigCheckBoxItems() {
            ListBoxModel listitems = new ListBoxModel();
            List<String> items = new ArrayList<String>();
            String configfilename = Hudson.getInstance().getRootDir()+"/plugins/leroy/configuration/"+getName()+".xml";                
            File configfile = new File(configfilename);
            
            String envspath = "";
            
            envspath = Hudson.getInstance().getRootDir()+"/plugins/leroy/temp1/environments.xml";          
            List<String> envsroles = XMLParser.getEnvironment(new File(envspath));

            if(envsroles!=null){
                for (String envs : envsroles) {
                    String tempname = XMLParser.getConfigurationElement(configfile, envs);
                    if(tempname==null)
                        tempname = "scm";
                    
                    items.add(tempname);
                    listitems.add(envs,tempname);
                }
            }
            //environment = items;
            return listitems;
            
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

            //select default item
//            String configfilename = Hudson.getInstance().getRootDir()+"/plugins/leroy/configuration/"+getName()+".json";
//            try {
//                ConfigurationHelper configurationHelper = new ConfigurationHelper(configfilename);
//                Workflow defaultWf = configurationHelper.getDefaultWorkflow();
//                for (int i = 0; i < listitems.size(); i++) {
//                    if (listitems.get(i).name.equals(defaultWf.getName())) {
//                        listitems.get(i).selected = true;
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }


            //listitems.addAll(items);
            workflow = items;
            return listitems;
        }
       
        /**
         * get workflow and environment from scm 
         * 
         * @return 
         */
        public boolean doUpdateConfiguration(@QueryParameter("name") String name,
                @QueryParameter("checked") Boolean checked) throws ServletException 
        {
           
            String configfilename = Hudson.getInstance().getRootDir()+"/plugins/leroy/configuration/"+getName()+".xml";
                
            File configfile = new File(configfilename);
            if(checked) 
                XMLParser.addConfigurationElement(configfile, name, "last");
            else
                XMLParser.addConfigurationElement(configfile, name, "scm");
            
            return false;
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
                
                
                //check if temp folder exists and clean it
                File tempfolder = new File(Hudson.getInstance().getRootDir()+"/plugins/leroy/temp/");
                File tempfolder1 = new File(Hudson.getInstance().getRootDir()+"/plugins/leroy/temp1/");

                if(!tempfolder.exists())
                {
                    tempfolder.mkdir();                
                }
//              else
//              {
//                  tempfolder.delete();
//                  tempfolder.mkdir();                
//              }
                
                if(!tempfolder1.exists())
                {                    
                    tempfolder1.mkdir();                
                }
                else
                {
                    delete(tempfolder1);
                    tempfolder1.mkdir();  
                }
                
                File tempfile = new File(Hudson.getInstance().getRootDir()+"/plugins/leroy/temp/"+uuid1+".txt");
                File tempfile1 = new File(Hudson.getInstance().getRootDir()+"/plugins/leroy/temp/"+uuid+".txt");
                
                String name = this.getName();
                
                StreamBuildListener stream = new StreamBuildListener(new FileOutputStream(tempfile));         
                boolean check=false;   
                
                try
                {
                    AbstractBuild b = new NewFreeStyleBuild((NewFreeStyleProject) this, Calendar.getInstance());
                    check = scm.checkout(b, launcher,checkoutdir ,stream, tempfile1);
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
                
                //update the list
                doFillWorkflowItems();
                doFillEnvrnItems();
                
                
                
                //create a xml file or add workflow and enviroment if available
                File configurationfolder = new File(Hudson.getInstance().getRootDir()+"/plugins/leroy/configuration/");
                
                if(!configurationfolder.exists())
                {
                    configurationfolder.mkdir();                
                }
                
                String configfilename = Hudson.getInstance().getRootDir()+"/plugins/leroy/configuration/"+getName()+".xml";
                String configfilename1 = Hudson.getInstance().getRootDir()+"/plugins/leroy/configuration/"+getName()+".json";

                // create json config
                ConfigurationHelper config = new ConfigurationHelper(configfilename1);
                //create xml
                try {
                    XMLParser.createConfigurtionXML(configfilename);

                    
                } catch(FileAlreadyExistsException e) {
                    //do nothing
                }
                
                //here add envrioment to xml
                // to remove
                File configfile = new File(configfilename);
                for(String s : environment){
                    if(!XMLParser.hasConfigurationElement(configfile, s))
                    {
                        XMLParser.addConfigurationElement(configfile, s, "scm");
                    }
                }

                // fill in config
                // add environments wiht default values
                for (String envName : environment) {
                    Environment env = config.findEnvironment(envName);
                    if (env == null) {
                        config.add(new Environment(envName, false, true, false, "scm"));
                    }
                }
                // add workflows with default values
                for (String wfName : workflow) {
                    Workflow wf = config.findWorkflow(wfName);
                    if (wf == null) {
                        config.add(new Workflow(wfName, false));
                    }
                }
                // save changes
                config.flush();
                
                return check;
                
            } 
            catch (Exception e) {               
                e.printStackTrace();
                return false;
            }
            
        }
        
        public static void delete(File file) throws IOException { 
            if(file.isDirectory()){

                //directory is empty, then delete it
                if(file.list().length==0){
                   file.delete(); 
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
                   }
                }

            }else{
                //if file, then delete it
                file.delete();
                System.out.println("File is deleted : " + file.getAbsolutePath());
            }
    }
}
