package org.jenkins.plugins.leroy.jaxb.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Dzmitry Bahdanovich on 29.06.14.
 */
@XmlRootElement(name="controller")
@XmlAccessorType(XmlAccessType.FIELD)
public class ControllerBean {

    /**
     * The maximum time (in seconds) that the controller should wait for all agents to connect.
     */
    @XmlAttribute
    private String agentsCheckinTimeout;

    /**
     * The hostname that agents will use to connect to the controller.
     */
    @XmlAttribute
    private String host;

    /**
     *  The IP address the controller binds to. Leave blank to bind to all.
     */
    @XmlAttribute
    private String bind;

    /**
     * The controller log file. Default is standard output.
     */
    @XmlAttribute
    private String logFile;

    /**
     * The controller log level. This may be “none”, “debug”, “info”, “warning”, or “error”. Default is “warning”.
     */
    @XmlAttribute
    private String logLevel;

    /**
     * The TCP port that the controller listens on.
     */
    @XmlAttribute
    private String port;

    public String getAgentsCheckinTimeout() {
        return agentsCheckinTimeout;
    }

    public void setAgentsCheckinTimeout(String agentsCheckinTimeout) {
        this.agentsCheckinTimeout = agentsCheckinTimeout;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getBind() {
        return bind;
    }

    public void setBind(String bind) {
        this.bind = bind;
    }

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}