/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Tom Huybrechts
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

import hudson.*;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.leroy.beans.Update;
import org.jenkins.plugins.leroy.jaxb.beans.AgentBean;
import org.jenkins.plugins.leroy.jaxb.beans.AgentInEnvironmentBean;
import org.jenkins.plugins.leroy.jaxb.beans.ControllerBean;
import org.jenkins.plugins.leroy.jaxb.beans.EnvironmentBean;
import org.jenkins.plugins.leroy.util.Constants;
import org.jenkins.plugins.leroy.util.LeroyUtils;
import org.jenkins.plugins.leroy.util.XMLParser;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link NodeProperty} that sets additional node properties.
 *
 * @author Dzmitry Bahdanovich
 * @since 1.286
 */
public class LeroyNodeProperty extends NodeProperty<Node> {

    private static final Logger LOGGER = Logger.getLogger(LeroyNodeProperty.class.getName());

    private String leroyhome;

    private String architecture;

    private String nodeName;

    private List<AgentBean> installedAgents = new ArrayList<AgentBean>();

    /**
     * The list of environments on this node
     */
    private List<EnvironmentBean> environments = new ArrayList<EnvironmentBean>();

    // controller settings
    private String controllerHost;
    private String controllerPort;
    private String controllerBind;
    private String controllerLogFile;
    private String controllerLogLevel;
    private String controllerTimeout;

    private String controllerVersion;

    private transient ControllerBean controllerBean;

    @DataBoundConstructor
    public LeroyNodeProperty(String nodeName, String leroyhome, String architecture, String controllerHost, String controllerPort, String controllerBind, String controllerLogFile, String controllerLogLevel, String controllerTimeout, List<AgentBean> installedAgents, List<EnvironmentBean> environments) {
        this.nodeName = nodeName;
        this.leroyhome = leroyhome;
        this.architecture = architecture;
        this.controllerHost = controllerHost;
        this.controllerPort = controllerPort;
        this.controllerBind = controllerBind;
        this.controllerLogFile = controllerLogFile;
        this.controllerLogLevel = controllerLogLevel;
        this.controllerTimeout = controllerTimeout;
        this.installedAgents = installedAgents;
        this.environments = environments;
    }

    /**
     * This method loads environments from <LEROY_HOME>/environments.xml
     *
     * @return
     */
    public List<EnvironmentBean> loadAndGetEnvironments() {
        Node node = LeroyUtils.findNodeByName(nodeName);
        if (LeroyUtils.isNodeAvailable(node)) {
            FilePath leroyHomePath = new FilePath(node.getChannel(), leroyhome);
            try {
                environments = leroyHomePath.act(new LeroyUtils.ReadEnvironments());
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        return environments;
    }

    public List<EnvironmentBean> getEnvironments() {
        return environments;
    }

    public void setEnvironments(List<EnvironmentBean> environments) {
        this.environments = environments;
    }

    public List<AgentBean> getInstalledAgents() {
        Node node = LeroyUtils.findNodeByName(nodeName);
        if (LeroyUtils.isNodeAvailable(node)) {
            FilePath leroyHomePath = new FilePath(node.getChannel(), leroyhome);
            try {
                installedAgents = leroyHomePath.act(new LeroyUtils.ReadAgents());
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        return installedAgents;
    }

    public void setInstalledAgents(List<AgentBean> installedAgents) {
        this.installedAgents = installedAgents;
    }

    /**
     * This method writes agents configuration to <code>agents.xml</code> file on a proper leroy node
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void updateAgentsXml() throws IOException, InterruptedException {
        if (CollectionUtils.isEmpty(installedAgents)) {
            return;
        }
        Node node = LeroyUtils.findNodeByName(nodeName);
        if (LeroyUtils.isNodeAvailable(node)) {
            FilePath leroyHomePath = new FilePath(node.getChannel(), leroyhome);
            leroyHomePath.act(new SaveAgents(installedAgents));
        }
    }

    /**
     * This method writes environments configuration to <code>environments.xml</code> file on a proper leroy node
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void updateEnvironmentsXml() throws IOException, InterruptedException {
        if (CollectionUtils.isEmpty(environments)) {
            return;
        }
        Node node = LeroyUtils.findNodeByName(nodeName);
        if (LeroyUtils.isNodeAvailable(node)) {
            FilePath leroyHomePath = new FilePath(node.getChannel(), leroyhome);
            leroyHomePath.act(new LeroyUtils.SaveEnvironments(environments));
        }
    }

    /**
     * This method writes controller configuration to <code>controller.xml</code> file on a proper leroy node
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void updateControllerXml() throws IOException, InterruptedException {
        ControllerBean controller = new ControllerBean();
        controller.setHost(controllerHost);
        controller.setPort(controllerPort);
        controller.setBind(controllerBind);
        controller.setLogFile(controllerLogFile);
        controller.setLogLevel(controllerLogLevel);
        controller.setAgentsCheckinTimeout(controllerTimeout);
        Node node = LeroyUtils.findNodeByName(nodeName);
        if (LeroyUtils.isNodeAvailable(node)) {
            FilePath leroyHomePath = new FilePath(node.getChannel(), leroyhome);
            leroyHomePath.act(new LeroyUtils.SaveController(controller));
        }
    }


    public String getLeroyhome() {
        return leroyhome;
    }

    public String getarchitecture() {
        return architecture;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getControllerHost() {
        if (StringUtils.isEmpty(controllerHost)) {
            controllerHost = "127.0.0.1";
            try {
                Node node = LeroyUtils.findNodeByName(nodeName);
                if (node != null) {
                    controllerHost = LeroyUtils.getHostAddress(new FilePath(node.getChannel(), leroyhome));
                }
            } catch (Exception e) {
                controllerHost = "127.0.0.1";
                LOGGER.log(Level.SEVERE, "Error getting controller host. Set Controller Host to 127.0.0.1", e);
            }
        }
        return controllerHost;
    }

    public String getControllerPort() {
        if (StringUtils.isEmpty(controllerPort)) {
            controllerPort = "1337";
        }
        return controllerPort;
    }

    public String getControllerBind() {
        if (StringUtils.isEmpty(controllerBind)) {
            controllerBind = "0.0.0.0";
        }
        return controllerBind;
    }

    public String getControllerLogFile() {
        if (StringUtils.isEmpty(controllerLogFile)) {
            controllerLogFile = "controller.log";
        }
        return controllerLogFile;
    }

    public String getControllerLogLevel() {
        if (StringUtils.isEmpty(controllerLogLevel)) {
            controllerLogLevel = "error";
        }
        return controllerLogLevel;
    }

    public String getControllerTimeout() {
        if (StringUtils.isEmpty(controllerTimeout)) {
            controllerTimeout = "15";
        }
        return controllerTimeout;
    }

    public String getControllerVersion() {
        Node node = LeroyUtils.findNodeByName(nodeName);
        String version = null;
        if (LeroyUtils.isNodeAvailable(node)) {
            version = LeroyUtils.getControllerVersion(node, new FilePath(node.getChannel(), leroyhome));
        } else {
            return "Node is unavailable";
        }
        if (StringUtils.isEmpty(version) || "N/A".equalsIgnoreCase(version)) {
            return "Leroy has not been properly installed.";
        } else {
            return "v" + version.trim() + " of Leroy is properly installed";
        }
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher,
                             BuildListener listener) throws IOException, InterruptedException {
        EnvVars env = new EnvVars();
        env.put(Constants.IS_LEROY_NODE, "TRUE");
        env.put(Constants.LEROY_HOME, getLeroyhome());
        return Environment.create(env);
    }

    @Override
    public void buildEnvVars(EnvVars env, TaskListener listener) throws IOException, InterruptedException {
        env.put(Constants.IS_LEROY_NODE, "TRUE");
        env.put(Constants.LEROY_HOME, getLeroyhome());
    }


    @Override
    public NodeProperty<?> reconfigure(org.kohsuke.stapler.StaplerRequest req, net.sf.json.JSONObject form) throws Descriptor.FormException {
        if (form == null) {
            return null;
        }
        LeroyNodeProperty prop = (LeroyNodeProperty) getDescriptor().newInstance(req, form);

        //TODO  move to validator class later
        if (!CollectionUtils.isEmpty(prop.getEnvironments())) {
            for (EnvironmentBean env : prop.getEnvironments()) {
                List<AgentInEnvironmentBean> agents = env.getAgents();
                if (!CollectionUtils.isEmpty(agents)) {
                    Set<String> agentIds = new HashSet<String>();
                    for (AgentInEnvironmentBean agent : agents) {
                        if (StringUtils.isEmpty(agent.getId())) {
                            throw new Descriptor.FormException("Agent ID cannot be empty", "id");
                        }
                        if (!agentIds.add(agent.getId())) {
                            throw new Descriptor.FormException("Agent with ID '" + agent.getId() + "' already exists in environment '" + env.getName() + "'", "id");
                        }
                    }
                }
            }
        }

        // update files in LEROY_HOME directory
        try {
            prop.updateControllerXml();
            prop.updateAgentsXml();
            prop.updateEnvironmentsXml();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Cannot update configuration files.", e);
        }
        return prop;
    }

    /**
     *
     */
    @Extension
    public static class DescriptorImpl extends NodePropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "Leroy Host"; //TODO externalize
        }

        public ListBoxModel doFillControllerLogLevelItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("none", "none");
            items.add("debug", "debug");
            items.add("info", "info");
            items.add("warning", "warning");
            items.add("error", "error");
            return items;
        }

        public ListBoxModel doFillArchitectureItems() {
            ListBoxModel items = new ListBoxModel();

            for (Constants.Architecture arc : Constants.Architecture.values()) {
                items.add(arc.getValue(), arc.getValue());
            }
            return items;
        }

        public ListBoxModel doFillAgentPlatformItems() {
            return doFillArchitectureItems();
        }

        public FormValidation doAddAgent(@QueryParameter("nodeName") final String nodeName,
                                         @QueryParameter("leroyhome") final String leroyHome,
                                         @QueryParameter("agentName") final String agentName,
                                         @QueryParameter("agentPlatform") final String agentPlatform,
                                         @QueryParameter("agentLockerpath") final String agentLockerpath,
                                         @QueryParameter("sshHost") final String sshHost,
                                         @QueryParameter("sshPort") final String sshPort,
                                         @QueryParameter("sshUser") final String sshUser,
                                         @QueryParameter("sshPass") final String sshPass,
                                         @QueryParameter("sshDest") final String sshDest,
                                         @QueryParameter("sshInstall") final String sshInstall)
                throws IOException, ServletException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            if (StringUtils.isEmpty(agentName)) {
                return FormValidation.error("Please provide a name of the agent");
            }

            Node node = LeroyUtils.findNodeByName(nodeName);
            if (!LeroyUtils.isNodeAvailable(node)) {
                return FormValidation.error(Constants.CANNOT_ESTABLISH_CONNECTION);
            }

            try {
                Launcher launcher = node.createLauncher(TaskListener.NULL);
                int returnCode = 0;
                List<String> cmds = new ArrayList<String>();
                cmds.add(leroyHome + "/controller");
                cmds.add("--addagent");
                cmds.add("--agent-name");
                cmds.add(agentName);
                cmds.add("--agent-platform");
                cmds.add(agentPlatform);
                if (!StringUtils.isEmpty(agentLockerpath)) {
                    cmds.add("--agent-lockerpath");
                    cmds.add(agentLockerpath);
                }
                returnCode = launcher.launch().pwd(leroyHome).envs(Functions.getEnvVars()).cmds(cmds).stdout(output).join();
                if (returnCode != 0) {
                    return FormValidation.error("Cannot add agent: " + output);
                } else {
                    LeroyNodeProperty prop = (LeroyNodeProperty) node.getNodeProperties().get(this);
                    List<AgentBean> agents = prop.getInstalledAgents();
                    if (CollectionUtils.isEmpty(agents)) {
                        prop.setInstalledAgents(new ArrayList<AgentBean>() {{
                            add(new AgentBean(agentName, agentLockerpath, null, null, null));
                        }});
                    } else {
                        agents.add(new AgentBean(agentName, agentLockerpath, null, null, null));
                    }
                }
                String bundleName = agentName + "-" + agentPlatform + ".zip";

                // if we need to install it via ssh
                if (!"true".equalsIgnoreCase(sshInstall)) {
                    if (returnCode == 0) {
                        String result = "Generated: " + new File(leroyHome, bundleName).getAbsoluteFile() + "\n" +
                                "To manually install agent, copy and extract this zip file " +
                                "to your host and run the agent binary."; //TODO externalize
                        return FormValidation.ok(result);
                    }
                } else {
                    cmds = new ArrayList<String>();
                    cmds.add("--ssh-install");
                    cmds.add(bundleName);
                    cmds.add("--ssh-host");
                    cmds.add(sshHost);
                    cmds.add("--ssh-port");
                    cmds.add(sshPort);
                    cmds.add("--ssh-user");
                    cmds.add(sshUser);
                    cmds.add("--ssh-pass");
                    cmds.add(sshPass);
                    cmds.add("--ssh-destdir");
                    cmds.add(sshDest);
                    returnCode = launcher.launch().envs(Functions.getEnvVars()).pwd(leroyHome)
                            .cmds(leroyHome + "/controller"
                                    , "--ssh-install", bundleName
                                    , "--ssh-host", sshHost
                                    , "--ssh-port", sshPort
                                    , "--ssh-user", sshUser
                                    , "--ssh-pass", sshPass
                                    , "--ssh-destdir", sshDest).stdout(output).join();
                }

                if (returnCode == 0) {
                    String result = "Generated: " + new File(leroyHome, bundleName).getAbsoluteFile() + "\n" +
                            "Installed via SSH"; //TODO externalize
                    return FormValidation.ok(result);
                }
                return FormValidation.error("Failed to install agent via SSH" + output);

            } catch (Exception e) {
                return FormValidation.error("Client error : " + e.getMessage());
            }
        }


        public FormValidation doUpdateController(@QueryParameter("architecture") String architecture,
                                                 @QueryParameter("leroyhome") String leroyhome,
                                                 @QueryParameter String nodeName) {
            //get update file from server
            try {
                Update update = XMLParser.readUpdate();
                if (update != null) {
                    // get controller version
                    Node node = LeroyUtils.findNodeByName(nodeName);
                    if (node == null) {
                        return FormValidation.error("Cannot find an appropriate node");
                    }
                    FilePath leroyHomeDir = new FilePath(node.getChannel(), leroyhome);
                    String controllerVersion = LeroyUtils.getControllerVersion(node, leroyHomeDir);
                    if (controllerVersion.equalsIgnoreCase(String.valueOf(update.getVersion()))) {
                        return FormValidation.warning("Controller has last version: '" + update.getVersion() + "'");
                    }
                    String binary = update.getBinaries().get(architecture);
                    if (binary != null) {
                        FilePath temp = LeroyUtils.downloadFile(binary, node);
                        LeroyUtils.unpack(temp, leroyHomeDir);
                    }
                } else {
                    return FormValidation.error("Cannot get update from server. Please try later.");
                }
            } catch (Exception e) {
                return FormValidation.error(e, "Error while updating the controller"); // temporatily show stacktrace on UI
            }
            return FormValidation.ok("Controller is updated successfully");
        }

        public FormValidation doCheckLeroyhome(@QueryParameter final String value, @QueryParameter String nodeName)
                throws IOException, ServletException {
            // check for null
            if (value.length() == 0) {
                return FormValidation.error("Please provide a path for leroy plugin");
            }

            // is directory exists and writable
            try {
                Node node = LeroyUtils.findNodeByName(nodeName);
                if (!LeroyUtils.isNodeAvailable(node)) {
                    return FormValidation.error("Couldn't establish connection to node");
                }
                FilePath leroyHome = new FilePath(node.getChannel(), value);
                if (!leroyHome.exists()) {
                    return FormValidation.warning("Folder doesn't exist.");
                }
                if (!LeroyUtils.canWrite(leroyHome)) {
                    return FormValidation.error("Cannot write to folder. Please check if user has write permissions on this folder");
                }
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Cannot access node", t);
                return FormValidation.error("Cannot access node");
            }
            return FormValidation.ok();
        }

        // TODO implement
        public FormValidation doCheckAgentName(@QueryParameter String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.error("Please provide a name of the agent");
            }
            return FormValidation.ok();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

    }

    /**
     * This class is used to save agents remotely to agents.xml in a given directory
     */
    public static class SaveAgents implements FilePath.FileCallable<Void> {
        private static final long serialVersionUID = 1;

        private List<AgentBean> agents;

        public SaveAgents(List<AgentBean> agents) {
            this.agents = agents;
        }

        @Override
        public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            File agentsXml = new File(f, "agents.xml");
            XMLParser.saveAgents(agents, agentsXml.getAbsolutePath());
            return null;
        }
    }

}
