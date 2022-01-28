package org.sagebionetworks.table.query.model;

/**
 * An enumeration to indicates the context of SQL that is being processed.
 */
public enum SqlContext {

	/**
	 * Indicates that the SQL is used to build a table/view. This type is set when
	 * the SQL is used to define a materialized view.
	 */
	build,
	/**
	 * Indicates that the SQL is query against a table/view
	 */
	query;

}
