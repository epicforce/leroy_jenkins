package org.jenkins.plugins.leroy.jaxb.beans;

import org.kohsuke.stapler.DataBoundConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

/**
 * Created by Dzmitry Bahdanovich on 04.07.14.
 */

@XmlRootElement(name="environment")
@XmlAccessorType(XmlAccessType.FIELD)
public class EnvironmentBean implements Serializable {

    private static final long serialVersionUID = -5330867106549771051L;

    @XmlAttribute
    private String name;

    private AgentsInEnvironmentBean agents;

    @DataBoundConstructor
    public EnvironmentBean(String name, List<AgentInEnvironmentBean> agents) {
        this.name = name;
        this.agents = new AgentsInEnvironmentBean(agents);
    }

    public EnvironmentBean() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AgentInEnvironmentBean> getAgents() {
        return agents.getAgents();
    }

    public void setAgents(List<AgentInEnvironmentBean> agents) {
        this.agents = new AgentsInEnvironmentBean(agents);
    }
}
