package org.sagebionetworks.table.cluster.description;

import java.util.List;

/**
 * Provides information about the index of a table/view. Extends the query-time
 * contract {@link QueryIndexDescription} with the build-only ability to generate
 * the index's create/update SQL.
 *
 */
public interface IndexDescription extends QueryIndexDescription {

	/**
	 * The SQL statement to create or update the index for this table/view
	 *
	 * @return
	 */
	String getCreateOrUpdateIndexSql();

	/**
	 * Get the dependencies of this Index. Re-declared with a covariant
	 * {@link IndexDescription} element type so build-side callers keep the full
	 * contract on each dependency.
	 *
	 * @return
	 */
	@Override
	List<IndexDescription> getDependencies();

}
