package org.jenkins.plugins.leroy.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dzmitry_bahdanovich on 24.06.14.
 */
public class Configuration {

    private List<Environment> envs;

    private List<Workflow> workflows;

    public Configuration() {
        envs = new ArrayList<Environment>();
        workflows = new ArrayList<Workflow>();
    }

    public Configuration(List<Environment> envs, List<Workflow> workflows) {

        this.envs = envs;
        this.workflows = workflows;
    }

    public List<Environment> getEnvs() {

        return envs;
    }

    public void setEnvs(List<Environment> envs) {
        this.envs = envs;
    }

    public List<Workflow> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<Workflow> workflows) {
        this.workflows = workflows;
    }
}
