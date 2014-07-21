package org.jenkins.plugins.leroy.util;

import com.trilead.ssh2.util.IOUtils;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.*;
import hudson.triggers.SCMTrigger;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.SystemUtils;
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

public class LeroyUtils {

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
     * @return Leroy home directory
     * @throws InterruptedException
     * @throws IOException
     */
    @Deprecated
    public static String getLeroyHome() throws InterruptedException, IOException {
        Jenkins jenkins = Jenkins.getInstance();
        Computer[] computers = jenkins.getComputers();

        for (int i = 0; i < computers.length; i++) {
            EnvVars envs = computers[i].buildEnvironment(TaskListener.NULL);
            if (envs.containsKey(Constants.IS_LEROY_NODE)) {
                return envs.get(Constants.LEROY_HOME);
            }
        }
        return null;
    }

    public static String getLeroyHome(Launcher launcher) throws InterruptedException, IOException {
        EnvVars envs = launcher.getComputer().buildEnvironment(TaskListener.NULL);
        if (envs.containsKey(Constants.IS_LEROY_NODE)) {
            return envs.get(Constants.LEROY_HOME);
        }
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

    public static String getEnvironmentsXml() throws InterruptedException, IOException {
        return getLeroyHome() + "/environments.xml";
    }

    public static String getWorkflowsFolder() throws InterruptedException, IOException {
        return getLeroyHome() + "/workflows/";
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


    public static String runController(String dir, Map<String, String> envs, List<String> params) throws LeroyException {

        String[] parList = new String[params.size()];
        parList = params.toArray(parList);
        return runController(dir, envs, parList);

    }
    // TODO currently forced to return only last line from controller's output
    // TODO it caused by a bug I encounter
    public static String runController(String dir, Map<String, String> envs, String[] parameters) throws LeroyException {

        String cmd = "";
        String result = "";
        try {

            if (SystemUtils.IS_OS_UNIX || SystemUtils.IS_OS_LINUX) {
                cmd += "sh ";
            }
            cmd += dir + File.separator +"controller";
            if (SystemUtils.IS_OS_WINDOWS) {
                cmd += ".exe";
            }
            String[] command = new String[parameters.length +1];
            command[0] = cmd;
            for (int i = 0; i <parameters.length; i++) {
                command[i+1] = parameters[i];
            }

            ProcessBuilder pb = new ProcessBuilder();
            pb.environment().putAll(envs);
            pb.command(command);

            Process p = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ( (line = br.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
                result = line;
            }
        } catch (Exception e) {
            throw new LeroyException("Cannot run command: '" + cmd + "'", e);
        }
        return result;

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


    public static String getScriptsHome() {
        return Hudson.getInstance().getRootDir() + "/plugins/leroy/scripts/";
    }


    public static void main(String[] args) {
        try {
            String res = runController("C:\\Users\\night\\Downloads\\leroy_Win64", null, new String[]{"-v"});
            System.out.println(res);
        } catch (LeroyException e) {
            e.printStackTrace();
        }

    }


}
