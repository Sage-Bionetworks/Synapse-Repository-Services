package org.sagebionetworks.table.cluster.description;

import java.util.List;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.query.model.SqlContext;

/**
 * Provides information about the index of a table/view.
 *
 */
public interface IndexDescription extends Comparable<IndexDescription> {
	
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
	 * @param isAggregate true if this query includes a group by clause with aggregate functions..
	 * @return Return an empty list if nothing should be added.
	 */
	List<String> getColumnNamesToAddToSelect(SqlContext context, boolean includeEtag, boolean isAggregate);
	
	/**
	 * Get the dependencies of this Index.
	 * @return
	 */
	List<IndexDescription> getDependencies();

	/**
	 * Default @Comparable based on IdAndVersion.
	 */
	@Override
	public default int compareTo(IndexDescription o) {
		return this.getIdAndVersion().compareTo(o.getIdAndVersion());
	}

}
