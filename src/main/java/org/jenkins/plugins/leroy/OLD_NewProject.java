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
import hudson.model.*;
import hudson.model.Descriptor.FormException;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.tasks.*;
import hudson.triggers.Trigger;
import hudson.util.DescribableList;
import hudson.util.RunList;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.leroy.util.LeroyUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Leroy Deployment Project. Has such a non-descriptive name because of historical reasons.
 * @param <P>
 * @param <B>
 */
public abstract class OLD_NewProject {

    /**
     * List of active {@link Builder}s configured for this project.
     */
    private volatile DescribableList<Builder, Descriptor<Builder>> builders;
    private static final AtomicReferenceFieldUpdater<OLD_NewProject, DescribableList> buildersSetter
            = AtomicReferenceFieldUpdater.newUpdater(OLD_NewProject.class, DescribableList.class, "builders");

    /**
     * List of active {@link Publisher}s configured for this project.
     */
    private volatile DescribableList<Publisher, Descriptor<Publisher>> publishers;
    private static final AtomicReferenceFieldUpdater<OLD_NewProject, DescribableList> publishersSetter
            = AtomicReferenceFieldUpdater.newUpdater(OLD_NewProject.class, DescribableList.class, "publishers");

    /**
     * List of active {@link BuildWrapper}s configured for this project.
     */
    private volatile DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrappers;
    private static final AtomicReferenceFieldUpdater<OLD_NewProject, DescribableList> buildWrappersSetter
            = AtomicReferenceFieldUpdater.newUpdater(OLD_NewProject.class, DescribableList.class, "buildWrappers");

    private static Logger LOGGER = Logger.getLogger(OLD_NewProject.class.getName());

    private List<String> workflow;

    private List<String> environment;


    private void readWorkflowsFromDisk(String workflowsDir) {
        File wfDir = new File(workflowsDir);
        workflow = new ArrayList<String>();
        if (wfDir.exists() && wfDir.canRead()) {
            //get file names
            IOFileFilter workflowFileFilter = new AbstractFileFilter() {
                @Override
                public boolean accept(File file) {
                    try {
                        if (LeroyUtils.isWorkflow(file)) {
                            return true;
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    return false;
                }
            };
            Iterator<File> fileIterator = FileUtils.iterateFiles(new File(workflowsDir), workflowFileFilter, TrueFileFilter.INSTANCE);
            if (fileIterator != null) {
                URI workFlowsBase = new File(workflowsDir).toURI();
                while (fileIterator.hasNext()) {
                    // get relative path using workflow folder as a base and remove extension
                    File wf = fileIterator.next();
                    String relative = workFlowsBase.relativize(wf.toURI()).getPath();
                    String wfName = relative.substring(0, relative.lastIndexOf('.'));
                    workflow.add(wfName);
                }
            }
        }
    }

    /**
     * This method loads files from SCM
     */
    public void loadConfigFromSCM() {
        try {
            SCM scm = this.getScm();
            if (scm == null || scm instanceof NullSCM) {
                return;
            }
            FilePath checkoutDir = new FilePath(Files.createTempDirectory("leroyTempDir").toFile());
            AbstractBuild dumbBuild = new OLD_NewBuild(this);
            Launcher dumbLauncher = Hudson.getInstance().createLauncher(TaskListener.NULL);
            BuildListener dumbListener = new StreamBuildListener(new FileOutputStream(File.createTempFile("dumbListener", "tmp")));
            boolean check = scm.checkout(dumbBuild, dumbLauncher, checkoutDir, dumbListener, File.createTempFile("changelog", "tmp"));
            if (!check) {
                LOGGER.log(Level.SEVERE, "Error trying to load environments and workflows from SCM");
                return;
            }

            readWorkflowsFromDisk(checkoutDir + "/workflows/");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error trying to load environments and workflows from SCM", e);
            return;
        }
    }

//    public List<Builder> getBuilders() {
//        // first load config from SCM
//        loadConfigFromSCM();
//        // if Leroy Builder and Copy Artifact already present return builders
//        boolean containsLeroy = false;
//        boolean containsCopyartifact = false;
//        List<Builder> builders = getBuildersList().toList();
//        for (Builder builder : builders) {
//            if (builder instanceof LeroyBuilder) {
//                containsLeroy = true;
//                // pass workflows to builder
//                ((LeroyBuilder) builder).setWorkflows(workflow);
//            } else if (builder instanceof CopyArtifact) {
//                containsCopyartifact = true;
//            }
//        }
//
//        builders = new ArrayList<Builder>(builders);
//        if (!containsCopyartifact) {
//            builders.add(new CopyArtifact("", "", new StatusBuildSelector(true), "", "${LEROY_HOME}/artifacts/", false, false, true));
//        }
//        if (!containsLeroy) {
//            LeroyBuilder builder = new LeroyBuilder(this.getName(), new ArrayList<LeroyBuilder.Target>(), "", false);
//            builder.setWorkflows(workflow);
//            builders.add(builder);
//        }
//        return builders;
//    }


    @Override
    protected void submit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
        JSONObject json = req.getSubmittedForm();
        super.submit(req, rsp);
        getBuildWrappersList().rebuild(req, json, BuildWrappers.getFor(this));
        getBuildersList().rebuildHetero(req, json, Builder.all(), "builder");
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
     * Return information about the latest builds run on each environment/workflow combination
     * Called from jelly view
     */
    public List<Stat> getJobStats() {
        RunList<B> builds = getBuilds();
        List<Stat> stats = StatsHelper.getLastStats(builds);
        Collections.sort(stats, new Comparator<Stat>() {
            @Override
            public int compare(Stat o1, Stat o2) {
                int result = o1.env.compareTo(o2.env);
                if (result == 0) {
                    result = o2.timestamp.compareTo(o1.timestamp);
                }
                return result;
            }
        });
        return stats;
    }

    @JavaScriptMethod
    public List<Stat> getBuildsByEnvironment(String env) {
        RunList<B> builds = getBuilds();
        if (env == null) {
            env = "null";
        }
        List<Stat> stats = StatsHelper.getBuildsByEnvironment(builds, env);
        Collections.sort(stats, new Comparator<Stat>() {
            @Override
            public int compare(Stat o1, Stat o2) {
                return o2.timestamp.compareTo(o1.timestamp);
            }
        });
        return stats;
    }

    @JavaScriptMethod
    public List<Stat> getBuildsByWorkflow(String workflow) {
        RunList<B> builds = getBuilds();
        if (workflow == null) {
            workflow = "null";
        }
        List<Stat> stats = StatsHelper.getBuildByWorkflow(builds, workflow);
        Collections.sort(stats, new Comparator<Stat>() {
            @Override
            public int compare(Stat o1, Stat o2) {
                return o2.timestamp.compareTo(o1.timestamp);
            }
        });
        return stats;
    }

    /**
     * This class is used to handle statistics of a build for LEroy Deployment Job
     * (e.g. select all altest builds for wf/env, get all builds which were run at a specific workflow/env, etc.)
     */
    public static class StatsHelper {

        public static List<Stat> getAllStats(RunList builds) {
            Iterator it = builds.iterator();
            List<Stat> stats = new ArrayList<Stat>();
            while (it.hasNext()) {
                AbstractBuild b = (AbstractBuild) it.next();
                if (!b.isBuilding() && !b.hasntStartedYet() && b.getResult().isCompleteBuild()) {
                    Stat stat = new Stat();
                    stat.buildNumber = String.valueOf(b.getNumber());
                    stat.artifactsLink = stat.buildNumber + "/artifact/";
                    stat.deployer = getDeployer(b);
                    stat.env = getEnv(b);
                    stat.workflow = getWorkflow(b);
                    stat.timestampString = b.getTimestampString2();
                    stat.timestamp = b.getTimestamp();
                    stat.resultIcon = getStatusImage(b);
                    stat.console = stat.buildNumber + "/console";
                    if (!StringUtils.isEmpty(stat.env) && !StringUtils.isEmpty(stat.workflow)) {
                        stats.add(stat);
                    }
                }
            }
            return stats;
        }

        /**
         * This metod is used to get all leroy builds which were run on specified <code>environment</code>
         * @param builds
         * @param env
         * @return
         */
        public static List<Stat> getBuildsByEnvironment(RunList builds, final String env) {
            if (CollectionUtils.isEmpty(builds)) {
                return new ArrayList<Stat>();
            }
            List<Stat> stats = StatsHelper.getAllStats(builds);
            CollectionUtils.filter(stats, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    Stat stat = (Stat) o;
                    return (env.equalsIgnoreCase(stat.env));
                }
            });
            return stats;
        }

        /**
         * This metod is used to get all leroy builds which were run on specified <code>workflow</code>
         * @param builds
         * @param workflow
         * @return
         */
        public static List<Stat> getBuildByWorkflow(RunList builds, final String workflow) {
            if (CollectionUtils.isEmpty(builds)) {
                return new ArrayList<Stat>();
            }
            List<Stat> stats = StatsHelper.getAllStats(builds);
            CollectionUtils.filter(stats, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    Stat stat = (Stat) o;
                    return (workflow.equalsIgnoreCase(stat.workflow));
                }
            });
            return stats;
        }

        /**
         * This method selects the latest build for each env/workflow combination,
         * and return the list of statistics for these builds.
         * @param builds
         * @return
         */
        public static List<Stat> getLastStats(RunList builds) {
            List<Stat> allStats = getAllStats(builds);
            Map<String, Stat> latestStatMap = new HashMap<String, Stat>();
            for (Stat s : allStats) {
                String key = s.env + "%delim%" + s.workflow;
                Stat latest = latestStatMap.get(key);
                if (latest == null) {
                    latestStatMap.put(key, s);
                } else {
                    if (latest.timestamp.before(s.timestamp)) {
                        latestStatMap.put(key, s);
                    }
                }
            }
            return new ArrayList<Stat>(latestStatMap.values());
        }

        /**
         * This method returns the deployer of the build
         * @param build
         * @return
         */
        public static String getDeployer(AbstractBuild build) {
            String depl = build.getDescription();
            if (!StringUtils.isEmpty(depl)) {
                // remove "By: "
                return depl.substring(4);
            }
            return "";
        }

        /**
         * This method return the environment this build was run at
         * @param build
         * @return
         */
        public static String getEnv(AbstractBuild build) {
            String displayName = build.getDisplayName();
            // <env>_<workflow>
            if (!StringUtils.isEmpty(displayName) && displayName.contains("_")) {
                return displayName.substring(0, displayName.indexOf("_"));
            }
            return "";
        }

        /**
         * This method return the workflow this build was run at
         * @param build
         * @return
         */
        public static String getWorkflow(AbstractBuild build) {
            String displayName = build.getDisplayName();
            // <env>_<workflow>
            if (!StringUtils.isEmpty(displayName) && displayName.contains("_")) {
                return displayName.substring(displayName.indexOf("_") + 1);
            }
            return "";
        }

        /**
         * Returns build icon. Use the icon for Jenkins icons set
         * @param build
         * @return
         */
        public static String getStatusImage(AbstractBuild build) {
            return build.getIconColor().getImageOf("16x16");
        }
    }

    /**
     * Represents a build statistics we display on UI for Leroy Deployment Job
     */
    public static class Stat {
        public String resultIcon;
        public String env;
        public String workflow;
        public String deployer;
        public String timestampString;
        public Calendar timestamp;
        public String artifactsLink; // relative link to artifacts
        public String buildNumber;
        public String console; // relative link to console "/<build #>/console"
    }

}
