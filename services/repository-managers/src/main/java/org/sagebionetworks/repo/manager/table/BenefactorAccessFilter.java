package org.sagebionetworks.repo.manager.table;

import java.util.Set;

/**
 * The set of benefactor IDs a user can access for a single benefactor column of a source
 * table/view, used to build a row-level access filter. The {@code -1} sentinel (the default
 * value for a row with no benefactor) is always included in {@code accessibleIds}, so the set
 * is never empty. Shared by the table-query SQL filter and the search-query OpenSearch filter
 * so both gates compute accessibility identically.
 */
public record BenefactorAccessFilter(String benefactorColumnName, Set<Long> accessibleIds) {
}
