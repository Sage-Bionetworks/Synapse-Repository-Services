package org.sagebionetworks.table.query.model;

/**
 * Marks elements that can represent aggregate functions.
 *
 */
public interface HasAggregate extends Element {

	/**
	 * Does this element represent an aggregate function?
	 * @return
	 */
	public boolean isAggregate();
}
