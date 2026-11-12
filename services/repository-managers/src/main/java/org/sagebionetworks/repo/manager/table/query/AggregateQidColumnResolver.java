package org.sagebionetworks.repo.manager.table.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.table.ColumnLineageEntry;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.DerivationKind;
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
	 * Resolve the queried object's own columns that carry a quasi-identifier of any source, as named by
	 * the configuration bound to that source.
	 *
	 * @param indexDescription the as-built description of the object being queried
	 * @return the matching columns. Empty when no column is derived from a quasi-identifier.
	 * @throws IllegalStateException if the description cannot supply a column lineage
	 */
	public QidColumns resolve(QueryIndexDescription indexDescription) {
		ValidateArgument.required(indexDescription, "indexDescription");
		// One configuration read per distinct source, however many of its columns the lineage names.
		Map<String, Set<String>> quasiIdentifiersBySource = new HashMap<>();
		return resolve(indexDescription,
				sourceId -> quasiIdentifiersBySource.computeIfAbsent(sourceId, this::quasiIdentifierNames));
	}

	/**
	 * Resolve the queried object's own columns that carry a quasi-identifier named by a data manager's
	 * preview configuration. The preview replaces every configuration bound to a source: it shows what
	 * aggregate-only users would see if the preview were the configuration, so its quasi-identifier
	 * names apply to every source the lineage reaches.
	 *
	 * @param indexDescription the as-built description of the object being queried
	 * @param preview          the configuration supplied on the request
	 * @return the matching columns. Empty when no column is derived from a quasi-identifier.
	 * @throws IllegalStateException if the description cannot supply a column lineage
	 */
	public QidColumns resolveWithPreview(QueryIndexDescription indexDescription, AggregateDataConfiguration preview) {
		ValidateArgument.required(indexDescription, "indexDescription");
		ValidateArgument.required(preview, "preview");
		Set<String> quasiIdentifierNames = normalizeAll(preview.getQuasiIdentifierColumnNames());
		return resolve(indexDescription, sourceId -> quasiIdentifierNames);
	}

	private QidColumns resolve(QueryIndexDescription indexDescription,
			Function<String, Set<String>> quasiIdentifierNamesBySource) {
		Set<String> all = new LinkedHashSet<>();
		Set<String> aggregates = new LinkedHashSet<>();
		for (ColumnLineageEntry entry : indexDescription.getColumnLineage()) {
			if (isDerivedFromQuasiIdentifier(entry, quasiIdentifierNamesBySource)) {
				all.add(entry.getOutputColumnId());
				if (DerivationKind.AGGREGATE.equals(entry.getDerivationKind())) {
					aggregates.add(entry.getOutputColumnId());
				}
			}
		}
		return new QidColumns(all, aggregates);
	}

	/**
	 * Every derivation kind qualifies. An aggregate or expression over a QID is still a QID-derived
	 * value: it was computed when the index was built, so the query-time cell-level k-anonymity that
	 * protects a {@code count} of a QID never saw it.
	 */
	private boolean isDerivedFromQuasiIdentifier(ColumnLineageEntry entry,
			Function<String, Set<String>> quasiIdentifierNamesBySource) {
		if (entry.getInputs() == null) {
			return false;
		}
		for (SourceColumnReference input : entry.getInputs()) {
			Set<String> quasiIdentifierNames = quasiIdentifierNamesBySource.apply(input.getSourceObjectId());
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
				.map(AggregateDataConfiguration::getQuasiIdentifierColumnNames)
				.map(AggregateQidColumnResolver::normalizeAll)
				.orElseGet(Collections::emptySet);
	}

	private static Set<String> normalizeAll(List<String> names) {
		if (names == null) {
			return Collections.emptySet();
		}
		return names.stream().filter(Objects::nonNull).map(AggregateQidColumnResolver::normalize)
				.collect(Collectors.toSet());
	}

	private static String normalize(String name) {
		return name == null ? null : name.trim().toUpperCase(Locale.ROOT);
	}

}
