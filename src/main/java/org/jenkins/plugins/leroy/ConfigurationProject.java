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

import com.jayway.jsonpath.JsonPath;
import hudson.Util;
import hudson.model.*;
import hudson.model.Descriptor.FormException;
import hudson.tasks.*;
import hudson.tasks.Maven.MavenInstallation;
import hudson.tasks.Maven.ProjectWithMaven;
import hudson.triggers.Trigger;
import hudson.util.DescribableList;
import net.sf.json.JSONObject;
import org.jenkins.plugins.leroy.util.JsonUtils;
import org.jenkins.plugins.leroy.util.LeroyUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Buildable software project.
 *
 * @author Yunus Dawji
 */
public abstract class ConfigurationProject<P extends ConfigurationProject<P, B>, B extends ConfigurationBuild<P, B>>
        extends AbstractProject<P, B> implements SCMedItem, Saveable, ProjectWithMaven, BuildableItemWithBuildWrappers {

    /**
     * List of active {@link Builder}s configured for this project.
     */
    private volatile DescribableList<Builder, Descriptor<Builder>> builders;
    private static final AtomicReferenceFieldUpdater<ConfigurationProject, DescribableList> buildersSetter
            = AtomicReferenceFieldUpdater.newUpdater(ConfigurationProject.class, DescribableList.class, "builders");

    /**
     * List of active {@link Publisher}s configured for this project.
     */
    private volatile DescribableList<Publisher, Descriptor<Publisher>> publishers;
    private static final AtomicReferenceFieldUpdater<ConfigurationProject, DescribableList> publishersSetter
            = AtomicReferenceFieldUpdater.newUpdater(ConfigurationProject.class, DescribableList.class, "publishers");

    /**
     * List of active {@link BuildWrapper}s configured for this project.
     */
    private volatile DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrappers;
    private static final AtomicReferenceFieldUpdater<ConfigurationProject, DescribableList> buildWrappersSetter
            = AtomicReferenceFieldUpdater.newUpdater(ConfigurationProject.class, DescribableList.class, "buildWrappers");

    /**
     * Creates a new project.
     */
    public ConfigurationProject(ItemGroup parent, String name) throws IOException {
        super(parent, name);

    }

    @Override
    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, Descriptor.FormException {

        super.doConfigSubmit(req, rsp);
        // set assigned node
        JSONObject json = req.getSubmittedForm();
        String jsonStr = json.toString();
        String leroyBuilderJson = JsonUtils.getLeroyConfigurationBuilderJSON(jsonStr);
        String leroyNode = JsonPath.read(leroyBuilderJson, "$.leroyNode");
        String assignedNodeName = leroyNode == null ? "" : leroyNode;
        Node n = LeroyUtils.findNodeByName(assignedNodeName);
        if (n != null) {
            setAssignedNode(n);
        }
        save();
    }

    public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
        super.onLoad((ItemGroup) parent, name);
        getBuildersList().setOwner(this);
        getPublishersList().setOwner(this);
        getBuildWrappersList().setOwner(this);
    }

    public AbstractProject<?, ?> asProject() {
        return this;
    }

    public List<Builder> getBuilders() {
        boolean check = false;
        List<Builder> temp = getBuildersList().toList();
        List<Builder> builders = new ArrayList<Builder>();
        for (Builder builder : temp) {
            if (builder instanceof LeroyConfigurationBuilder) {
                check = true;
            }
            builders.add(builder);
        }
        if (!check) {
            builders.add(new LeroyConfigurationBuilder(""));
        }
        return builders;
    }

    /**
     * @deprecated as of 1.463
     * We will be soon removing the restriction that only one instance of publisher is allowed per type.
     * Use {@link #getPublishersList()} instead.
     */
    public Map<Descriptor<Publisher>, Publisher> getPublishers() {
        return getPublishersList().toMap();
    }

    public DescribableList<Builder, Descriptor<Builder>> getBuildersList() {
        if (builders == null) {
            buildersSetter.compareAndSet(this, null, new DescribableList<Builder, Descriptor<Builder>>(this));
        }
        return builders;
    }

    public DescribableList<Publisher, Descriptor<Publisher>> getPublishersList() {
        if (publishers == null) {
            publishersSetter.compareAndSet(this, null, new DescribableList<Publisher, Descriptor<Publisher>>(this));
        }
        return publishers;
    }

    public Map<Descriptor<BuildWrapper>, BuildWrapper> getBuildWrappers() {
        return getBuildWrappersList().toMap();
    }

    public DescribableList<BuildWrapper, Descriptor<BuildWrapper>> getBuildWrappersList() {
        if (buildWrappers == null) {
            buildWrappersSetter.compareAndSet(this, null, new DescribableList<BuildWrapper, Descriptor<BuildWrapper>>(this));
        }
        return buildWrappers;
    }

    @Override
    protected Set<ResourceActivity> getResourceActivities() {
        final Set<ResourceActivity> activities = new HashSet<ResourceActivity>();

        activities.addAll(super.getResourceActivities());
        activities.addAll(Util.filter(getBuildersList(), ResourceActivity.class));
        activities.addAll(Util.filter(getPublishersList(), ResourceActivity.class));
        activities.addAll(Util.filter(getBuildWrappersList(), ResourceActivity.class));

        return activities;
    }

    /**
     * Adds a new {@link BuildStep} to this {@link Project} and saves the configuration.
     *
     * @deprecated as of 1.290
     * Use {@code getPublishersList().add(x)}
     */
    public void addPublisher(Publisher buildStep) throws IOException {
        getPublishersList().add(buildStep);
    }

    /**
     * Removes a publisher from this project, if it's active.
     *
     * @deprecated as of 1.290
     * Use {@code getPublishersList().remove(x)}
     */
    public void removePublisher(Descriptor<Publisher> descriptor) throws IOException {
        getPublishersList().remove(descriptor);
    }

    public Publisher getPublisher(Descriptor<Publisher> descriptor) {
        for (Publisher p : getPublishersList()) {
            if (p.getDescriptor() == descriptor)
                return p;
        }
        return null;
    }

    protected void buildDependencyGraph(DependencyGraph graph) {
        getPublishersList().buildDependencyGraph(this, graph);
        getBuildersList().buildDependencyGraph(this, graph);
        getBuildWrappersList().buildDependencyGraph(this, graph);
    }

    @Override
    public boolean isFingerprintConfigured() {
        return getPublishersList().get(Fingerprinter.class) != null;
    }

    public MavenInstallation inferMavenInstallation() {
        Maven m = getBuildersList().get(Maven.class);
        if (m != null) return m.getMaven();
        return null;
    }

    @Override
    protected void submit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
        super.submit(req, rsp);

        JSONObject json = req.getSubmittedForm();

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
}
