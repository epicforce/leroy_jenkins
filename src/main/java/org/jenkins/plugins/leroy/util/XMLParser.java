/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
 
	//optional, but recommended
	//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
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
  
   public static List<String> getEnvironment(File fXmlFile) {
 
    try {
        List<String> agents = new ArrayList<String>();
	//File fXmlFile = new File("/Users/mkyong/staff.xml");
	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	Document doc = dBuilder.parse(fXmlFile);
 
	//optional, but recommended
	//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
	doc.getDocumentElement().normalize();
 
//	System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
 
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
 
 
}
