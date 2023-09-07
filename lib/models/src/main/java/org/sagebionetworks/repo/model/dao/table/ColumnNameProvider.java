package org.sagebionetworks.repo.model.dao.table;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ColumnNameProvider {

	/**
	 * Get names of the columns identified by the provided ColumnIds.
	 * 
	 * @param columnIds
	 * @return
	 */
	Map<Long, String> getColumnNames(Set<Long> columnIds);
	

	/**
	 * Get the constraint clause for the given constraint name.
	 * @param constraintName
	 * @return {@link Optional#empty()} if the constraint does not exist.
	 */
	Optional<String> getConstraintClause(String constraintName);
}
