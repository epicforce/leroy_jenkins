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
public abstract class OLD_ConfigurationProject {


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

    @Override
    protected void submit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
        super.submit(req, rsp);

        JSONObject json = req.getSubmittedForm();

        getBuildWrappersList().rebuild(req, json, BuildWrappers.getFor(this));
        getBuildersList().rebuildHetero(req, json, Builder.all(), "builder");
        getPublishersList().rebuildHetero(req, json, Publisher.all(), "publisher");
    }

}
