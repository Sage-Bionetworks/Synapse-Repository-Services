package org.sagebionetworks.table.query.model;

/**
 * Abstraction for any predicate.
 *
 */
public interface HasPredicate extends Element {

	/**
	 * Get the left-hand-side of the predicate
	 * @return
	 */
	ColumnReference getLeftHandSide();
	
	/**
	 * Get right-hand-side values.
	 * @return
	 */
	Iterable<UnsignedLiteral> getRightHandSideValues();
	
}
