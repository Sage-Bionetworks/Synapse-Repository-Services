package org.sagebionetworks.table.cluster.description;

import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnLineageEntry;
import org.sagebionetworks.table.query.model.SqlContext;

/**
 * The query-time contract of a table/view index: everything the preflight
 * authorization, benefactor row-level filter, and query translation consume.
 * This is the narrow projection satisfied both by the live {@link IndexDescription}
 * subtypes and by a snapshot-backed reconstitution of an as-built index, so the
 * same authorization/translation code path can run against either current-truth
 * state or the as-built state captured with the served index.
 * <p>
 * It intentionally omits everything needed only to build an index (create/update
 * SQL); that lives on {@link IndexDescription}.
 */
public interface QueryIndexDescription extends Comparable<QueryIndexDescription> {

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
	List<? extends QueryIndexDescription> getDependencies();

	/**
	 * The flattened lineage of this object's output columns, in select-list order:
	 * one entry per output column, naming the leaf source columns it is derived from.
	 * Used to decide whether an output column carries a quasi-identifier of any
	 * source, independent of any renaming or transformation applied along the way.
	 *
	 * @return one entry per output column, in select-list order
	 * @throws IllegalStateException if this description cannot supply a lineage
	 */
	default List<ColumnLineageEntry> getColumnLineage() {
		// Failing loudly is required: an empty lineage would resolve zero quasi-identifier-derived
		// columns and silently release protected rows, so a description that cannot answer must
		// never be treated as answering "none".
		throw new IllegalStateException(
				"No column lineage is available for " + getIdAndVersion() + " of type " + getTableType());
	}

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
		for (QueryIndexDescription dependency : getDependencies()) {
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
	 * @return True if a query against this index supports caching
	 */
	default boolean supportQueryCache() {
		return false;
	}

	/**
	 * Default @Comparable based on IdAndVersion.
	 */
	@Override
	public default int compareTo(QueryIndexDescription o) {
		return this.getIdAndVersion().compareTo(o.getIdAndVersion());
	}

}
