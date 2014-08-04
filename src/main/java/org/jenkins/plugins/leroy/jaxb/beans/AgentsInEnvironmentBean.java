package org.jenkins.plugins.leroy.jaxb.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dzmitry Bahdanovich on 19.07.14.
 */
@XmlRootElement(name = "agents")
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentsInEnvironmentBean implements Serializable {

    private static final long serialVersionUID = 1024177132823035785L;

    @XmlElement(name = "agent", type = AgentInEnvironmentBean.class)
    private List<AgentInEnvironmentBean> agents = new ArrayList<AgentInEnvironmentBean>();

    public AgentsInEnvironmentBean(List<AgentInEnvironmentBean> agents) {
        this.agents = agents;
    }

    public AgentsInEnvironmentBean() {
    }

    public List<AgentInEnvironmentBean> getAgents() {
        return agents;
    }

    public void setAgents(List<AgentInEnvironmentBean> agents) {
        this.agents = agents;
    }
}
