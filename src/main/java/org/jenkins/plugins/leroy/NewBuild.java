/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Martin Eigenbrodt
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
import hudson.tasks.*;
import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.leroy.util.Constants;
import org.jenkins.plugins.leroy.util.JsonUtils;
import org.jenkins.plugins.leroy.util.LeroyUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import static hudson.model.Result.FAILURE;

/**
 * A build of a {@link Project}.
 * <p/>
 * <h2>Steps of a build</h2>
 * <p/>
 * Roughly speaking, a {@link NewBuild} goes through the following stages:
 * <p/>
 * <dl>
 * <dt>SCM checkout
 * <dd>Hudson decides which directory to use for a build, then the source code is checked out
 * <p/>
 * <dt>Pre-build steps
 * <dd>Everyone gets their {@link BuildStep#prebuild(AbstractBuild, BuildListener)} invoked
 * to indicate that the build is starting
 * <p/>
 * <dt>NewBuild wrapper set up
 * <dd>{@link BuildWrapper#setUp(AbstractBuild, Launcher, BuildListener)} is invoked. This is normally
 * to prepare an environment for the build.
 * <p/>
 * <dt>Builder runs
 * <dd>{@link Builder#perform(AbstractBuild, Launcher, BuildListener)} is invoked. This is where
 * things that are useful to users happen, like calling Ant, Make, etc.
 * <p/>
 * <dt>Recorder runs
 * <dd>{@link Recorder#perform(AbstractBuild, Launcher, BuildListener)} is invoked. This is normally
 * to record the output from the build, such as test results.
 * <p/>
 * <dt>Notifier runs
 * <dd>{@link Notifier#perform(AbstractBuild, Launcher, BuildListener)} is invoked. This is normally
 * to send out notifications, based on the results determined so far.
 * </dl>
 * <p/>
 * <p/>
 * And beyond that, the build is considered complete, and from then on {@link NewBuild} object is there to
 * keep the record of what happened in this build.
 *
 * @author Kohsuke Kawaguchi
 */
public class NewBuild<P extends NewProject<P, B>, B extends NewBuild<P, B>>
        extends AbstractBuild<P, B> {

    /**
     * Creates a new build.
     */
    protected NewBuild(P project) throws IOException {
        super(project);
    }

    protected NewBuild(P job, Calendar timestamp) {
        super(job, timestamp);
    }

    /**
     * Loads a build from a log file.
     */
    protected NewBuild(P project, File buildDir) throws IOException {
        super(project, buildDir);
    }

    //
//
// actions
//
//
    @Override
    public void run() {
        execute(createRunner());
    }

    /**
     * @deprecated as of 1.467
     * Override the {@link #run()} method by calling {@link #execute(RunExecution)} with
     * proper execution object.
     */
    @Restricted(NoExternalUse.class)
    protected Runner createRunner() {
        return new BuildExecution();
    }

    /**
     * @deprecated as of 1.467
     * Please use {@link BuildExecution}
     */
    protected class RunnerImpl extends BuildExecution {
    }

    protected class BuildExecution extends AbstractRunner {
        /*
            Some plugins might depend on this instance castable to Runner, so we need to use
            deprecated class here.
         */

        private String user;
        private LeroyBuilder.Target target;


        public BuildExecution() {
            super();
            try {
                init();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void init() throws IOException, InterruptedException {
            String targetConfig = getBuild().getEnvironment(listener).get(Constants.TARGET_CONFIGURATION);
            if (!StringUtils.isEmpty(targetConfig)) {
                target = JsonUtils.getTargetFromBuildParameter(targetConfig);
            }
            user = LeroyUtils.getUserRunTheBuild(getBuild());
        }

        private void setNameDescription() throws IOException, InterruptedException {
            String env = "null";
            String wf = "null";
            if (target != null) {
                env = target.environment;
                wf = target.workflow;
            }
            getBuild().setDisplayName(env + "_" + wf);
            getBuild().setDescription("By: " + user); //TODO externalize
        }

        private Result checkLeroyHomeWritable(BuildListener listener) throws IOException, InterruptedException {
            String leroyHome = LeroyUtils.getLeroyHome(Executor.currentExecutor());
            Node node = Executor.currentExecutor().getOwner().getNode();
            Result r = null;
            if (!LeroyUtils.canWrite(new FilePath(node.getChannel(), leroyHome))) {
                r = Executor.currentExecutor().abortResult();
                listener.error("LEROY_HOME is not writeable to {0} please grant this user write permissions to this folder in order for Leroy to function properly.", user);
            }
            return r;
        }

        protected Result doRun(BuildListener listener) throws Exception {
            Result r = null;

            setNameDescription();

            if (!preBuild(listener, project.getBuilders()))
                return FAILURE;
            if (!preBuild(listener, project.getPublishersList()))
                return FAILURE;

            try {
                r = checkLeroyHomeWritable(listener);

                List<BuildWrapper> wrappers = new ArrayList<BuildWrapper>(project.getBuildWrappers().values());
                ParametersAction parameters = getAction(ParametersAction.class);

                if (parameters != null)
                    parameters.createBuildWrappers(NewBuild.this, wrappers);

                for (BuildWrapper w : wrappers) {
                    Environment e = w.setUp((AbstractBuild<?, ?>) NewBuild.this, launcher, listener);
                    if (e == null)
                        return (r = FAILURE);
                    buildEnvironments.add(e);
                }

                if (!build(listener, project.getBuilders()))
                    r = FAILURE;
            } catch (InterruptedException e) {
                r = Executor.currentExecutor().abortResult();
                // not calling Executor.recordCauseOfInterruption here. We do that where this exception is consumed.
                throw e;
            } finally {
                if (r != null) setResult(r);
                // tear down in reverse order
                boolean failed = false;
                for (int i = buildEnvironments.size() - 1; i >= 0; i--) {
                    if (!buildEnvironments.get(i).tearDown(NewBuild.this, listener)) {
                        failed = true;
                    }
                }
                // WARNING The return in the finally clause will trump any return before
                if (failed) return FAILURE;
            }
            return r;
        }

        public void post2(BuildListener listener) throws IOException, InterruptedException {
            if (!performAllBuildSteps(listener, project.getPublishersList(), true))
                setResult(FAILURE);
            if (!performAllBuildSteps(listener, project.getProperties(), true))
                setResult(FAILURE);
        }

        @Override
        public void cleanUp(BuildListener listener) throws Exception {
            // at this point it's too late to mark the build as a failure, so ignore return value.
            performAllBuildSteps(listener, project.getPublishersList(), false);
            performAllBuildSteps(listener, project.getProperties(), false);
            super.cleanUp(listener);
        }

        private boolean build(BuildListener listener, Collection<Builder> steps) throws IOException, InterruptedException {
            for (BuildStep bs : steps) {
                if (!perform(bs, listener)) {
                    LOGGER.fine(MessageFormat.format("{0} : {1} failed", NewBuild.this.toString(), bs));
                    return false;
                }

                Executor executor = getExecutor();
                if (executor != null && executor.isInterrupted()) {
                    // someone asked build interruption, let stop the build before trying to run another build step
                    throw new InterruptedException();
                }
            }
            return true;
        }
    }

    public boolean doWo() {
        return true;
    }

    private static final Logger LOGGER = Logger.getLogger(NewBuild.class.getName());
}
