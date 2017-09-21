package org.sagebionetworks.table.query.model;

/**
 * Abstraction for any predicate.
 *
 */
public interface HasPredicate extends Element {

	/**
	 * Get the left-hand-side of the predicate.
	 * @return
	 */
	public ColumnReference getLeftHandSide();
	
	/**
	 * Get right-hand-side values.
	 * @return
	 */
	public Iterable<UnsignedLiteral> getRightHandSideValues();
	
	/**
	 * Get right-hand-side column references.
	 * @return
	 */
	public Iterable<ColumnName> getRightHandSideColumnReferences();
}
