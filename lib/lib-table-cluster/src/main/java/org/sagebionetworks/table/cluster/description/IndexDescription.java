package org.sagebionetworks.table.cluster.description;

import java.util.List;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.query.model.SqlContext;

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
	 * Provide the column names that should be added to the select statement for the given context.
	 * @param context
	 * @param includeEtag
	 * @return Return an empty list if nothing should be added.
	 */
	List<String> getColumnNamesToAddToSelect(SqlContext context, boolean includeEtag);
	

}
