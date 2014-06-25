package org.jenkins.plugins.leroy.beans;

/**
 * Created by dzmitry_bahdanovich on 24.06.14.
 */
public class Workflow {

    private String name;

    private boolean def;

    public Workflow() {
    }

    public Workflow(String name, boolean def) {
        this.name = name;
        this.def = def;
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
}
