package org.sagebionetworks.web.client.widget.entity;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/**
 * Some common DOM utility methods.
 * 
 * @author <a href="mailto:jasone@greenrivercomputing.com">Jason Essington</a>
 * @version $Revision: 0.0 $
 */
public class DOMUtil {

    /**
     * Support class used to represent a NodeList in Java.
     * 
     * @author <a href="mailto:jasone@greenrivercomputing.com">Jason Essington</a>
     * @version $Revision: 0.0 $
     */
    private static class NodeList {

        // holds the nodelist for access via JSNI
        private JavaScriptObject nodeList;

        /**
         * Creates a new NodeList.
         * 
         * @param nodeListObject
         *            NodeList returned from a native method
         */
        public NodeList(JavaScriptObject nodeListObject) {
            this.nodeList = nodeListObject;
        }

        /**
         * Returns the javascript NodeList as List&lt;Element&gt;.
         * 
         * @return List of Elements
         */
        public List asList() {
            List nodes = new ArrayList();
            for (int i = 0; i < size(); i++) {
                nodes.add(item(i));
            }
            return nodes;
        }

        /**
         * Returns a particular element from the node list.
         * 
         * @param index
         *            index of the element to return
         * @return Element found at the supplied index
         */
        public native Element item(int index)/*-{
//         var nl = this.@com.leapfrog.gwt.common.client.util.DOMUtil.NodeList::nodeList;
//         return nl.item(index);
         }-*/;

        /**
         * Returns the "length" of this node list.
         * 
         * @return length of the node list
         */
        public native int size()/*-{
//         var nl = this.@com.leapfrog.gwt.common.client.util.DOMUtil.NodeList::nodeList;
//         return nl.length;
         }-*/;
    }

    public static final String HTML_ANCHOR = "A";

    public static final String HTML_DIV = "DIV";

    public static final String HTML_LIST_ITEM = "LI";

    public static final String HTML_FORM = "FORM";

    public static final String HTML_SELECT = "SELECT";

    public static final String HTML_UNORDERED_LIST = "UL";

    public static final int NODE_TYPE_ELEMENT = 1;

    public static final int NODE_TYPE_TEXT = 3;

    private static final String PROPERTY_NODE_NAME = "nodeName";

    /**
     * GWT doesn't include any way to create a text node, so here's how it is
     * done.
     * 
     * @param text
     *            Raw text to make into an element
     * @return Element object that may be inserted into DOM
     */
    public static native Element createTextNode(String text) /*-{
     return $doc.createTextNode(text);
     }-*/;

    /**
     * Utility method to determine if a given element contains the supplied CSS
     * class name.
     * 
     * @param e
     *            Element to check
     * @param name
     *            CSS class name
     * @return true it the element contains the name
     */
    public static boolean elementContainsClassName(Element e, String name) {
        boolean containsName = false;
        String classes = DOM.getElementProperty(e, "className");
        if (classes != null && !"".equals(classes)) {
            String[] names = classes.split(" ");
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(name)) {
                    containsName = true;
                    break;
                }
            }
        }
        return containsName;
    }

    /**
     * Checks the element to see if it is of the supplied type. For instance to
     * see if an element is a select list call, elementIS(e, "SELECT"). The
     * actual comparison is case insensitive, but the nodeName property returns
     * uppercase.
     * 
     * @param e
     *            element to check
     * @param type
     *            node name to check against
     * @return true if the element is a of the particular type.
     */
    public static boolean elementIs(Element e, String type) {
        boolean match = false;
        if (type != null && e != null) {
            String nodeName = DOM.getElementProperty(e, PROPERTY_NODE_NAME);
            match = type.equalsIgnoreCase(nodeName);
        }
        return match;
    }

    /**
     * Returns all elements from the document that contain the supplied css
     * class name.
     * 
     * @param name
     *            CSS class name
     * @return list of Elements
     */
    public static List getElementsByClassName(String name) {
        return getElementsByClassNameFrom(RootPanel.getBodyElement(), name);
    }

    /**
     * Returns all child elements of the supplied parent that contain the
     * supplied css class name.
     * 
     * @param parent
     *            Parent element
     * @param name
     *            CSS class name
     * @return list of Elements
     */
    public static List getElementsByClassNameFrom(Element parent, String name) {
        List elements = new ArrayList();
        if (parent != null) {
            int children = DOM.getChildCount(parent);
            for (int i = 0; i < children; i++) {
                Element child = DOM.getChild(parent, i);
                if (elementContainsClassName(child, name)) {
                    elements.add(child);
                }
                elements.addAll(getElementsByClassNameFrom(child, name));
            }
        }
        return elements;
    }
    
    /**
     * Removes the listed style names recursively from the root element
     * @param classNames list of class names to remove
     * @param rootElement start of the element tree
     */
	public static void removeStyles(String[] classNames, Element rootElement) {
		for(String className : classNames) {
		@SuppressWarnings("unchecked")
		List<Element> bwraps = DOMUtil.getElementsByClassNameFrom(rootElement, className);
			for(Element el : bwraps) {
				el.removeClassName(className);
			}
		}
	}


    /**
     * Returns a list of all elements in the document with the supplied tag
     * name.
     * 
     * @param name
     *            Tag name to find
     * @return List of Elements
     */
    public static List getElementsByTagName(String name) {
        return marshalNodeList(nativeGetElementsByTagNameFrom(RootPanel.getBodyElement(), 
name));
    }

    /**
     * Returns a list of all elements which are children of the supplied parent
     * with the supplied tag name.
     * 
     * @param name
     *            Tag name to find
     * @param parent
     *            Element to start from rather than document
     * @return List of Elements
     */
    public static List getElementsByTagNameFrom(Element parent, String name) {
        if (parent != null) {
            return marshalNodeList(nativeGetElementsByTagNameFrom(parent, name));
        } else {
            return new ArrayList();
        }
    }

    /**
     * Fetches the first child of the supplied parent if it is of the same node
     * type.
     * 
     * @param parent
     *            Element who's first child we want to fetch
     * @param ntype
     *            The node type that we are looking for
     * @return element found, or null if it wasn't the proper type or there were
     *         no children
     */
    public static native Element getFirstChildIfType(Element parent, int ntype)/*-{
     var child = parent.firstChild;
     return (child != null && child.nodeType == ntype) ? child : null;
     }-*/;

    /**
     * Searches the document for elements of a particular type that contain the
     * given CSS class.
     * 
     * @param tagName
     *            Element tag name
     * @param className
     *            CSS class name
     * @return List of matching elements
     */
    public static List getTagsByClassName(String tagName, String className) {
        return getTagsByClassNameFrom(RootPanel.getBodyElement(), tagName, className);
    }

    /**
     * Searches the supplied element for elements of a particular type that
     * contain the given CSS class.
     * 
     * @param parent
     *            Element that is the starting point for the search
     * @param tagName
     *            Element tag name
     * @param className
     *            CSS class name
     * @return List of matching elements
     */
    public static List getTagsByClassNameFrom(Element parent, String tagName, String className) 
{
        List elements = getElementsByTagNameFrom(parent == null ? RootPanel.getBodyElement() : 
parent, tagName);
        for (Iterator i = elements.iterator(); i.hasNext();) {
            Element element = (Element) i.next();
            if (!elementContainsClassName(element, className)) {
                i.remove();
            }
        }
        return elements;
    }

    /**
     * Converts a Javascript NodeList to a java List of Elements.
     * 
     * @param jso
     *            Javascript NodeList
     * @return List of Element objects
     */
    private static List marshalNodeList(JavaScriptObject jso) {
        List nodes = new ArrayList();
        if (jso != null) {
            NodeList nodeList = new NodeList(jso);
            for (int i = 0; i < nodeList.size(); i++) {
                nodes.add(nodeList.item(i));
            }
        }
        return nodes;
    }

    /**
     * Native call to get elements by tag name from the document.
     * 
     * @param parent
     *            Element where the search for nodes should begin
     * @param name
     *            tag name
     * @return NodeList as a JavaScriptObject
     */
    private static native JavaScriptObject nativeGetElementsByTagNameFrom(Element parent, String 
name)/*-{
     var nodeList = parent.getElementsByTagName(name);
     return nodeList == null ? null : nodeList;
     }-*/;

    /**
     * Hide default constructor.
     */
    protected DOMUtil() {
    }
}