package org.sagebionetworks.repo.manager.grid.synch.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sagebionetworks.repo.manager.grid.internal.replica.change.AddColumnChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.DeleteArrayNodeChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateColumnNamesChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.synch.core.SyncOutcomeHandler;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandler;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceWriter;
import org.sagebionetworks.repo.model.grid.GridConstants;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * Responsible for applying schema changes to the CRDT replica via the
 * {@link IntendedChangePublisher}, pushing user schema changes to the
 * source via the {@link SourceWriter}, and tracking the reconciled
 * {@link #getFinalSchema() final schema} used by Phase 2.
 */
public class SchemaSyncOutcomeHandler implements SyncOutcomeHandler<ColumnCopyItem, ColumnSourceItem>, AutoCloseable {

	private final IntendedChangePublisher intendedChangePublisher;
	private final SourceWriter sourceWriter;
	private final List<ColumnCopyItem> copyColumns;
	private final List<Column> finalSchema;
	private final LogicalTimestamp columnOrderArrId;
	private final LogicalTimestamp columnNamesVecId;
	private int nextColumnIndex;
	private boolean hasSchemaChange;
	private boolean hasUpdatedColumnNameVec;

	public SchemaSyncOutcomeHandler(IntendedChangePublisher intendedChangePublisher, CopyHandler copyHandler,
			SourceWriter sourceWriter) {
		this.intendedChangePublisher = intendedChangePublisher;
		this.sourceWriter = sourceWriter;
		GridHeader header = copyHandler.getHeader();
		this.columnOrderArrId = header.getColumnOrderArrId();
		this.columnNamesVecId = header.getColumnNamesVecId();

		this.copyColumns = new ArrayList<>();
		for (Column c : header.getOrderedColumns()) {
			boolean wasChangedByUser = GridConstants.isUserReplica(c.getColumnOrderNodeId().getRep());
			copyColumns.add(new ColumnCopyItem().setColumnName(c.getName()).setWasChangedByUser(wasChangedByUser)
					.setColumnOrderNodeId(c.getColumnOrderNodeIdAsLogical()));
		}

		this.finalSchema = new ArrayList<>(header.getOrderedColumns());
		this.nextColumnIndex = calculateNextColumnIndex();
		this.hasSchemaChange = false;
		this.hasUpdatedColumnNameVec = false;
	}

	/**
	 * The copy columns to classify during Phase 1 (the classifier's input stream).
	 */
	public Stream<ColumnCopyItem> streamCopyItems() {
		return copyColumns.stream();
	}

	int calculateNextColumnIndex() {
		return finalSchema.stream().mapToInt(Column::getVectorIndex).max().orElse(-1) + 1;
	}

	@Override
	public void onCopyAndSourceMatch(ColumnCopyItem copyItem, ColumnSourceItem sourceItem) {
		// Matched column: already present in the final schema, nothing to do.
	}

	@Override
	public void onCopyAndSourceConflict(ColumnCopyItem copyItem, ColumnSourceItem sourceItem) {
		// Columns are matched by name, so a same-key mismatch cannot occur.
		throw new IllegalStateException("Schema columns cannot conflict: they are matched by name.");
	}

	@Override
	public void onNewCopyItem(ColumnCopyItem copyItem, String key) {
		if (sourceWriter.canAddRemoveColumns()) {
			// Push the addition to the source; the column stays in the grid.
			sourceWriter.addColumnToSource(key);
		} else {
			// The source cannot accept the column - drop it from the grid.
			removeColumnFromCopy(copyItem);
		}
	}

	@Override
	public void onDeletedFromSource(ColumnCopyItem copyItem) {
		// Column was deleted from the source - remove it from the grid.
		removeColumnFromCopy(copyItem);
	}

	@Override
	public void onDeletedFromCopy(ColumnSourceItem sourceItem) {
		if (sourceWriter.canAddRemoveColumns()) {
			// Push the user's column deletion to the source; do not re-import it.
			sourceWriter.removeColumn(sourceItem.getColumnName());
		} else {
			// The source cannot remove the column - pull it back into the grid.
			addColumnToCopy(sourceItem);
		}
	}

	@Override
	public void onNewSourceItem(ColumnSourceItem sourceItem) {
		// Column was added to the source - pull it into the grid.
		addColumnToCopy(sourceItem);
	}

	private void removeColumnFromCopy(ColumnCopyItem item) {
		intendedChangePublisher.publish(new DeleteArrayNodeChange(columnOrderArrId, item.getColumnOrderNodeId()));
		finalSchema.removeIf(column -> column.getName().equals(item.getColumnName()));
		hasSchemaChange = true;
	}

	private void addColumnToCopy(ColumnSourceItem item) {
		Column newColumn = new Column().setName(item.getColumnName()).setVectorIndex(nextColumnIndex);
		intendedChangePublisher.publish(new AddColumnChange(columnOrderArrId, columnOrderArrId, (long) nextColumnIndex));
		finalSchema.add(newColumn);
		nextColumnIndex++;
		hasSchemaChange = true;
	}

	/**
	 * The reconciled schema after Phase 1, used to drive Phase 2 row
	 * synchronization.
	 */
	public List<Column> getFinalSchema() {
		return finalSchema;
	}

	@Override
	public void close() {
		if (hasSchemaChange && !hasUpdatedColumnNameVec) {
			Map<Integer, String> indexToNameMap = finalSchema.stream()
					.collect(Collectors.toMap(Column::getVectorIndex, Column::getName));
			intendedChangePublisher.publish(new UpdateColumnNamesChange(columnNamesVecId, indexToNameMap));
			hasUpdatedColumnNameVec = true;
		}
	}

}
