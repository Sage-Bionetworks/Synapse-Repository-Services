package org.sagebionetworks.table.cluster.description;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_BENEFACTOR;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.BenefactorColumn;
import org.sagebionetworks.repo.model.table.IndexDescriptionSnapshot;
import org.sagebionetworks.repo.model.table.SourceDependency;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A {@link QueryIndexDescription} reconstituted from the as-built
 * {@link IndexDescriptionSnapshot} captured with an index. It drives the
 * preflight transitive-dependency ACL check, the benefactor row-level filter,
 * and query translation against exactly what the served index contains, rather
 * than against current-truth state that may have drifted since the index was
 * built.
 * <p>
 * It is a pure reconstitution: id/version, table type, benefactors, and the
 * flattened dependency node set come from the snapshot; live change numbers
 * (for the query-cache hash) are supplied by an injected provider. It carries no
 * build-only ability, so it implements only {@link QueryIndexDescription}.
 */
public class SnapshotIndexDescription implements QueryIndexDescription {

	private final IdAndVersion idAndVersion;
	private final TableType tableType;
	private final List<BenefactorDescription> benefactors;
	private final List<SnapshotIndexDescription> dependencies;
	private final Function<IdAndVersion, Optional<Long>> changeNumberProvider;

	public SnapshotIndexDescription(IdAndVersion idAndVersion, TableType tableType,
			List<BenefactorDescription> benefactors, List<SnapshotIndexDescription> dependencies,
			Function<IdAndVersion, Optional<Long>> changeNumberProvider) {
		super();
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.required(tableType, "tableType");
		ValidateArgument.required(benefactors, "benefactors");
		ValidateArgument.required(dependencies, "dependencies");
		ValidateArgument.required(changeNumberProvider, "changeNumberProvider");
		this.idAndVersion = idAndVersion;
		this.tableType = tableType;
		this.benefactors = benefactors;
		this.dependencies = dependencies;
		this.changeNumberProvider = changeNumberProvider;
	}

	/**
	 * Reconstitute a query-time description from an as-built
	 * {@link IndexDescriptionSnapshot}.
	 *
	 * @param snapshot             the as-built authorization projection
	 * @param changeNumberProvider supplies the live last-change-number for an
	 *                             object (drives the query-cache hash); typically
	 *                             bound to {@code getLastTableChangeNumber}
	 * @return a reconstituted description
	 */
	public static SnapshotIndexDescription fromSnapshot(IndexDescriptionSnapshot snapshot,
			Function<IdAndVersion, Optional<Long>> changeNumberProvider) {
		ValidateArgument.required(snapshot, "snapshot");
		List<BenefactorDescription> benefactors = new ArrayList<>();
		if (snapshot.getBenefactors() != null) {
			for (BenefactorColumn column : snapshot.getBenefactors()) {
				benefactors.add(new BenefactorDescription(column.getBenefactorColumnName(),
						ObjectType.valueOf(column.getBenefactorType())));
			}
		}
		// Dependencies are stored pre-flattened; each becomes a childless node carrying
		// only id, version, and type so the transitive ACL check evaluates the same node
		// set as the live path.
		List<SnapshotIndexDescription> dependencies = new ArrayList<>();
		if (snapshot.getDependencies() != null) {
			for (SourceDependency dependency : snapshot.getDependencies()) {
				dependencies.add(new SnapshotIndexDescription(
						toIdAndVersion(dependency.getObjectId(), dependency.getVersionNumber()),
						TableType.valueOf(dependency.getTableType()), Collections.emptyList(), Collections.emptyList(),
						changeNumberProvider));
			}
		}
		return new SnapshotIndexDescription(toIdAndVersion(snapshot.getObjectId(), snapshot.getVersionNumber()),
				TableType.valueOf(snapshot.getTableType()), benefactors, dependencies, changeNumberProvider);
	}

	private static IdAndVersion toIdAndVersion(String objectId, Long versionNumber) {
		return IdAndVersion.parse(versionNumber == null ? objectId : objectId + "." + versionNumber);
	}

	@Override
	public IdAndVersion getIdAndVersion() {
		return idAndVersion;
	}

	@Override
	public TableType getTableType() {
		return tableType;
	}

	@Override
	public List<BenefactorDescription> getBenefactors() {
		return benefactors;
	}

	@Override
	public List<? extends QueryIndexDescription> getDependencies() {
		return dependencies;
	}

	@Override
	public List<ColumnToAdd> getColumnNamesToAddToSelect(SqlContext context, boolean includeEtag, boolean isAggregate) {
		if (!SqlContext.query.equals(context)) {
			throw new IllegalArgumentException("Only 'query' is supported for a snapshot index description");
		}
		if (isAggregate) {
			return Collections.emptyList();
		}
		// A view row references an object, so it carries an etag (optional) and a
		// benefactor; all other materialized types add only ROW_ID and ROW_VERSION.
		if (tableType.isViewEntityType()) {
			if (includeEtag) {
				return Arrays.asList(new ColumnToAdd(idAndVersion, ROW_ID), new ColumnToAdd(idAndVersion, ROW_VERSION),
						new ColumnToAdd(idAndVersion, ROW_ETAG), new ColumnToAdd(idAndVersion, ROW_BENEFACTOR));
			}
			return Arrays.asList(new ColumnToAdd(idAndVersion, ROW_ID), new ColumnToAdd(idAndVersion, ROW_VERSION),
					new ColumnToAdd(idAndVersion, ROW_BENEFACTOR));
		}
		return Arrays.asList(new ColumnToAdd(idAndVersion, ROW_ID), new ColumnToAdd(idAndVersion, ROW_VERSION));
	}

	@Override
	public Optional<Long> getLastTableChangeNumber() {
		return changeNumberProvider.apply(idAndVersion);
	}

	@Override
	public boolean addRowIdToSearchIndex() {
		// The row_id in a view is a reference to an object.
		return tableType.isViewEntityType();
	}

	@Override
	public int hashCode() {
		return Objects.hash(benefactors, dependencies, idAndVersion, tableType);
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
		return Objects.equals(benefactors, other.benefactors) && Objects.equals(dependencies, other.dependencies)
				&& Objects.equals(idAndVersion, other.idAndVersion) && tableType == other.tableType;
	}

	@Override
	public String toString() {
		return "SnapshotIndexDescription [idAndVersion=" + idAndVersion + ", tableType=" + tableType + ", benefactors="
				+ benefactors + ", dependencies=" + dependencies + "]";
	}

}
