package org.sagebionetworks.table.query.model;

import java.util.Optional;

/**
 * Abstraction for any predicate.
 *
 */
public interface HasPredicate extends Element {

	/**
	 * Get the left-hand-side of the predicate
	 * 
	 * @return
	 */
	ColumnReference getLeftHandSide();

	/**
	 * Get right-hand-side values.
	 * 
	 * @return
	 */
	Iterable<UnsignedLiteral> getRightHandSideValues();

	/**
	 * Get the right-hand-side column if it exists.
	 * 
	 * @return {@link Optional#empty()} if the right-hand-side is not a column
	 *         reference.
	 */
	default Optional<ColumnReference> getRightHandSideColumn() {
		return Optional.empty();
	}

}
