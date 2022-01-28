package org.sagebionetworks.table.query.model;

/**
 * Marker for a provider of a SqlContext.
 *
 */
public interface HasSqlContext extends Element{

	/**
	 * Get the SqlContext
	 * @return
	 */
	SqlContext getSqlContext();
}
