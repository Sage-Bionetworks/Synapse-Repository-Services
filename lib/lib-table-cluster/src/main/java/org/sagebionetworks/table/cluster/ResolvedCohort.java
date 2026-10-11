package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * The server-side result of running a request-scoped cohort query: the distinct values of the cohort's
 * single selected column. The values must never be returned to the caller, so {@link #toString()} omits
 * them.
 *
 * @param name          the cohort's name, as referenced by {@code COHORT(<name>)}.
 * @param columnType    the type of the cohort's selected column.
 * @param values        the distinct values of the selected column.
 * @param aggregateOnly true when the caller has aggregate-only access to the cohort's source.
 */
public record ResolvedCohort(String name, ColumnType columnType, List<String> values, boolean aggregateOnly) {

	public ResolvedCohort {
		values = List.copyOf(values);
	}

	@Override
	public String toString() {
		return "ResolvedCohort[name=" + name + ", columnType=" + columnType + ", aggregateOnly=" + aggregateOnly + "]";
	}

}
