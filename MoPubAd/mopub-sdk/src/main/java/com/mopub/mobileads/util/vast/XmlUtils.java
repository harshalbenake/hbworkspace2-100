package com.mopub.mobileads.util.vast;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class XmlUtils {
    private XmlUtils() {}

    static Node getFirstMatchingChildNode(final Node node, final String nodeName) {
        return getFirstMatchingChildNode(node, nodeName, null, null);
    }

    static Node getFirstMatchingChildNode(final Node node, final String nodeName, final String attributeName, final List<String> attributeValues) {
        if (node == null || nodeName == null) {
            return null;
        }

        final List<Node> nodes = getMatchingChildNodes(node, nodeName, attributeName, attributeValues);
        if (nodes != null && !nodes.isEmpty()) {
            return nodes.get(0);
        }
        return null;
    }

    static List<Node> getMatchingChildNodes(final Node node, final String nodeName, final String attributeName, final List<String> attributeValues) {
        if (node == null || nodeName == null) {
            return null;
        }

        final List<Node> nodes = new ArrayList<Node>();
        final NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node childNode = nodeList.item(i);
            if (childNode.getNodeName().equals(nodeName)
                    && nodeMatchesAttributeFilter(childNode, attributeName, attributeValues)) {
                nodes.add(childNode);
            }
        }
        return nodes;
    }

    static boolean nodeMatchesAttributeFilter(final Node node, final String attributeName, final List<String> attributeValues) {
        if (attributeName == null || attributeValues == null) {
            return true;
        }

        final NamedNodeMap attrMap = node.getAttributes();
        if (attrMap != null) {
            Node attrNode = attrMap.getNamedItem(attributeName);
            if (attrNode != null && attributeValues.contains(attrNode.getNodeValue())) {
                return true;
            }
        }

        return false;
    }

    static String getNodeValue(final Node node) {
        if (node != null
                && node.getFirstChild() != null
                && node.getFirstChild().getNodeValue() != null) {
            return node.getFirstChild().getNodeValue().trim();
        }
        return null;
    }

    static Integer getAttributeValueAsInt(final Node node, final String attributeName) {
        if (node == null || attributeName == null) {
            return null;
        }

        try {
            return Integer.parseInt(getAttributeValue(node, attributeName));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String getAttributeValue(final Node node, final String attributeName) {
        if (node == null || attributeName == null) {
            return null;
        }

        final NamedNodeMap attrMap = node.getAttributes();
        final Node attrNode = attrMap.getNamedItem(attributeName);
        if (attrNode != null) {
            return attrNode.getNodeValue();
        }
        return null;
    }

    static List<String> getStringDataAsList(final Document vastDoc, final String elementName) {
        return getStringDataAsList(vastDoc, elementName, null, null);
    }

    static List<String> getStringDataAsList(final Document vastDoc, final String elementName, final String attributeName, final String attributeValue) {
        final ArrayList<String> results = new ArrayList<String>();

        if (vastDoc == null) {
            return results;
        }

        final NodeList nodes = vastDoc.getElementsByTagName(elementName);

        if (nodes == null) {
            return results;
        }

        for (int i = 0; i < nodes.getLength(); i++) {
            final Node node = nodes.item(i);

            if (node != null && nodeMatchesAttributeFilter(node, attributeName, Arrays.asList(attributeValue))) {
                // since we parsed with coalescing set to true, CDATA is added as the child of the element
                final String nodeValue = getNodeValue(node);
                if (nodeValue != null) {
                    results.add(nodeValue);
                }
            }
        }

        return results;
    }
}
