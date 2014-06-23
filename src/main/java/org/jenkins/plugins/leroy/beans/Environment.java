package org.jenkins.plugins.leroy.beans;

/**
 * Created by night on 24.06.14.
 */
public class Environment {

    private String name;
    private boolean def;
    private boolean enabled = true;
    private boolean autodeploy;
    private String usedConfig;

    public Environment() {
    }

    public Environment(String name,  boolean def, boolean enabled, boolean autodeploy, String usedConfig) {
        this.name = name;
        this.def = def;
        this.enabled = enabled;
        this.autodeploy = autodeploy;
        this.usedConfig = usedConfig;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDef() {
        return def;
    }

    public void setDef(boolean def) {
        this.def = def;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutodeploy() {
        return autodeploy;
    }

    public void setAutodeploy(boolean autodeploy) {
        this.autodeploy = autodeploy;
    }

    public String getUsedConfig() {
        return usedConfig;
    }

    public void setUsedConfig(String usedConfig) {
        this.usedConfig = usedConfig;
    }
}
