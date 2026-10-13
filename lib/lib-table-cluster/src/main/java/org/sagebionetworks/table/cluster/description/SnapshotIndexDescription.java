package org.sagebionetworks.table.cluster.description;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.BenefactorColumn;
import org.sagebionetworks.repo.model.table.ColumnLineageEntry;
import org.sagebionetworks.repo.model.table.IndexAuthorizationSnapshot;
import org.sagebionetworks.repo.model.table.IndexDescriptionSnapshot;
import org.sagebionetworks.repo.model.table.SourceDependency;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A {@link QueryIndexDescription} reconstituted from the as-built
 * {@link IndexAuthorizationSnapshot} captured with an index. It drives the
 * preflight transitive-dependency ACL check, the benefactor row-level filter,
 * and query translation against exactly what the served index contains, rather
 * than against current-truth state that may have drifted since the index was
 * built.
 * <p>
 * The snapshot supplies the authorization projection directly - benefactors, the
 * flattened dependency node set, and the column lineage - while the SQL-shaping
 * behavior is answered by the queried type's own {@link IndexDescription},
 * rebuilt from the captured {@link IndexDescriptionState} and delegated to. That
 * delegation is what makes a type whose query is not a plain select against its
 * own index work from a snapshot: a VirtualTable has no index at all and must
 * inline its defining SQL as a common table expression.
 * <p>
 * It implements the full {@link IndexDescription} (not just the narrow
 * {@link QueryIndexDescription}) and {@link IndexDescriptionLookup} so the
 * rebuilt description can resolve its own sources back out of the snapshot's
 * dependency set. It has no build-only ability, so
 * {@link #getCreateOrUpdateIndexSql()} is unreachable on the query path and
 * throws.
 */
public class SnapshotIndexDescription implements IndexDescription, IndexDescriptionLookup {

	private final IndexDescriptionState state;
	private final List<BenefactorDescription> benefactors;
	private final List<ColumnLineageEntry> columnLineage;
	private final Map<IdAndVersion, SnapshotIndexDescription> dependencies;
	private final Function<IdAndVersion, Optional<Long>> changeNumberProvider;
	private final boolean queryable;
	private IndexDescription delegate;

	/**
	 * Reconstitute a query-time description from an as-built
	 * {@link IndexAuthorizationSnapshot}.
	 *
	 * @param authorizationSnapshot the as-built authorization snapshot, including
	 *                              its column lineage
	 * @param changeNumberProvider  supplies an object's live table version (drives
	 *                              the query-cache hash so it invalidates on every
	 *                              index update); must match the value the live
	 *                              {@code IndexDescription} is built with, i.e.
	 *                              {@code TableManagerSupport.getTableVersion}
	 * @return a reconstituted description
	 */
	public static SnapshotIndexDescription fromSnapshot(IndexAuthorizationSnapshot authorizationSnapshot,
			Function<IdAndVersion, Optional<Long>> changeNumberProvider) {
		ValidateArgument.required(authorizationSnapshot, "authorizationSnapshot");
		ValidateArgument.required(changeNumberProvider, "changeNumberProvider");
		IndexDescriptionSnapshot snapshot = authorizationSnapshot.getIndexDescription();
		ValidateArgument.required(snapshot, "authorizationSnapshot.indexDescription");

		IndexDescriptionState state = new IndexDescriptionState(
				toIdAndVersion(snapshot.getObjectId(), snapshot.getVersionNumber()),
				TableType.valueOf(snapshot.getTableType()), snapshot.getDefiningSql(), snapshot.getIndexVersion());
		return new SnapshotIndexDescription(state, toBenefactorDescriptions(snapshot.getBenefactors()),
				authorizationSnapshot.getColumnLineage(),
				toDependencyNodes(snapshot.getDependencies(), changeNumberProvider), changeNumberProvider);
	}

	private SnapshotIndexDescription(IndexDescriptionState state, List<BenefactorDescription> benefactors,
			List<ColumnLineageEntry> columnLineage, Map<IdAndVersion, SnapshotIndexDescription> dependencies,
			Function<IdAndVersion, Optional<Long>> changeNumberProvider) {
		this.state = state;
		this.benefactors = benefactors;
		this.columnLineage = columnLineage;
		this.dependencies = dependencies;
		this.changeNumberProvider = changeNumberProvider;
		this.queryable = true;
	}

	/**
	 * A flattened dependency node, which the snapshot records by id, version, and type alone. It
	 * exists only to be part of the node set the transitive ACL check evaluates and to contribute
	 * its live change number to the query-cache hash, so it carries no benefactors, no nested
	 * dependencies, no lineage, and nothing to delegate SQL shaping to.
	 */
	private SnapshotIndexDescription(IdAndVersion idAndVersion, TableType tableType,
			Function<IdAndVersion, Optional<Long>> changeNumberProvider) {
		this.state = new IndexDescriptionState(idAndVersion, tableType, null, null);
		this.benefactors = Collections.emptyList();
		this.columnLineage = null;
		this.dependencies = Collections.emptyMap();
		this.changeNumberProvider = changeNumberProvider;
		this.queryable = false;
	}

	private static List<BenefactorDescription> toBenefactorDescriptions(List<BenefactorColumn> columns) {
		if (columns == null) {
			return Collections.emptyList();
		}
		List<BenefactorDescription> descriptions = new ArrayList<>(columns.size());
		for (BenefactorColumn column : columns) {
			descriptions.add(new BenefactorDescription(column.getBenefactorColumnName(),
					ObjectType.valueOf(column.getBenefactorType())));
		}
		return descriptions;
	}

	/**
	 * Dependencies are stored pre-flattened; each becomes a childless node so the transitive ACL
	 * check evaluates the same node set as the live path. They are keyed by id so a rebuilt
	 * defining-SQL description can resolve each source its SQL names back out of this set.
	 */
	private static Map<IdAndVersion, SnapshotIndexDescription> toDependencyNodes(List<SourceDependency> dependencies,
			Function<IdAndVersion, Optional<Long>> changeNumberProvider) {
		if (dependencies == null) {
			return Collections.emptyMap();
		}
		Map<IdAndVersion, SnapshotIndexDescription> nodes = new LinkedHashMap<>(dependencies.size());
		for (SourceDependency dependency : dependencies) {
			IdAndVersion dependencyId = toIdAndVersion(dependency.getObjectId(), dependency.getVersionNumber());
			nodes.put(dependencyId, new SnapshotIndexDescription(dependencyId,
					TableType.valueOf(dependency.getTableType()), changeNumberProvider));
		}
		return nodes;
	}

	private static IdAndVersion toIdAndVersion(String objectId, Long versionNumber) {
		return IdAndVersion.parse(versionNumber == null ? objectId : objectId + "." + versionNumber);
	}

	@Override
	public IndexDescription getIndexDescription(IdAndVersion idAndVersion) {
		SnapshotIndexDescription dependency = dependencies.get(idAndVersion);
		if (dependency == null) {
			// The snapshot's dependency closure is captured from the same defining SQL being rebuilt
			// here, so a source it does not contain means the snapshot and the SQL disagree.
			throw new IllegalStateException("The snapshot of " + getIdAndVersion() + " does not include its source "
					+ idAndVersion);
		}
		return dependency;
	}

	@Override
	public IdAndVersion getIdAndVersion() {
		return state.getIdAndVersion();
	}

	@Override
	public TableType getTableType() {
		return state.getTableType();
	}

	@Override
	public IndexDescriptionState getState() {
		return state;
	}

	@Override
	public List<BenefactorDescription> getBenefactors() {
		return benefactors;
	}

	@Override
	public List<IndexDescription> getDependencies() {
		return new ArrayList<>(dependencies.values());
	}

	@Override
	public List<ColumnLineageEntry> getColumnLineage() {
		if (columnLineage == null || columnLineage.isEmpty()) {
			// Every index has at least one output column, so an absent or empty lineage means none was
			// captured. Deferring to the default, which throws, is required: an empty lineage would
			// resolve zero quasi-identifier-derived columns and silently release protected rows.
			return IndexDescription.super.getColumnLineage();
		}
		return columnLineage;
	}

	@Override
	public String getCreateOrUpdateIndexSql() {
		// A snapshot description only drives the query path; it is never used to build an index.
		throw new UnsupportedOperationException("Cannot create or update the index of a snapshot description");
	}

	@Override
	public List<ColumnToAdd> getColumnNamesToAddToSelect(SqlContext context, boolean includeEtag, boolean isAggregate) {
		if (!SqlContext.query.equals(context)) {
			throw new IllegalArgumentException("Only 'query' is supported for a snapshot index description");
		}
		return asBuilt().getColumnNamesToAddToSelect(context, includeEtag, isAggregate);
	}

	@Override
	public String preprocessQuery(String sql) {
		return asBuilt().preprocessQuery(sql);
	}

	@Override
	public boolean supportQueryCache() {
		return asBuilt().supportQueryCache();
	}

	@Override
	public boolean addRowIdToSearchIndex() {
		return asBuilt().addRowIdToSearchIndex();
	}

	@Override
	public Optional<Long> getLastTableChangeNumber() {
		// Mirror the live subtypes exactly so the query-cache hash matches the live path. A
		// materialized view and a virtual table own no change number: the first is kept fresh
		// entirely by its dependencies' change numbers and the second is never materialized at all,
		// so both contribute nothing to the hash. Every other type contributes its live table
		// version, which is why this reads the provider rather than the as-built index version.
		switch (getTableType()) {
		case materializedview:
		case virtualtable:
			return Optional.empty();
		default:
			return changeNumberProvider.apply(getIdAndVersion());
		}
	}

	/**
	 * The rebuilt description that answers how this type's query is shaped.
	 */
	private IndexDescription asBuilt() {
		if (!queryable) {
			throw new IllegalStateException("The flattened dependency node " + getIdAndVersion()
					+ " is not queryable; only the object a snapshot was captured for can be queried");
		}
		if (delegate == null) {
			// Rebuilt on first use rather than at construction, because a caller that only needs the
			// authorization projection or the column lineage must not be blocked by state that exists
			// only to shape SQL. The rebuilt description resolves its own sources through this object
			// as its lookup, which is safe here because construction has already completed. A race
			// can only rebuild an equivalent description, so this needs no synchronization.
			delegate = IndexDescriptionFactory.createIndexDescription(state, this);
		}
		return delegate;
	}

	// Equality models the authorization identity of this description: the node set the transitive
	// ACL check evaluates. Neither the change-number provider nor the column lineage participates,
	// because neither can change which ACLs are consulted.
	@Override
	public int hashCode() {
		return Objects.hash(benefactors, dependencies.keySet(), getIdAndVersion(), getTableType());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SnapshotIndexDescription)) {
			return false;
		}
		SnapshotIndexDescription other = (SnapshotIndexDescription) obj;
		return Objects.equals(benefactors, other.benefactors)
				&& Objects.equals(dependencies.keySet(), other.dependencies.keySet())
				&& Objects.equals(getIdAndVersion(), other.getIdAndVersion()) && getTableType() == other.getTableType();
	}

	@Override
	public String toString() {
		return "SnapshotIndexDescription [idAndVersion=" + getIdAndVersion() + ", tableType=" + getTableType()
				+ ", benefactors=" + benefactors + ", dependencies=" + dependencies.keySet() + "]";
	}

}
