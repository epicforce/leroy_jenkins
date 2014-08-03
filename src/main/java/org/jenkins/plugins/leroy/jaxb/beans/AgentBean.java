package org.jenkins.plugins.leroy.jaxb.beans;

import org.kohsuke.stapler.DataBoundConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Created by Dzmitry Bahdanovich on 06.07.14.
 */
@XmlRootElement(name="agent")
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentBean implements Serializable {

    private static final long serialVersionUID = -2910800743222127321L;
    @XmlAttribute
    private String name;

    @XmlAttribute
    private String lockerPath;

    @XmlAttribute
    private String runAsUser;

    @XmlAttribute
    private String shell;

    @XmlAttribute
    private String temporaryDirectory;

    public AgentBean() {
    }

    @DataBoundConstructor
    public AgentBean(String name, String lockerPath, String runAsUser, String shell, String temporaryDirectory) {
        this.name = name;
        this.lockerPath = lockerPath;
        this.runAsUser = runAsUser;
        this.shell = shell;
        this.temporaryDirectory = temporaryDirectory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLockerPath() {
        return lockerPath;
    }

    public void setLockerPath(String lockerPath) {
        this.lockerPath = lockerPath;
    }

    public String getRunAsUser() {
        return runAsUser;
    }

    public void setRunAsUser(String runAsUser) {
        this.runAsUser = runAsUser;
    }

    public String getShell() {
        return shell;
    }

    public void setShell(String shell) {
        this.shell = shell;
    }

    public String getTemporaryDirectory() {
        return temporaryDirectory;
    }

    public void setTemporaryDirectory(String temporaryDirectory) {
        this.temporaryDirectory = temporaryDirectory;
    }
}
