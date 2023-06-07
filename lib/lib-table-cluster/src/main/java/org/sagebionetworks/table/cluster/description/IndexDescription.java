package org.sagebionetworks.table.cluster.description;

import java.util.List;

import org.sagebionetworks.repo.model.dao.table.TableType;
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
	TableType getTableType();
	
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
	List<ColumnToAdd> getColumnNamesToAddToSelect(SqlContext context, boolean includeEtag, boolean isAggregate);
	
	/**
	 * Get the dependencies of this Index.
	 * @return
	 */
	List<IndexDescription> getDependencies();
	
	/**
	 * @return True if the row id should be included in the search index when search is enabled
	 */
	default boolean addRowIdToSearchIndex() {
		return false;
	}
	
	/**
	 * Pre-process the given runtime-query and return a new query to be run in its place.
	 * @param sql
	 * @return
	 */
	default String preprocessQuery(String sql) {
		return sql;
	}

	/**
	 * Default @Comparable based on IdAndVersion.
	 */
	@Override
	public default int compareTo(IndexDescription o) {
		return this.getIdAndVersion().compareTo(o.getIdAndVersion());
	}

}
