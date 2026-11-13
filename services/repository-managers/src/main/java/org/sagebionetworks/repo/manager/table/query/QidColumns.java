package org.sagebionetworks.repo.manager.table.query;

import java.util.Set;

import org.sagebionetworks.util.ValidateArgument;

/**
 * The columns of a queried object that carry a quasi-identifier (QID) of an aggregate-only source.
 *
 * @param all        the ids of the queried object's own columns whose lineage reaches a QID.
 * @param aggregates the subset of {@code all} whose value is an aggregate of a QID, such as a count computed
 *                   by a MaterializedView or VirtualTable.
 */
public record QidColumns(Set<String> all, Set<String> aggregates) {

	public static final QidColumns NONE = new QidColumns(Set.of(), Set.of());

	public QidColumns {
		ValidateArgument.required(all, "all");
		ValidateArgument.required(aggregates, "aggregates");
		ValidateArgument.requirement(all.containsAll(aggregates), "Every aggregate QID column must also be a QID column");
		all = Set.copyOf(all);
		aggregates = Set.copyOf(aggregates);
	}
}
