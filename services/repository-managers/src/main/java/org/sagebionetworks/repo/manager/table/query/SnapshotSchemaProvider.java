package org.sagebionetworks.repo.manager.table.query;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnLineageEntry;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.IndexAuthorizationSnapshot;
import org.sagebionetworks.repo.model.table.IndexDescriptionSnapshot;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A {@link SchemaProvider} that reproduces the as-built schema of a queried
 * object from its {@link IndexAuthorizationSnapshot}, so query translation binds
 * to exactly the columns the served index contains rather than the object's
 * current (possibly drifted) binding.
 * <p>
 * A {@link ColumnModel} is immutable and content-addressed by id, so only the
 * as-built column id set is pinned by the snapshot; the column content behind
 * each id is still loaded live. For the queried object the type and the ordered
 * id set come from the snapshot; every other request (and all column-content
 * loads) delegate to the wrapped live provider.
 */
public class SnapshotSchemaProvider implements SchemaProvider {

	private final SchemaProvider wrapped;
	private final IdAndVersion objectId;
	private final TableType tableType;
	private final List<String> outputColumnIds;

	public SnapshotSchemaProvider(SchemaProvider wrapped, IndexAuthorizationSnapshot snapshot) {
		ValidateArgument.required(wrapped, "wrapped");
		ValidateArgument.required(snapshot, "snapshot");
		ValidateArgument.required(snapshot.getIndexDescription(), "snapshot.indexDescription");
		this.wrapped = wrapped;
		IndexDescriptionSnapshot description = snapshot.getIndexDescription();
		this.objectId = IdAndVersion.parse(description.getVersionNumber() == null ? description.getObjectId()
				: description.getObjectId() + "." + description.getVersionNumber());
		this.tableType = TableType.valueOf(description.getTableType());
		this.outputColumnIds = snapshot.getColumnLineage() == null ? Collections.emptyList()
				: snapshot.getColumnLineage().stream().map(ColumnLineageEntry::getOutputColumnId)
						.collect(Collectors.toList());
	}

	@Override
	public TableType getTableType(IdAndVersion tableId) {
		if (objectId.equals(tableId)) {
			return tableType;
		}
		return wrapped.getTableType(tableId);
	}

	@Override
	public List<ColumnModel> getTableSchema(IdAndVersion tableId) {
		if (objectId.equals(tableId)) {
			// The as-built schema is the pinned output-column id set (in select-list order),
			// each resolved to its immutable ColumnModel content live by id.
			return outputColumnIds.stream().map(wrapped::getColumnModel).collect(Collectors.toList());
		}
		return wrapped.getTableSchema(tableId);
	}

	@Override
	public ColumnModel getColumnModel(String id) {
		return wrapped.getColumnModel(id);
	}
}
