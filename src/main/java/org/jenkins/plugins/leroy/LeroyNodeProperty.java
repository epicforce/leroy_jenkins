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

import hudson.EnvVars;
import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.*;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.leroy.beans.Update;
import org.jenkins.plugins.leroy.jaxb.beans.AgentBean;
import org.jenkins.plugins.leroy.jaxb.beans.ControllerBean;
import org.jenkins.plugins.leroy.jaxb.beans.EnvironmentBean;
import org.jenkins.plugins.leroy.util.Constants;
import org.jenkins.plugins.leroy.util.LeroyUtils;
import org.jenkins.plugins.leroy.util.XMLParser;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link NodeProperty} that sets additional node properties.
 * @author Dzmitry Bahdanovich
 * @since 1.286
 */
public class LeroyNodeProperty extends NodeProperty<Node> {

    private String leroyhome;

    private String architecture;

    private List<AgentBean> installedAgents;

    /**
     * The list of environments on this node
     */
    private List<EnvironmentBean> environments;

    // controller settings
    private String controllerHost;
    private String controllerPort;
    private String controllerBind;
    private String controllerLogFile;
    private String controllerLogLevel;
    private String controllerTimeout;

    private String controllerVersion;

    @DataBoundConstructor
    public LeroyNodeProperty(String leroyhome, String architecture, String controllerHost, String controllerPort, String controllerBind, String controllerLogFile, String controllerLogLevel, String controllerTimeout, List<AgentBean> installedAgents, List<EnvironmentBean> environments) {
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
        updateControllerXml();
        updateAgentsXml();
        updateEnvironmentsXml();
    }

    private void updateControllerXml() {
        String controllerXml = leroyhome + "/controller.xml";
        ControllerBean controller = new ControllerBean();
        controller.setHost(controllerHost);
        controller.setPort(controllerPort);
        controller.setBind(controllerBind);
        controller.setLogFile(controllerLogFile);
        controller.setLogLevel(controllerLogLevel);
        controller.setAgentsCheckinTimeout(controllerTimeout);
        XMLParser.saveController(controller, controllerXml);
    }

    private void updateControllerJobProperties() {
        String controllerXml = leroyhome + "/controller.xml";
        ControllerBean controller = XMLParser.readController(controllerXml);
        controllerHost = controller.getHost();
        controllerPort = controller.getPort();
        controllerBind = controller.getBind();
        controllerLogFile = controller.getLogFile();
        controllerLogLevel = controller.getLogLevel();
        controllerTimeout = controller.getAgentsCheckinTimeout();
    }

    public List<EnvironmentBean> getEnvironments() {
        // dumb
//        environments = new ArrayList<EnvironmentBean>();
//
//        List<AgentInEnvironmentBean> agents = new ArrayList<AgentInEnvironmentBean>();
//        agents.add(new AgentInEnvironmentBean("agent1","admin,customer,deployer","id1"));
//        agents.add(new AgentInEnvironmentBean("agent2","admin,deployer","id2"));
//        agents.add(new AgentInEnvironmentBean("agent3","admin","id3"));
//
//        environments.add(new EnvironmentBean("env1", agents ));
//        environments.add(new EnvironmentBean("env2", agents ));
        environments = XMLParser.readEnvironments(leroyhome + "/environments.xml");

        return environments;
    }

    public void setEnvironments(List<EnvironmentBean> environments) {
        this.environments = environments;
    }

    public List<AgentBean> getInstalledAgents() {
        installedAgents = XMLParser.readAgents(leroyhome + "/agents.xml");
        return installedAgents;
    }

    public void setInstalledAgents(List<AgentBean> installedAgents) {
        this.installedAgents = installedAgents;
    }

    private void updateAgentsXml() {
        if (installedAgents == null) {
            return;
        }
        String agentsXml = leroyhome + "/agents.xml";
        XMLParser.saveAgents(installedAgents, agentsXml);
    }


    private void updateEnvironmentsXml() {
        if (CollectionUtils.isEmpty(environments)) {
            return;
        }
        String xmlFile = leroyhome + "/environments.xml";
        XMLParser.saveEnvironments(environments, xmlFile);
    }

    private synchronized ControllerBean getControllerBean() {
        return XMLParser.readController(leroyhome + "/controller.xml");
    }

    public String getLeroyhome() {
        return leroyhome;
    }

    public String getarchitecture() {
        return architecture;
    }

    public String getControllerHost() {
        if (getControllerBean() == null || StringUtils.isEmpty(getControllerBean().getHost())) {
            String host = "127.0.0.1";
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch (Throwable e) {
                // omit
            }
            return host;
        }
        return getControllerBean().getHost();
    }

    public String getControllerPort() {
        if (getControllerBean() == null || StringUtils.isEmpty(getControllerBean().getPort())) {
            return "1337";
        }
        return getControllerBean().getPort();
    }

    public String getControllerBind() {
        if (getControllerBean() == null || StringUtils.isEmpty(getControllerBean().getBind())) {
            return "0.0.0.0";
        }
        return getControllerBean().getBind();
    }

    public String getControllerLogFile() {
        if (getControllerBean() == null || StringUtils.isEmpty(getControllerBean().getLogFile())) {
            return "controller.log";
        }
        return getControllerBean().getLogFile();
    }

    public String getControllerLogLevel() {
        if (getControllerBean() == null || StringUtils.isEmpty(getControllerBean().getLogLevel())) {
            return "error";
        }
        return getControllerBean().getLogLevel();
    }

    public String getControllerTimeout() {
        if (getControllerBean() == null || StringUtils.isEmpty(getControllerBean().getAgentsCheckinTimeout())) {
            return "15";
        }
        return getControllerBean().getAgentsCheckinTimeout();
    }

    public String getControllerVersion() {
        return LeroyUtils.getControllerVersion(getLeroyhome());
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

        String requesturl = req.getOriginalRequestURI();
        JSONObject json = null;
        try {
            json = req.getSubmittedForm();
            JSONObject nodeproperties = json.getJSONObject("nodeProperties");
            if (nodeproperties.containsKey("org-jenkins-plugins-leroy-LeroyNodeProperty")) {
                Jenkins jenkins = Jenkins.getInstance();
                Computer[] computers = jenkins.getComputers();

                for (int i = 0; i < computers.length; i++) {
                    EnvVars envs = null;
                    envs = computers[i].buildEnvironment(TaskListener.NULL);

                    String name = computers[i].getName();

                        // TODO remove check for single LEROY_HOME
//                    if (computers[i].getName() == "" && envs.containsKey(Constants.IS_LEROY_NODE) && !requesturl.contains("master")) {
//                        throw new Descriptor.FormException("There cannot be more that one leroy node", "");
//                    } else if (computers[i].getName() != "" && envs.containsKey(Constants.IS_LEROY_NODE) && !requesturl.contains(computers[i].getName())) {
//                        throw new Descriptor.FormException("There cannot be more that one leroy node", "");
//                    }

                }
            }
        } catch (ServletException ex) {
            Logger.getLogger(LeroyNodeProperty.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LeroyNodeProperty.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(LeroyNodeProperty.class.getName()).log(Level.SEVERE, null, ex);
        }
        return super.reconfigure(req, form);
    }

    /**
     * @return internal address of the slave hosting this node or 127.0.0.1 if cannot define it
     */
    public String getDefaultControllerAddress() {
        String defautlAddr = "127.0.0.1";
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            if (localhost != null) {
                defautlAddr = localhost.getHostAddress();
            }
        } catch (Exception e) {
            Logger.getLogger(LeroyNodeProperty.class.getName()).log(Level.SEVERE, "Cannot get the address of the local host", e);
        }
        return defautlAddr;
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

        public FormValidation doAddAgent(@QueryParameter("leroyhome") final String leroyHome,
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

            try {
                Launcher launcher = Hudson.getInstance().createLauncher(TaskListener.NULL);
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
                    return FormValidation.error("Cannot add agent " + output);
                }

                // if we need to install it via ssh
                if ("true".equalsIgnoreCase(sshInstall)) {
                    String bundleName = agentName + "-" + agentPlatform + ".zip";
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
                    return FormValidation.ok("Success");
                }
                return FormValidation.error("Failed to install agent via SSH" + output);

            } catch (Exception e) {
                return FormValidation.error("Client error : " + e.getMessage());
            }
        }


        public FormValidation doUpdateController(@QueryParameter("architecture") String architecture,
                                            @QueryParameter("leroyhome") String leroyhome) {
            //get update file from server
            try {
                Update update = XMLParser.readUpdate();
                if (update != null) {
                    // get controller version
                    String controllerVersion = LeroyUtils.getControllerVersion(leroyhome);
                    if (controllerVersion.equalsIgnoreCase(String.valueOf(update.getVersion()))) {
                        return FormValidation.warning("Controller has last version: '" + update.getVersion() + "'");
                    }
                    String binary = update.getBinaries().get(architecture);
                    if (binary != null) {
                        File archive = LeroyUtils.downloadFile(binary);
                        // create leroyHome if necessary
                        File leroyHomeFile = new File(leroyhome);
                        if (!leroyHomeFile.exists()) {
                            leroyHomeFile.mkdirs();
                        }
                        LeroyUtils.unpack(archive, new File(leroyhome));
                    }
                } else {
                    return FormValidation.error("Cannot get update from server. Please try later.");
                }
            } catch (Exception e) {
                return FormValidation.error(e, "Error while updating the controller"); // temporatily show stacktrace on UI
            }
            return FormValidation.ok("Controller is updated successfully");
        }

        public FormValidation doCheckLeroyhome(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please provide a path for leroy plugin");
            } else if (!canWrite(value)) {
                return FormValidation.error("Cannot write to folder. Please check if folder exists/user has write permissions on this folder");
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

        public String getHelpPage() {
            // yes, I know this is a hack.
            ComputerSet object = Stapler.getCurrentRequest().findAncestorObject(ComputerSet.class);

            if (object != null) {
                // we're on a node configuration page, show show that help page
                return "/help/system-config/nodeEnvironmentVariables.html";
            } else {
                // show the help for the global config page
                return "/help/system-config/globalEnvironmentVariables.html";
            }
        }


        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        private boolean canWrite(String path) {
            File f = new File(path);
            return f.canWrite();
        }
    }

}
