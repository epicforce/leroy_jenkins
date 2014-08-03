package org.jenkins.plugins.leroy.util;

import hudson.*;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.slaves.NodeProperty;
import hudson.triggers.SCMTrigger;
import jenkins.model.Jenkins;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.leroy.LeroyException;
import org.jenkins.plugins.leroy.LeroyNodeProperty;
import org.jenkins.plugins.leroy.jaxb.beans.AgentBean;
import org.jenkins.plugins.leroy.jaxb.beans.ControllerBean;
import org.jenkins.plugins.leroy.jaxb.beans.EnvironmentBean;
import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LeroyUtils {
    private static final Logger LOGGER = Logger.getLogger(LeroyUtils.class.getName());

    public static final String USER_ID_CAUSE_CLASS_NAME = "hudson.model.Cause$UserIdCause";
    public static final String SCM_TRIGGER = "SCMTrigger";

    public static String getUserRunTheBuild(Run build) {

        // If build has been triggered form an upstream build, get UserCause from there to set user build variables
        Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause) build.getCause(Cause.UpstreamCause.class);
        if (upstreamCause != null) {
            Job job = Jenkins.getInstance().getItemByFullName(upstreamCause.getUpstreamProject(), Job.class);
            if (job != null) {
                Run upstream = job.getBuildByNumber(upstreamCause.getUpstreamBuild());
                if (upstream != null) {
                    getUserRunTheBuild(upstream);
                }
            }
        }

        // set BUILD_USER_NAME to fixed value if the build was triggered by a change in the scm
        SCMTrigger.SCMTriggerCause scmTriggerCause = (SCMTrigger.SCMTriggerCause) build.getCause(SCMTrigger.SCMTriggerCause.class);
        if (scmTriggerCause != null) {
            return SCM_TRIGGER;
        }

        // Use UserIdCause.class if it exists in the system (should be starting from b1.427 of jenkins).
        if (isClassExists(USER_ID_CAUSE_CLASS_NAME)) {
            /* Try to use UserIdCause to get & set jenkins user build variables */
            Cause.UserIdCause userIdCause = (Cause.UserIdCause) build.getCause(Cause.UserIdCause.class);
            if (userIdCause != null) {
                return userIdCause.getUserId();
            }
        }

        // Try to use deprecated UserCause to get & set jenkins user build variables
        Cause.UserCause userCause = (Cause.UserCause) build.getCause(Cause.UserCause.class);
        if (userCause != null) {
            return userCause.getUserName();
        }

        return "unknown";
    }

    public static boolean isClassExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Return LEROY_HOME of a node with a given name
     * @param nodeName
     * @return
     */
    public static String getLeroyHome(String nodeName) {
        Node node = findNodeByName(nodeName);
        String result = "";
        if (node != null) {
            for (NodeProperty prop : node.getNodeProperties()) {
                if (prop instanceof LeroyNodeProperty) {
                    result = ((LeroyNodeProperty) prop).getLeroyhome();
                    break;
                }
            }
        }
        return result;
    }

    public static String getLeroyHome(Executor exec) throws IOException, InterruptedException {
        if (exec == null) {
            LOGGER.log(Level.SEVERE, "Cannot get LEROY_HOME as executor is null");
            return null;
        }
        Computer comp = exec.getOwner();
        if (comp == null) {
            LOGGER.log(Level.SEVERE, "Cannot get LEROY_HOME as executor owner is null");
            return null;
        }
        EnvVars envs = comp.buildEnvironment(TaskListener.NULL);
        if (envs.containsKey(Constants.IS_LEROY_NODE)) {
            String leroyHome = envs.get(Constants.LEROY_HOME);
            if (StringUtils.isEmpty(leroyHome)) {
                LOGGER.log(Level.SEVERE, "LEROY_HOME is empty at " + comp.getDisplayName());
            }
            LOGGER.log(Level.FINE, "LEROY_HOME: " + leroyHome);
            return leroyHome;
        }
        LOGGER.log(Level.SEVERE, "Node " + comp.getDisplayName() + " is not a Leroy Node");
        return null;
    }

    public static List<Computer> getLeroyNodes() throws IOException, InterruptedException {
        Jenkins jenkins = Jenkins.getInstance();
        Computer[] computers = jenkins.getComputers();
        List<Computer> leroyNodes = new ArrayList<Computer>();
        for (int i = 0; i < computers.length; i++) {
            EnvVars envs = computers[i].buildEnvironment(TaskListener.NULL);
            if (envs.containsKey(Constants.IS_LEROY_NODE)) {
                leroyNodes.add(computers[i]);
            }
        }
        return leroyNodes;
    }

    /**
     * Return jenkins node by it's name
     * @param name
     * @return
     */
    public static Node findNodeByName(String name) {
        if (name == null) {
            return null;
        }
        Jenkins jenkins = Jenkins.getInstance();
        Computer[] computers = jenkins.getComputers();
        // master node is a special case
        if (Constants.MASTER_NODE.equalsIgnoreCase(name)) {
            for (Computer c : computers) {
                if (c instanceof Hudson.MasterComputer) {
                    return c.getNode();
                }
            }
        } else {
            // this is not a master node
            for (Computer c : computers) {
                if (name.equalsIgnoreCase(c.getName())) {
                    return c.getNode();
                }
            }
        }
        return null;
    }

    public static boolean isWorkflow(File file) throws LeroyException {

        if (file != null && file.exists() && file.getName().toLowerCase().endsWith(".xml")) {
            try {
                Document doc = XMLHandler.loadXMLFile(file.getAbsolutePath());
                if (doc != null && XMLHandler.countNodes(doc, "workflow") > 0) {
                    return true;
                }
            } catch (Exception e) {
                throw new LeroyException("Error parsing XML file", e);
            }
        }
        return false;
    }

    public static String runController(String dir, Map<String, String> envs, String[] parameters) throws LeroyException {
        try {
            String[] command = new String[parameters.length +1];
            command[0] = dir + "/controller";
            for (int i = 0; i <parameters.length; i++) {
                command[i+1] = parameters[i];
            }
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(dir));
            if (envs != null) {
                pb.environment().putAll(envs);
            }

            Process p = pb.command(command).start();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line ;
            while ( (line = br.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new LeroyException("Cannot run controller", e);
        }
    }

    public static String runController(final FilePath leroyHome, final String[] parameters) throws IOException, InterruptedException {
        String result = leroyHome.act(new FilePath.FileCallable<String>() {
            private static final long serialVersionUID = 1L;
            public String invoke(File leroyHomeDir, VirtualChannel channel) throws IOException, InterruptedException {
                Map envs = Functions.getEnvVars();
                String[] command = new String[parameters.length +1];
                command[0] = new FilePath(leroyHome, "controller").toString();
                for (int i = 0; i <parameters.length; i++) {
                    command[i+1] = parameters[i];
                }
                ProcessBuilder pb = new ProcessBuilder();
                pb.directory(new File(leroyHomeDir.toString()));
                if (envs != null) {
                    pb.environment().putAll(envs);
                }

                Process p = pb.command(command).start();

                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String line ;
                while ( (line = br.readLine()) != null) {
                    builder.append(line);
                    builder.append(System.getProperty("line.separator"));
                }
                int exitval = p.exitValue();
                return exitval + "\n" + builder.toString();
            }
        });
        return result;
    }

    public static int getExitCode(String execResult) {
        String exitCode = execResult.split("\n")[0];
        int val = 0;
        try {
            val = Integer.valueOf(exitCode);
        } catch (Exception e) {
            return -1;
        }
        return val;
    }

    public static String getProcessOutput(String execResult) {
        return execResult.substring(execResult.indexOf("\n") + 1);
    }


    /**
     * Downloads a file from specific url to temp folder on specific node
     * @param url
     * @param node
     * @return FilePath object pointing to downloaded file
     * @throws LeroyException
     */
    public static FilePath downloadFile(String url, Node node) throws LeroyException {
        if (node == null || url == null || url.isEmpty()) {
            return null;
        }
        try {
            URL fileUrl = new URL(url);
            String suffix = ".zip";
            if (url.toLowerCase().endsWith(".tgz")) {
                suffix = ".tgz";
            } else if (url.toLowerCase().endsWith(".tar.gz")) {
                suffix = ".tar.gz";
            }
            // create temp file on a node
            FilePath target = new FilePath(node.getChannel(), "");
            target = target.createTextTempFile("tempfile", suffix, "", false);
            // download from url
            target.copyFrom(fileUrl.openStream());
            return target;
        } catch (Exception e) {
            throw new LeroyException("Cannot download from '" + url + "' to '" + node.getDisplayName() + "'", e);
        }
    }

    public static void unpack(FilePath source, FilePath target) throws LeroyException {
        try {
           if (source.getName().toLowerCase().endsWith(".zip")) {
               source.unzip(target);
           } else {
               source.untar(target, FilePath.TarCompression.GZIP);
           }
        } catch (Exception e) {
            throw new LeroyException("Cannot extract from '" + source.getName() + "' to '" + target.getName() + "'", e);
        }
    }

    public static String getControllerVersion(Node node, FilePath leroyHome) {
        String res = "N/A";
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Launcher launcher = node.createLauncher(TaskListener.NULL);
            List<String> cmds = new ArrayList<String>();
            cmds.add(leroyHome + "/controller");
            cmds.add("--version");
            int returnCode = launcher.launch().pwd(leroyHome).envs(Functions.getEnvVars()).cmds(cmds).stdout(output).join();
            if (returnCode == 0) {
                res = output.toString();
            }
        } catch (Exception e) {
            // just omit
        }
        return res;
    }

    public static String getHostAddress(FilePath path) throws LeroyException {
        try {
            String result = path.act(new FilePath.FileCallable<String>() {
                @Override
                public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                    return InetAddress.getLocalHost().getHostAddress();
                }
            });
            return result;
        } catch (Exception e) {
            throw new LeroyException("Cannot get ip address of the node." , e);
        }
    }

    public static void main(String[] args) {
        try {
            String res = runController("C:/leroy", null, new String[]{"--version"});
            System.out.println(res);
        } catch (LeroyException e) {
            e.printStackTrace();
        }
    }

    public static boolean canWrite(FilePath path) throws IOException, InterruptedException {
        Boolean result = path.act(new FilePath.FileCallable<Boolean>() {
            private static final long serialVersionUID = 1L;
            public Boolean invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                return f.canWrite();
            }
        });
        return result;
    }

    public static Map<String,String> listFiles(FilePath dir, String include, String exclude) throws IOException, InterruptedException {
        return dir.act(new ListFiles(include, exclude));
    }

    private static final class ListFiles implements FilePath.FileCallable<Map<String,String>> {
        private static final long serialVersionUID = 1;
        private final String includes, excludes;
        ListFiles(String includes, String excludes) {
            this.includes = includes;
            this.excludes = excludes;
        }
        @Override public Map<String,String> invoke(File basedir, VirtualChannel channel) throws IOException, InterruptedException {
            Map<String,String> r = new HashMap<String,String>();
            for (String f : Util.createFileSet(basedir, includes, excludes).getDirectoryScanner().getIncludedFiles()) {
                f = f.replace(File.separatorChar, '/');
                r.put(f, f);
            }
            return r;
        }
    }

    public static boolean isNodeAvailable(Node node) {
        if (node == null || node.getChannel() == null) {
            return false;
        }
        return true;
    }

    /**
     * This class is used to save environments remotely to environments.xml in a given directory
     */
    public static class SaveEnvironments implements FilePath.FileCallable<Void> {
        private static final long serialVersionUID = 1;

        private List<EnvironmentBean> envs;

        public SaveEnvironments(List<EnvironmentBean> envs) {
            this.envs = envs;
        }

        @Override
        public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            File envsXml = new File(f, "environments.xml");
            XMLParser.saveEnvironments(envs, envsXml.getAbsolutePath());
            return null;
        }
    }

    /**
     * This class is used to save controller remotely to controller.xml in a given directory
     */
    public static class SaveController implements FilePath.FileCallable<Void> {
        private static final long serialVersionUID = 1;

        ControllerBean controller;

        public SaveController(ControllerBean controller) {
            this.controller = controller;
        }

        @Override
        public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            File controllerXml = new File(f, "controller.xml");
            XMLParser.saveController(controller, controllerXml.getAbsolutePath());
            return null;
        }
    }

    /**
     * This class is used to get environments from remote environments.xml
     */
    public static class ReadEnvironments implements FilePath.FileCallable<List<EnvironmentBean>> {
        @Override
        public List<EnvironmentBean> invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            File environemntsXml = new File(f, "environments.xml");
            List<EnvironmentBean> environments = XMLParser.readEnvironments(environemntsXml.getAbsolutePath());
            return environments;
        }
    }

    /**
     * This class is used to get agents from remote agents.xml
     */
    public static class ReadAgents implements FilePath.FileCallable<List<AgentBean>> {
        @Override
        public List<AgentBean> invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            File agentsXml = new File(f, "agents.xml");
            List<AgentBean> agents = XMLParser.readAgents(agentsXml.getAbsolutePath());
            return agents;
        }
    }

}
