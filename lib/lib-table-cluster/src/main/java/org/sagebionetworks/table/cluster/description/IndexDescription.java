package org.sagebionetworks.table.cluster.description;

import java.util.List;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;

/**
 * Provides information about the index of a table/view.
 *
 */
public interface IndexDescription {
	
	/**
	 * The IdAndVersion of this table/view
	 * @return
	 */
	IdAndVersion getIdAndVersion();
	
	/**
	 * Get the type of table for this index.
	 * @return
	 */
	EntityType getTableType();
	
	/**
	 * The SQL statement to create or update the index for this table/view
	 * @return
	 */
	String getCreateOrUpdateIndexSql();
	
	/**
	 * The description of each benefactor column in this table/view.
	 * @return Will return an empty if there are no benefactors.s
	 */
	List<BenefactorDescription> getBenefactors();
	
	
	/**
	 * Does this table/view include an etag column.
	 * @return
	 */
	boolean isEtagColumnIncluded();
	
	/**
	 * The list of column names to add to the select statement.
	 * @return
	 */
	List<String> getColumnNamesToAddToSelect(SqlType type);
	

}
