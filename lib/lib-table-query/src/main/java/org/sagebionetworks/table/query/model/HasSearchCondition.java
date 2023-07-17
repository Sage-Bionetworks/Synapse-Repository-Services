package org.sagebionetworks.table.query.model;

/**
 * Abstraction for any Element that has a search condition.
 *
 */
public interface HasSearchCondition extends Element {

	SearchCondition getSearchCondition();
}
