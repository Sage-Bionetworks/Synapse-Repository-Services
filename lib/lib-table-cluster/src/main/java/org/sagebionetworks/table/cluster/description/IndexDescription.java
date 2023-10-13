package org.sagebionetworks.table.cluster.description;

import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
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
	 * 
	 * @return
	 */
	IdAndVersion getIdAndVersion();

	/**
	 * Get the type of table for this index.
	 * 
	 * @return
	 */
	TableType getTableType();

	/**
	 * The SQL statement to create or update the index for this table/view
	 * 
	 * @return
	 */
	String getCreateOrUpdateIndexSql();

	/**
	 * The description of each benefactor column in this table/view.
	 * 
	 * @return Will return an empty if there are no benefactors.s
	 */
	List<BenefactorDescription> getBenefactors();

	/**
	 * Provide the column names that should be added to the select statement for the
	 * given context.
	 * 
	 * @param context
	 * @param includeEtag
	 * @param isAggregate true if this query includes a group by clause with
	 *                    aggregate functions..
	 * @return Return an empty list if nothing should be added.
	 */
	List<ColumnToAdd> getColumnNamesToAddToSelect(SqlContext context, boolean includeEtag, boolean isAggregate);

	/**
	 * Get the dependencies of this Index.
	 * 
	 * @return
	 */
	List<IndexDescription> getDependencies();

	/**
	 * This number is used to generate a table's hash. Therefore, an implementor
	 * should override this method and return a new value whenever a table has
	 * changed. The hash is used to prevent stale query cache hits. The default
	 * implementation returns {@link Optional#empty()}.
	 * 
	 * @return
	 */
	default Optional<Long> getLastTableChangeNumber() {
		return Optional.empty();
	}

	/**
	 * Used to build recursively build the table's hash. Implementors should not
	 * need to override this method.
	 */
	default void recursiveAppendIdAndChangeNumber(StringBuilder builder) {
		getLastTableChangeNumber().ifPresent(n -> {
			builder.append("+");
			builder.append(getIdAndVersion().toString());
			builder.append("-");
			builder.append(n);
		});
		for (IndexDescription dependency : getDependencies()) {
			dependency.recursiveAppendIdAndChangeNumber(builder);
		}
	}

	/**
	 * The table's hash represents a unique hash that recursively includes all
	 * dependencies. The hash is a MD5 hex string of the combination of each table's
	 * IdAndVersion plus the value from {@link #getLastTableChangeNumber()}. Any
	 * change to a dependency's hash will result in a change to root's hash.
	 * Implementors should not need to override this method.
	 * 
	 * @return
	 */
	default String getTableHash() {
		StringBuilder builder = new StringBuilder();
		recursiveAppendIdAndChangeNumber(builder);
		return DigestUtils.md5Hex(builder.toString());
	}

	/**
	 * @return True if the row id should be included in the search index when search
	 *         is enabled
	 */
	default boolean addRowIdToSearchIndex() {
		return false;
	}

	/**
	 * Pre-process the given runtime-query and return a new query to be run in its
	 * place.
	 * 
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
