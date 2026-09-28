package org.sagebionetworks.repo.manager.table.query;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnLineageEntry;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.IndexAuthorizationSnapshot;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A {@link SchemaProvider} that reproduces the as-built schema of any object
 * that has an {@link IndexAuthorizationSnapshot}, so query translation binds to
 * exactly the columns the served index contains rather than the object's current
 * (possibly drifted) binding.
 * <p>
 * Snapshots are resolved per object id: the queried object, and — when a
 * VirtualTable is inlined over a materialized source — the source (recursively,
 * for VirtualTable-over-VirtualTable). Any object without a snapshot (e.g. a
 * VirtualTable itself, which is inlined and never materialized) delegates to the
 * wrapped live provider.
 * <p>
 * A {@link ColumnModel} is immutable and content-addressed by id, so only the
 * as-built column id set is pinned by a snapshot; the column content behind each
 * id is still loaded live.
 */
public class SnapshotSchemaProvider implements SchemaProvider {

	private final SchemaProvider wrapped;
	private final Function<IdAndVersion, Optional<IndexAuthorizationSnapshot>> snapshotLookup;

	public SnapshotSchemaProvider(SchemaProvider wrapped,
			Function<IdAndVersion, Optional<IndexAuthorizationSnapshot>> snapshotLookup) {
		ValidateArgument.required(wrapped, "wrapped");
		ValidateArgument.required(snapshotLookup, "snapshotLookup");
		this.wrapped = wrapped;
		this.snapshotLookup = snapshotLookup;
	}

	@Override
	public TableType getTableType(IdAndVersion tableId) {
		return snapshotLookup.apply(tableId)
				.map(snapshot -> TableType.valueOf(snapshot.getIndexDescription().getTableType()))
				.orElseGet(() -> wrapped.getTableType(tableId));
	}

	@Override
	public List<ColumnModel> getTableSchema(IdAndVersion tableId) {
		Optional<IndexAuthorizationSnapshot> snapshot = snapshotLookup.apply(tableId);
		if (snapshot.isPresent()) {
			// The as-built schema is the pinned output-column id set (in select-list order),
			// each resolved to its immutable ColumnModel content live by id.
			return snapshot.get().getColumnLineage().stream().map(ColumnLineageEntry::getOutputColumnId)
					.map(wrapped::getColumnModel).collect(Collectors.toList());
		}
		return wrapped.getTableSchema(tableId);
	}

	@Override
	public ColumnModel getColumnModel(String id) {
		return wrapped.getColumnModel(id);
	}
}
