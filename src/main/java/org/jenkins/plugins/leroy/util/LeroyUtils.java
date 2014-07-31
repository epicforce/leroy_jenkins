package org.jenkins.plugins.leroy.util;

import com.trilead.ssh2.util.IOUtils;
import hudson.EnvVars;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.*;
import hudson.slaves.NodeProperty;
import hudson.triggers.SCMTrigger;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.leroy.LeroyException;
import org.jenkins.plugins.leroy.LeroyNodeProperty;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.w3c.dom.Document;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LeroyUtils {
    private static final Logger LOGGER = Logger.getLogger(LeroyUtils.class.getName());

    public static final String USER_ID_CAUSE_CLASS_NAME = "hudson.model.Cause$UserIdCause";
    public static final String SCM_TRIGGER = "SCMTrigger";

    public static boolean isLeroyNode() {
        Jenkins jenkins = Jenkins.getInstance();
        DescribableList nodeProperties = jenkins.getNodeProperties();
        boolean result = false;
        for (Object property : nodeProperties) {
            if (property instanceof LeroyNodeProperty) {
                result = true;
            }
        }
        return result;
    }

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

    public static void copyDirectoryQuietly(File source, File dest) throws IOException {
        if (source.exists() && source.canRead()) {
            IOFileFilter filter = FileFilterUtils.orFileFilter(DirectoryFileFilter.DIRECTORY, FileFileFilter.FILE);
            FileUtils.copyDirectory(source, dest, FileFilterUtils.makeSVNAware(filter));
        }
    }

    public static void copyFileToDirectoryQuietly(File source, File dest) throws IOException {
        if (source.exists() && source.canRead()) {
            FileUtils.copyFileToDirectory(source, dest);
        }
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

    public static File downloadFile(String url) throws LeroyException {
        File f = null;
        FileOutputStream fos = null;
        try {
            URL website = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            String suffix = ".zip";
            if (url.toLowerCase().endsWith(".tgz")) {
                suffix = ".tgz";
            } else if (url.toLowerCase().endsWith(".tar.gz")) {
                suffix = ".tar.gz";
            }
            f = File.createTempFile("leroyupdate", suffix);
            fos = new FileOutputStream(f);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (Exception e) {
            throw new LeroyException("Cannot upload file : '" + url + "'", e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
        return f;
    }

    public static void unpack(File source, File target) throws LeroyException {
        try {
            Archiver archiver = ArchiverFactory.createArchiver(source);
            archiver.extract(source, target);
        } catch (Exception e) {
            throw new LeroyException("Cannot extract from '" + source.toString() + "' to '" + target.toString() + "'");
        }
    }

    public static String getControllerVersion(String leroyHome) {
        String res = "N/A";
        try {
            res = LeroyUtils.runController(leroyHome, Functions.getEnvVars(), new String[]{"--version"});
            res = String.valueOf(Integer.valueOf(res.trim())); // is number?
        } catch (Exception e) {
            // just omit
        }
        return res;
    }

    public static void main(String[] args) {
        try {
            String res = runController("C:/leroy", null, new String[]{"--version"});
            System.out.println(res);
        } catch (LeroyException e) {
            e.printStackTrace();
        }
    }

}
