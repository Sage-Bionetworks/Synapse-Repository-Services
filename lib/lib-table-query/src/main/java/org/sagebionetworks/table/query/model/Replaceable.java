package org.sagebionetworks.table.query.model;

/**
 * 
 * A replaceable element can be replaced with another instance of the same type.
 *
 * @param <T>
 */
public interface Replaceable<T extends Element> extends Element {

	/**
	 * Replace this element with the provided replacement. This is done by visiting
	 * the element's parent and replacing its child with the provided element. Note:
	 * This will effectively disconnect this element and all of its children from
	 * its original tree.
	 * 
	 * @param replacement
	 */
	default void replaceElement(T replacement) {
		if (replacement == null) {
			throw new IllegalArgumentException("Replacement cannot be null");
		}
		Element old = this;
		Element parent = this.getParent();
		if (parent == null) {
			throw new IllegalStateException("Cannot replace Element since its parent is null");
		}
		HasReplaceableChildren<T> parentReplaceable = (HasReplaceableChildren<T>) parent;
		parentReplaceable.replaceChildren(replacement);
		parent.recursiveSetParent();
		old.recursiveClearParent();
	};
}
