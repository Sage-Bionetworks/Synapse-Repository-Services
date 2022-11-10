package org.sagebionetworks.table.query.model;

import org.sagebionetworks.util.ValidateArgument;

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

	/**
	 * Helper method to prepare a new element to replace an old element. The parent
	 * from the old element will be set on the new
	 * {@link Element#recursiveSetParent()} and the parent from the old will be
	 * cleared {@link Element#recursiveClearParent()}. This method should be used
	 * when it is not possible to implement {@link Replaceable}, such as when more
	 * than one child element is replaceable.
	 * 
	 * @param <T>
	 * @param oldElement The old element to be replaced.
	 * @param newElement The new replacement element.
	 * @param parent     The parent element that will contain the new element.
	 * @return
	 */
	public static <T extends Element> T prepareToReplace(T oldElement, T newElement, Element parent) {
		ValidateArgument.required(parent, "parent");
		if(oldElement == newElement) {
			// nothing needs to change.
			return newElement;
		}
		if (newElement != null) {
			newElement.setParent(parent);
			newElement.recursiveSetParent();
		}
		if (oldElement != null) {
			oldElement.recursiveClearParent();
		}
		return newElement;
	}
}
