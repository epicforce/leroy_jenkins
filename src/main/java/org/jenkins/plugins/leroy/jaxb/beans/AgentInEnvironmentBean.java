package org.jenkins.plugins.leroy.jaxb.beans;

import org.kohsuke.stapler.DataBoundConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Created by Dzmitry Bahdanovich on 19.07.14.
 */
@XmlRootElement(name = "agent")
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentInEnvironmentBean implements Serializable {

    private static final long serialVersionUID = -399112613849556948L;
    @XmlAttribute
    private String name;

    @XmlAttribute
    private String roles = "";

    @XmlAttribute
    private String id;

    @DataBoundConstructor
    public AgentInEnvironmentBean(String name, String roles, String id) {
        this.name = name;
        this.roles = roles;
        this.id = id;
    }

    public AgentInEnvironmentBean() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}