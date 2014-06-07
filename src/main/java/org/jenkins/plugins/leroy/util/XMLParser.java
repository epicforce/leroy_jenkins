package org.jenkins.plugins.leroy.util;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;
 
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
 
   public static void createConfigurtionXML(String path) throws FileAlreadyExistsException{
       File configfile = new File(path);
       
       if(configfile.exists()){
           throw new FileAlreadyExistsException(path);
       }
       
       try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("Environments");
            doc.appendChild(rootElement);

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(configfile);

            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);

            transformer.transform(source, result);

            System.out.println("File saved!");
 
	  } catch (ParserConfigurationException pce) {
		pce.printStackTrace();
	  } catch (TransformerException tfe) {
		tfe.printStackTrace();
	  }
   }
   
   public static boolean addConfigurationElement(File configxml, String name, String value)
   {
       DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
       DocumentBuilder dBuilder;
       try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configxml);
            doc.getDocumentElement().normalize();
            
            NodeList envnodes = doc.getElementsByTagName("Environment");
            Node env = null;
            //loop for each employee
            for(int i=0; i<envnodes.getLength();i++){
                env = envnodes.item(i);
                NamedNodeMap attr = env.getAttributes();
		Node nodeAttr = attr.getNamedItem("id");
                
//		nodeAttr.setTextContent("2");
                String temp = nodeAttr.getNodeValue();
                if(nodeAttr.getNodeValue().equals(name))
                {
                    env.setTextContent(value);
                    
//                  doc.getElementsByTagName("Environments").item(0).replaceChild(env, envnodes.item(i));
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer();
                    DOMSource source = new DOMSource(doc);
                    StreamResult result = new StreamResult(configxml);
                    transformer.transform(source, result);
                    return true;
                }    
                  
                //Node name = emp.getElementsByTagName("name").item(0).getFirstChild();
                //name.setNodeValue(name.getNodeValue().toUpperCase());
            }
            
            // staff elements
            Element newenv = doc.createElement("Environment");
            
            Attr attr = doc.createAttribute("id");
            attr.setValue(name);
            newenv.setAttributeNode(attr);
            newenv.appendChild(doc.createTextNode(value));
            doc.getElementsByTagName("Environments").item(0).appendChild(newenv);
             
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(configxml);
            transformer.transform(source, result);
           // System.out.println("XML file updated successfully");
            return true;
        } catch (SAXException e) { 
            e.printStackTrace();
        } catch (TransformerException e) { 
            e.printStackTrace();
        } catch (IOException e) { 
            e.printStackTrace();
        } catch (ParserConfigurationException e) { 
            e.printStackTrace();
        }
        return false;
   }
   
   public static boolean hasConfigurationElement(File configxml, String name)
   {
       DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
       DocumentBuilder dBuilder;
       try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configxml);
            doc.getDocumentElement().normalize();
            
            NodeList envnodes = doc.getElementsByTagName("Environment");
            Node env = null;
            //loop for each employee
            for(int i=0; i<envnodes.getLength();i++){
                env = envnodes.item(i);
                NamedNodeMap attr = env.getAttributes();
		Node nodeAttr = attr.getNamedItem("id");
                
                if(nodeAttr.getNodeValue().equals(name))
                {
                      return true;
                }    
            }
            return false;
        } catch (SAXException e) { 
            e.printStackTrace();
        }  catch (IOException e) { 
            e.printStackTrace();
        } catch (ParserConfigurationException e) { 
            e.printStackTrace();
        }
        return false;
   }
   
   public static String getConfigurationElement(File configxml, String name)
   {
       DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
       DocumentBuilder dBuilder;
       try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configxml);
            doc.getDocumentElement().normalize();
            
            NodeList envnodes = doc.getElementsByTagName("Environment");
            Node env = null;
            //loop for each employee
            for(int i=0; i<envnodes.getLength();i++){
                env = envnodes.item(i);
                NamedNodeMap attr = env.getAttributes();
		Node nodeAttr = attr.getNamedItem("id");
                
                if(nodeAttr.getNodeValue().equals(name))
                {
                      return env.getTextContent();
                }    
            }
            return null;
        } catch (SAXException e) { 
            e.printStackTrace();
        }  catch (IOException e) { 
            e.printStackTrace();
        } catch (ParserConfigurationException e) { 
            e.printStackTrace();
        }
        return null;
   }

}