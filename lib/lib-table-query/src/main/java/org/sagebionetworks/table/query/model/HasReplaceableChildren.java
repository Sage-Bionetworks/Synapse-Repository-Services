package org.sagebionetworks.table.query.model;

/**
 * A parent element that allows for the children to be replaced with a given element
 *
 */
public interface HasReplaceableChildren extends Element {

	/**
	 * Replace the children of this element with the given replacement. 
	 * 
	 * @param replacement
	 */
	public void replaceChildren(Element replacement);
}
