package org.jenkins.plugins.leroy.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.leroy.beans.Update;
import org.jenkins.plugins.leroy.jaxb.JaxbUtils;
import org.jenkins.plugins.leroy.jaxb.ListWrapper;
import org.jenkins.plugins.leroy.jaxb.beans.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XMLParser {

    // update.xml tags
    public static final String UP_BINARY_TAG_SUFF = "_binary";
    public static final String UP_VERSION_TAG = "version";
    public static final String UP_UPDATE_TAG = "update";

    //controller.xml tags
    public static final String CO_CONTROLLER_TAG = "controller";
    public static final String CO_TIMEOUT_ATTR = "agentsCheckinTimeout";
    public static final String CO_HOST_ATTR = "host";
    public static final String CO_BIND_ATTR = "bind";
    public static final String CO_PORT_ATTR = "port";
    public static final String CO_LOGFILE_ATTR = "logFile";
    public static final String CO_LOGLEVEL_ATTR = "logLevel";


    public static void main(String argv[]) {
        Update up = readUpdate();
        readController("C:\\leroy\\controller.xml");

    }

    public static List<String> getEnvironment(File fXmlFile) {

        try {
            List<String> agents = new ArrayList<String>();
            //File fXmlFile = new File("/Users/mkyong/staff.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("environment");

            for (int temp = 0; temp < nList.getLength(); temp++) {

                Node nNode = nList.item(temp);
                Element eElement = (Element) nNode;
                agents.add(eElement.getAttribute("name"));
            }
            return agents;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> addRoles(File fXmlFile, String agentname, String enviromentname, String rolename) {

        try {
            List<String> agents = new ArrayList<String>();
            //File fXmlFile = new File("/Users/mkyong/staff.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            //doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("environment");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                Element eElement = (Element) nNode;

                if (eElement.getAttribute("name").equalsIgnoreCase(enviromentname)) {
                    NodeList nList1 = eElement.getElementsByTagName("agent");
                    for (int i = 0; i < nList1.getLength(); i++) {
                        Node nNode1 = nList1.item(temp);
                        Element eElement1 = (Element) nNode1;
                        agents.add(eElement1.getAttribute("roles"));
                    }
                }
            }
            return agents;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Update readUpdate() {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        Update update = null;
        InputStream is = null;
        try {
            URL updateFile = new URL(Constants.UPDATE_XML);
            is = new BufferedInputStream(updateFile.openStream());
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            Node updateNode = XMLHandler.getSubNode(doc, UP_UPDATE_TAG);

            // get version
            Node versionNode = XMLHandler.getSubNode(updateNode, UP_VERSION_TAG);
            int version = Integer.valueOf(XMLHandler.getNodeValue(versionNode));

            // get binaries nodes
            Map<String, String> binariesMap = new LinkedHashMap<String, String>();
            String binaryRegex = "(.*)_binary";
            List<Node> binNodes = XMLHandler.getNodesByRegex(updateNode, binaryRegex);

            for (Node node : binNodes) {
                String nodeName = node.getNodeName();
                String name = nodeName.substring(0, nodeName.indexOf(UP_BINARY_TAG_SUFF));
                String url = XMLHandler.getNodeValue(node);
                binariesMap.put(name, url);
            }

            update = new Update(version, binariesMap);

        } catch (Exception e) {
            e.printStackTrace(); //TODO handle
        } finally {
            IOUtils.closeQuietly(is);
        }

        return update;
    }


//    public static Controller readController(String controllerXml) {
//        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder dBuilder;
//        Controller controller = new Controller();
//        try {
//            dBuilder = dbFactory.newDocumentBuilder();
//            Document doc = dBuilder.parse(controllerXml);
//            doc.getDocumentElement().normalize();
//
//            // get controller
//            Node contollerNode = XMLHandler.getSubNode(doc, CO_CONTROLLER_TAG);
//            if (contollerNode != null) {
//
//                Map<String, String> attrs = XMLHandler.getNodeAttributesWithValues(contollerNode);
//                for (Map.Entry<String, String> attr : attrs.entrySet()) {
//                    String name = attr.getKey();
//                    if (CO_BIND_ATTR.equalsIgnoreCase(name)) {
//                        controller.setBind(name);
//                    } else if (CO_HOST_ATTR.equalsIgnoreCase(name)) {
//
//                    }
//                }
//
//            }
//
//        } catch (Exception e) {
//
//        }
//        return controller;
//    }

    public static ControllerBean readController(String controllerXml) {
        try {
            JAXBContext jc = JAXBContext.newInstance(ControllerBean.class);
            Unmarshaller u = jc.createUnmarshaller();
            ControllerBean c = (ControllerBean) u.unmarshal(new File(controllerXml));
            return c;
        } catch (JAXBException e) {
            e.printStackTrace(); // TODO handle
        }
        return null;
    }

    public static void saveController(ControllerBean controller, String controllerXml) {
        try {
            JAXBContext jc = JAXBContext.newInstance(ControllerBean.class);
            Marshaller marshaller = jc.createMarshaller();
            JaxbUtils.replaceEmptyStringFieldsToNull(controller);
            marshaller.marshal(controller, new File(controllerXml));
        } catch (Exception e) {
            e.printStackTrace(); // TODO handle
        }
    }

    public static List<EnvironmentBean> readEnvironments(String xmlFile) {
        List<EnvironmentBean> result = new ArrayList<EnvironmentBean>();
        if (!StringUtils.isEmpty(xmlFile) && Files.exists(Paths.get(xmlFile))) {
            try {
                JAXBContext jc = JAXBContext.newInstance(ListWrapper.class, EnvironmentBean.class, AgentInEnvironmentBean.class, AgentsInEnvironmentBean.class);
                Unmarshaller unmarshaller = jc.createUnmarshaller();
                result = JaxbUtils.unmarshal(unmarshaller, EnvironmentBean.class, xmlFile);
            } catch (JAXBException e) {
                e.printStackTrace(); // TODO handle
            }
        }
        return result;
    }

    public static void saveEnvironments(List<EnvironmentBean> envs, String xmlFile) {
        try {
            JAXBContext jc = JAXBContext.newInstance(ListWrapper.class, EnvironmentBean.class, AgentInEnvironmentBean.class, AgentsInEnvironmentBean.class);
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            JaxbUtils.marshal(marshaller, envs, "environments", xmlFile);
        } catch (JAXBException e) {
            e.printStackTrace(); // TODO handle
        }
    }

    // read <LEROY)_HOME>/agents.xml
    public static List<AgentBean> readAgents(String xmlFile) {
        List<AgentBean> result = new ArrayList<AgentBean>();
        try {
            JAXBContext jc = JAXBContext.newInstance(ListWrapper.class, AgentBean.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            result = JaxbUtils.unmarshal(unmarshaller, AgentBean.class, xmlFile);
        } catch (JAXBException e) {
            e.printStackTrace(); // TODO handle
        }
        return result;
    }

    // write <LEROY_HOME>/agents.xml
    public static void saveAgents(List<AgentBean> agentBeans, String xmlFile) {
        try {
            JAXBContext jc = JAXBContext.newInstance(ListWrapper.class, AgentBean.class);
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            for (AgentBean agentBean : agentBeans) {
                JaxbUtils.replaceEmptyStringFieldsToNull(agentBean);
            }
            JaxbUtils.marshal(marshaller, agentBeans, "agents", xmlFile);
        } catch (Exception e) {
            e.printStackTrace(); // TODO handle
        }
    }


}