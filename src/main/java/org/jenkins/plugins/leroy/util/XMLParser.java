package org.jenkins.plugins.leroy.util;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
 
public class XMLParser {
 
    public static void main(String argv[]){
        getAgents(new File(argv[0]));
    }
    
  public static List<String> getAgents(File fXmlFile) {
 
    try {
        List<String> agents = new ArrayList<String>();
	//File fXmlFile = new File("/Users/mkyong/staff.xml");
	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	Document doc = dBuilder.parse(fXmlFile);
 
	doc.getDocumentElement().normalize();
 
	NodeList nList = doc.getElementsByTagName("agent");
  
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
   public static List<String> getRoles(File fXmlFile) {
 
    try {
        List<String> agents = new ArrayList<String>();
	//File fXmlFile = new File("/Users/mkyong/staff.xml");
	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	Document doc = dBuilder.parse(fXmlFile);
 
	doc.getDocumentElement().normalize();
 

	NodeList nList = doc.getElementsByTagName("agent");
 
	for (int temp = 0; temp < nList.getLength(); temp++) {
 
		Node nNode = nList.item(temp);
                Element eElement = (Element) nNode;
                //NodeList nList = eElement.getElementsByTagName("environments");
                agents.add(eElement.getAttribute("roles"));
	}
        return agents;
    } catch (Exception e) {
	e.printStackTrace();
    }
    return null;
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
                if(eElement.getAttribute("name").equalsIgnoreCase(enviromentname)){
                NodeList nList1 = eElement.getElementsByTagName("agent");
                for(int i = 0; i < nList1.getLength(); i++)
                {
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
 
}
