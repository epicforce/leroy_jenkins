package org.jenkins.plugins.leroy.util;

/**
 * Created by Dzmitry Bahdanovich on 27.06.14.
 */

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XMLHandler {

    /**
     * Get the value of a tag in a node
     *
     * @param n   The node to look in
     * @param tag The tag to look for
     * @return The value of the tag or null if nothing was found.
     */
    public static final String getTagValue(Node n, String tag) {
        NodeList children;
        Node childnode;

        if (n == null) {
            return null;
        }

        children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            childnode = children.item(i);
            if (childnode.getNodeName().equalsIgnoreCase(tag)) {
                if (childnode.getFirstChild() != null) {
                    return childnode.getFirstChild().getNodeValue();
                }
            }
        }
        return null;
    }

    /**
     * Get the value of a tag in a node
     *
     * @param n   The node to look in
     * @param tag The tag to look for
     * @return The value of the tag or null if nothing was found.
     */
    public static final String getTagValueWithAttribute(Node n, String tag, String attribute) {
        NodeList children;
        Node childnode;

        if (n == null) {
            return null;
        }

        children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            childnode = children.item(i);
            if (childnode.getNodeName().equalsIgnoreCase(tag)
                    && childnode.getAttributes().getNamedItem(attribute) != null) {
                if (childnode.getFirstChild() != null) {
                    return childnode.getFirstChild().getNodeValue();
                }
            }
        }
        return null;
    }

    /**
     * Search a node for a certain tag, in that subnode search for a certain subtag. Return the value of that subtag.
     *
     * @param n      The node to look in
     * @param tag    The tag to look for
     * @param subtag The subtag to look for
     * @return The string of the subtag or null if nothing was found.
     */
    public static final String getTagValue(Node n, String tag, String subtag) {
        NodeList children, tags;
        Node childnode, tagnode;

        if (n == null) {
            return null;
        }

        children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            childnode = children.item(i);
            if (childnode.getNodeName().equalsIgnoreCase(tag)) {
                // <file>
                tags = childnode.getChildNodes();
                for (int j = 0; j < tags.getLength(); j++) {
                    tagnode = tags.item(j);
                    if (tagnode.getNodeName().equalsIgnoreCase(subtag)) {
                        if (tagnode.getFirstChild() != null) {
                            return tagnode.getFirstChild().getNodeValue();
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Count nodes with a certain tag
     *
     * @param n   The node to look in
     * @param tag The tags to count
     * @return The number of nodes found with a certain tag
     */
    public static final int countNodes(Node n, String tag) {
        NodeList children;
        Node childnode;

        int count = 0;

        if (n == null) {
            return 0;
        }

        children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            childnode = children.item(i);
            if (childnode.getNodeName().equalsIgnoreCase(tag)) {
                // <file>
                count++;
            }
        }
        return count;
    }

    /**
     * Get nodes with a certain tag one level down
     *
     * @param n   The node to look in
     * @param tag The tags to count
     * @return The list of nodes found with the specified tag
     */
    public static final List<Node> getNodes(Node n, String tag) {
        NodeList children;
        Node childnode;

        List<Node> nodes = new ArrayList<Node>();

        if (n == null) {
            return nodes;
        }

        children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            childnode = children.item(i);
            if (childnode.getNodeName().equalsIgnoreCase(tag)) {
                // <file>
                nodes.add(childnode);
            }
        }
        return nodes;
    }

    /**
     * Get nodes with a certain tag one level down
     *
     * @param n   The node to look in
     * @param tagRegex The tags regex to count
     * @return The list of nodes found which names match a regex
     */
    public static final List<Node> getNodesByRegex(Node n, String tagRegex) {
        NodeList children;
        Node childnode;

        List<Node> nodes = new ArrayList<Node>();

        if (n == null) {
            return nodes;
        }

        children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            childnode = children.item(i);
            if (childnode.getNodeName().matches(tagRegex)) {
                nodes.add(childnode);
            }
        }
        return nodes;
    }

    /**
     * Get node child with a certain subtag set to a certain value
     *
     * @param n           The node to search in
     * @param tag         The tag to look for
     * @param subtag      The subtag to look for
     * @param subtagvalue The value the subtag should have
     * @param nr          The nr of occurance of the value
     * @return The node found or null if we couldn't find anything.
     */
    public static final Node getNodeWithTagValue(Node n, String tag, String subtag, String subtagvalue, int nr) {
        NodeList children;
        Node childnode, tagnode;
        String value;

        int count = 0;

        children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            childnode = children.item(i);
            if (childnode.getNodeName().equalsIgnoreCase(tag)) {
                // <hop>
                tagnode = getSubNode(childnode, subtag);
                value = getNodeValue(tagnode);
                if (value.equalsIgnoreCase(subtagvalue)) {
                    if (count == nr) {
                        return childnode;
                    }
                    count++;
                }
            }
        }
        return null;
    }

    /**
     * Get node child with a certain subtag set to a certain value
     *
     * @return The node found or null if we couldn't find anything.
     */
    public static final Node getNodeWithAttributeValue(Node n, String tag, String attributeName,
                                                       String attributeValue) {
        NodeList children;
        Node childnode;

        children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            childnode = children.item(i);
            if (childnode.getNodeName().equalsIgnoreCase(tag)) {
                // <hop>
                Node attribute = childnode.getAttributes().getNamedItem(attributeName);

                if (attribute != null && attributeValue.equals(attribute.getTextContent())) {
                    return childnode;
                }
            }
        }
        return null;
    }

    /**
     * Search for a subnode in the node with a certain tag.
     *
     * @param n   The node to look in
     * @param tag The tag to look for
     * @return The subnode if the tag was found, or null if nothing was found.
     */
    public static final Node getSubNode(Node n, String tag) {
        int i;
        NodeList children;
        Node childnode;

        if (n == null) {
            return null;
        }

        // Get the childres one by one out of the node,
        // compare the tags and return the first found.
        //
        children = n.getChildNodes();
        for (i = 0; i < children.getLength(); i++) {
            childnode = children.item(i);
            if (childnode.getNodeName().equalsIgnoreCase(tag)) {
                return childnode;
            }
        }
        return null;
    }

    /**
     * Search a node for a child of child
     *
     * @param n      The node to look in
     * @param tag    The tag to look for in the node
     * @param subtag The tag to look for in the children of the node
     * @return The sub-node found or null if nothing was found.
     */
    public static final Node getSubNode(Node n, String tag, String subtag) {
        Node t = getSubNode(n, tag);
        if (t != null) {
            return getSubNode(t, subtag);
        }
        return null;
    }


    /**
     * Find the value entry in a node
     *
     * @param n The node
     * @return The value entry as a string
     */
    public static final String getNodeValue(Node n) {
        if (n == null) {
            return null;
        }

        // Find the child-nodes of this Node n:
        NodeList children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            // Try all children
            Node childnode = children.item(i);
            String retval = childnode.getNodeValue();
            if (retval != null) { // We found the right value
                return retval;
            }
        }
        return null;
    }

    public static final String getTagAttribute(Node node, String attribute) {
        if (node == null) {
            return null;
        }

        String retval = null;

        NamedNodeMap nnm = node.getAttributes();
        if (nnm != null) {
            Node attr = nnm.getNamedItem(attribute);
            if (attr != null) {
                retval = attr.getNodeValue();
            }
        }
        return retval;
    }

    /**
     * Load a String into an XML document
     *
     * @param string The XML text to load into a document
     */
    public static final Document loadXMLString(String string) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf;
        DocumentBuilder db;
        Document doc;

        // Check and open XML document
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", true);
        dbf.setNamespaceAware(false); // parameterize this as well
        db = dbf.newDocumentBuilder();
        StringReader stringReader = new java.io.StringReader(string);
        InputSource inputSource = new InputSource(stringReader);
        try {
            doc = db.parse(inputSource);
        } finally {
            stringReader.close();
        }

        return doc;
    }

    public static final Document loadXMLFile(String filename) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf;
        DocumentBuilder db;
        Document doc;

        // Check and open XML document
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", true);
        dbf.setNamespaceAware(false); // parameterize this as well
        db = dbf.newDocumentBuilder();
        try {
            doc = db.parse(new File(filename));
        } finally {
        }

        return doc;
    }


    public static final String getString() {
        return XMLHandler.class.getName();
    }


    /**
     * Get all the attributes in a certain node (on the root level)
     *
     * @param node The node to examine
     * @return an array of strings containing the names of the attributes.
     */
    public static String[] getNodeAttributes(Node node) {
        NamedNodeMap nnm = node.getAttributes();
        if (nnm != null) {
            String[] attributes = new String[nnm.getLength()];
            for (int i = 0; i < nnm.getLength(); i++) {
                Node attr = nnm.item(i);
                attributes[i] = attr.getNodeName();
            }
            return attributes;
        }
        return null;

    }

    /**
     * Get all the attributes and their values in a certain node (on the root level)
     *
     * @param node The node to examine
     * @return an array of strings containing the names of the attributes.
     */
    public static Map<String, String> getNodeAttributesWithValues(Node node) {
        Map<String, String> attrMap = new LinkedHashMap<String, String>();
        NamedNodeMap nnm = node.getAttributes();
        if (nnm != null) {
            for (int i = 0; i < nnm.getLength(); i++) {
                Node attr = nnm.item(i);
                String name = attr.getNodeName();
                String value = getNodeValue(attr);
                attrMap.put(name, value);
            }
        }
        return attrMap;
    }


    public static String[] getNodeElements(Node node) {
        ArrayList<String> elements = new ArrayList<String>(); // List of String

        NodeList nodeList = node.getChildNodes();
        if (nodeList == null) {
            return null;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            String nodeName = nodeList.item(i).getNodeName();
            if (elements.indexOf(nodeName) < 0) {
                elements.add(nodeName);
            }
        }

        if (elements.isEmpty()) {
            return null;
        }

        return elements.toArray(new String[elements.size()]);
    }


    public static final String openTag(String tag) {
        return "<" + tag + ">";
    }

    public static final String closeTag(String tag) {
        return "</" + tag + ">";
    }

}

