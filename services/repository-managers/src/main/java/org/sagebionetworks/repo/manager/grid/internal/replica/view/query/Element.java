package org.sagebionetworks.repo.manager.grid.internal.replica.view.query;

import java.util.Map;

public interface Element {

	/**
	 * Each element will be called append its data to the SQL statement.
	 * 
	 * @param sqlBuilder SQL to append to.
	 * @param params     All parameters should be added with a bind variable.
	 * @param context    The context of the query.
	 */
	void toSql(StringBuilder sqlBuilder, Map<String, Object> params, Context context);

}
