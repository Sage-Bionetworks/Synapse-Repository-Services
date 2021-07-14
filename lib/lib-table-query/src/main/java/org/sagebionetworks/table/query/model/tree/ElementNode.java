package org.sagebionetworks.table.query.model.tree;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.table.query.model.Element;

/**
 * An immutable doubly linked tree of SQL elements.
 *
 */
public class ElementNode {
	
	private final ElementNode parent;
	private final Element element;
	private final Iterable<ElementNode> children;
	
	ElementNode(ElementNode parentElement, Element thisElement, Iterable<ElementNode> children) {
		super();
		this.parent = parentElement;
		this.element = thisElement;
		this.children = children;
	}
	
	/**
	 * @return the parentElement
	 */
	public ElementNode getParentElement() {
		return parent;
	}

	/**
	 * @return the thisElement
	 */
	public Element getThisElement() {
		return element;
	}


	/**
	 * @return the children
	 */
	public Iterable<ElementNode> getChildren() {
		return children;
	}
	
	/**
	 * Build an immutable tree from the given root.
	 * @param root
	 * @return
	 */
	public static ElementNode build(Element root) {
		//root has no parent.
		ElementNode parent = null;
		return build(parent, root);
	}
	
	static ElementNode build(ElementNode parent, Element child) {
		List<ElementNode> children = new LinkedList<>();
		ElementNode childNode = new ElementNode(parent, child, children);
		for(Element grandChild: child.children()) {
			children.add(build(childNode, grandChild));
		}
		return childNode;
	}

	@Override
	public int hashCode() {
		return Objects.hash(children, parent, element);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ElementNode)) {
			return false;
		}
		ElementNode other = (ElementNode) obj;
		return Objects.equals(children, other.children) && Objects.equals(parent, other.parent)
				&& Objects.equals(element, other.element);
	}


	@Override
	public String toString() {
		return "ElementNode [parentElement=" + parent + ", thisElement=" + element + ", children=" + children
				+ "]";
	}
}
