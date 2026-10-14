package org.sagebionetworks.repo.manager.table.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.table.ColumnLineageEntry;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.SourceColumnReference;
import org.sagebionetworks.table.cluster.description.QueryIndexDescription;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * Resolves which of a queried object's own columns carry a quasi-identifier (QID) of
 * an aggregate-only source, so the count-only QID restriction can be enforced against
 * the columns the caller's query actually reads.
 * <p>
 * A defining-SQL object may rename, aggregate, or otherwise transform a source QID
 * ({@code select foo as bar from syn123}), and may do so through a chain of
 * intermediate objects, so the queried object's column is a different ColumnModel than
 * the source's. The decision is therefore made from the queried object's as-built
 * column lineage: any output column whose flattened lineage reaches a QID leaf is a
 * QID of the queried object.
 */
@Service
public class AggregateQidColumnResolver {

	private final TableManagerSupport tableManagerSupport;

	public AggregateQidColumnResolver(TableManagerSupport tableManagerSupport) {
		this.tableManagerSupport = tableManagerSupport;
	}

	/**
	 * Resolve the ids of the queried object's own columns that carry a quasi-identifier of any
	 * source.
	 *
	 * @param indexDescription the as-built description of the object being queried
	 * @return the matching column ids. Empty when no column is derived from a quasi-identifier.
	 * @throws IllegalStateException if the description cannot supply a column lineage
	 */
	public Set<String> resolve(QueryIndexDescription indexDescription) {
		ValidateArgument.required(indexDescription, "indexDescription");
		// One configuration read per distinct source, however many of its columns the lineage names.
		Map<String, Set<String>> quasiIdentifiersBySource = new HashMap<>();
		Set<String> resolved = new LinkedHashSet<>();
		for (ColumnLineageEntry entry : indexDescription.getColumnLineage()) {
			if (isDerivedFromQuasiIdentifier(entry, quasiIdentifiersBySource)) {
				resolved.add(entry.getOutputColumnId());
			}
		}
		return resolved;
	}

	/**
	 * Every derivation kind qualifies. An aggregate or expression over a QID is still a QID-derived
	 * value: it was computed when the index was built, so the query-time cell-level k-anonymity that
	 * protects a {@code count} of a QID never saw it.
	 */
	private boolean isDerivedFromQuasiIdentifier(ColumnLineageEntry entry,
			Map<String, Set<String>> quasiIdentifiersBySource) {
		if (entry.getInputs() == null) {
			return false;
		}
		for (SourceColumnReference input : entry.getInputs()) {
			Set<String> quasiIdentifierNames = quasiIdentifiersBySource
					.computeIfAbsent(input.getSourceObjectId(), this::quasiIdentifierNames);
			if (quasiIdentifierNames.isEmpty()) {
				continue;
			}
			// A configuration names its QID columns while the lineage pins column ids, so the leaf's
			// own ColumnModel bridges the two. Resolving an id cannot drift: a ColumnModel is
			// immutable and content-addressed by that id.
			ColumnModel columnModel = tableManagerSupport.getColumnModel(input.getSourceColumnId());
			if (columnModel == null) {
				throw new IllegalStateException(
						"Column model not found for id: " + input.getSourceColumnId() + " from source " + input.getSourceObjectId());
			}
			String leafColumnName = columnModel.getName();
			if (quasiIdentifierNames.contains(normalize(leafColumnName))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The QID verdict is deliberately read live, because revoking a QID must take effect on the next
	 * query rather than on the next index build.
	 */
	private Set<String> quasiIdentifierNames(String sourceObjectId) {
		return tableManagerSupport.getAggregateDataConfiguration(sourceObjectId)
				.map(AggregateDataConfiguration::getQuasiIdentifierColumnNames).filter(Objects::nonNull)
				.map(names -> names.stream().map(AggregateQidColumnResolver::normalize).collect(Collectors.toSet()))
				.orElseGet(Collections::emptySet);
	}

	private static String normalize(String name) {
		return name == null ? null : name.trim().toUpperCase(Locale.ROOT);
	}

}
