package org.sagebionetworks.table.query.model;

/**
 * Marks elements that can represent aggregate functions.
 *
 */
public interface HasAggregate extends Element {

	/**
	 * Does this element represent an aggregate function?
	 * Note: This method is not recursive an will only answer the question for this element.
	 * @return
	 */
	boolean isElementAggregate();
}
