package org.jenkins.plugins.leroy.beans;

import java.util.List;

/**
 * Created by dzmitry_bahdanovich on 22.06.14.
 */
public class LeroyJenkinsJobState {

    private List<String> workflows;
    private List<String> environments;
    private String defaultEnvironment;
    private List<String> enabledEnvironments;
    private List<String> autoDeployEnvironments;

    public LeroyJenkinsJobState(List<String> workflows, List<String> environments, String defaultEnvironment, List<String> enabledEnvironments, List<String> autoDeployEnvironments) {
        this.workflows = workflows;
        this.environments = environments;
        this.defaultEnvironment = defaultEnvironment;
        this.enabledEnvironments = enabledEnvironments;
        this.autoDeployEnvironments = autoDeployEnvironments;
    }


    public List<String> getAutoDeployEnvironments() {
        return autoDeployEnvironments;
    }

    public void setAutoDeployEnvironments(List<String> autoDeployEnvironments) {
        this.autoDeployEnvironments = autoDeployEnvironments;
    }

    public List<String> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<String> workflows) {
        this.workflows = workflows;
    }

    public List<String> getEnvironments() {
        return environments;
    }

    public void setEnvironments(List<String> environments) {
        this.environments = environments;
    }

    public String getDefaultEnvironment() {
        return defaultEnvironment;
    }

    public void setDefaultEnvironment(String defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
    }

    public List<String> getEnabledEnvironments() {
        return enabledEnvironments;
    }

    public void setEnabledEnvironments(List<String> enabledEnvironments) {
        this.enabledEnvironments = enabledEnvironments;
    }
}
