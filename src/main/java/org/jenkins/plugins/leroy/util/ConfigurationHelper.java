package org.jenkins.plugins.leroy.util;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.jenkins.plugins.leroy.beans.Configuration;
import org.jenkins.plugins.leroy.beans.Environment;
import org.jenkins.plugins.leroy.beans.Workflow;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by dzmitry_bahdanovich on 23.06.14.
 */
public class ConfigurationHelper {

    private String configFilename;
    private Configuration config = new Configuration();

    public ConfigurationHelper(String filename) throws IOException {
        this.configFilename = filename;
        createConfig();
    }

    private void createConfig() throws IOException {
        File config = new File(configFilename);
        if (!config.exists()) {
            flush(); // create new empty config
        } else {
            load(); // else load existing
        }
    }

    public void load() throws IOException {
        String content = FileUtils.readFileToString(new File(configFilename));
        Gson gson = new Gson();
        config = gson.fromJson(content, Configuration.class);
    }

    public void flush() throws IOException {
        Gson gson = new Gson();
        String content = gson.toJson(config);
        FileUtils.writeStringToFile(new File(configFilename), content);
    }

    public Environment findEnvironment(String name) {
        List<Environment> envs = config.getEnvs();
        for (Environment env : envs) {
            if (name.equals(env.getName())) {
                return env;
            }
        }
        return null;
    }

    public void add(Environment env) {
        config.getEnvs().add(env);
    }

    public Workflow findWorkflow(String name) {
        List<Workflow> wfs = config.getWorkflows();
        for (Workflow wf : wfs) {
            if (name.equals(wf.getName())) {
                return wf;
            }
        }
        return null;
    }

    public void add(Workflow wf) {
        config.getWorkflows().add(wf);
    }

    public List<Workflow> getWorkflows() {
        return new ArrayList<Workflow>(config.getWorkflows());
    }

    public void setEnabledEnvironments(List<String> enabled) {
        if (isEmpty(enabled)) {
            return;
        }
        Set<String> enabledSet = new HashSet<String>(enabled);
        for (Environment env : config.getEnvs()) {
            if (enabledSet.contains(env.getName())) {
                env.setEnabled(true);
            } else {
                env.setEnabled(false);
            }
        }
    }

    public void setDefaultEnvironment(String name) {
        for (Environment env : config.getEnvs()) {
            if (name.equals(env.getName())) {
                env.setDef(true);
            } else {
                env.setDef(false);
            }
        }
    }

    public void setAutodeployEnvironments(List<String> names) {
//        if (isEmpty(names)) {
//            return;
//        }
        Set<String> namesSet = new HashSet<String>(names);
        for (Environment env : config.getEnvs()) {
            if (namesSet.contains(env.getName())) {
                env.setAutodeploy(true);
            } else {
                env.setAutodeploy(false);
            }
        }
    }

    public void setDefaultWorkflow(String name) {
        for (Workflow wf : config.getWorkflows()) {
            if (name.equals(wf.getName())) {
                wf.setDef(true);
            } else {
                wf.setDef(false);
            }
        }
    }

    public Workflow getDefaultWorkflow() {
        for (Workflow wf : config.getWorkflows()) {
            if (wf.isDef()) {
                return wf;
            }
        }
        return null;
    }

    public void putUsedConfigs(Map<String, String> envToConfigMap) {

        for (Map.Entry<String, String> entry : envToConfigMap.entrySet()) {
            Environment env = findEnvironment(entry.getKey());
            if (env != null) {
                env.setUsedConfig(entry.getValue());
            }
        }

    }

    public List<Environment> getEnabledEnvironments() {
        List<Environment> envs = new ArrayList<Environment>();
        for (Environment env : config.getEnvs()) {
            if (env.isEnabled()) {
                envs.add(env);
            }
        }
        return envs;
    }

    public Environment getDefaultEnvironment() {
        for (Environment env : config.getEnvs()) {
            if (env.isDef()) {
                return env;
            }
        }
        return null;
    }


    public Environment getDefaultEnabledAutodeploy() {
        for (Environment env : config.getEnvs()) {
            if (env.isDef() && env.isAutodeploy() && env.isEnabled()) {
                return env;
            }
        }
        return null;
    }

    public List<Environment> getEnabledAutodeploy() {
        List<Environment> result = new ArrayList<Environment>();
        for (Environment env : config.getEnvs()) {
            if (env.isAutodeploy() && env.isEnabled()) {
                result.add(env);
            }
        }
        return result;
    }

    private boolean isEmpty(Collection c) {
        if (c == null || c.size() == 0) {
            return true;
        }
        return false;
    }

    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(config);
    }

}
