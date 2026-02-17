package org.sagebionetworks.repo.manager.grid.synch.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sagebionetworks.repo.manager.grid.internal.replica.change.AddColumn;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.DeleteColumn;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateColumnNames;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.synch.core.SynchronizationLogic;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandler;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * Implementation of {@link SchemaCopy} that provides access to the copy schema
 * and operations for modifying the copy during Phase 1 schema synchronization.
 *
 * <p>
 * This class bridges the synchronization logic with the CRDT replica by:
 * <ul>
 * <li>Reading the current schema state from {@link GridHeader}</li>
 * <li>Publishing schema changes (additions, deletions) to the CRDT via
 * {@link IntendedChangePublisher}</li>
 * <li>Tracking the final synchronized schema for use in Phase 2 row
 * synchronization</li>
 * <li>Managing vector indices for new columns to ensure unique CRDT
 * identifiers</li>
 * </ul>
 *
 * <p>
 * Key responsibilities:
 * <ul>
 * <li>Stream copy columns for comparison with source during Phase 1 of
 * {@link SynchronizationLogic#synchronize}</li>
 * <li>Remove columns that were deleted from source (pulling external
 * deletions)</li>
 * <li>Add columns that were added to source (pulling external additions)</li>
 * <li>Maintain the final synchronized schema for Phase 2</li>
 * <li>Assign unique vector indices to new columns for CRDT operations</li>
 * </ul>
 *
 * <p>
 * The {@link #finalSchema} represents the reconciled schema after Phase 1
 * completes, containing:
 * <ul>
 * <li>Vector indices mapping column names to CRDT vector positions</li>
 * <li>RGA node IDs for conflict\-free column ordering</li>
 * <li>Column metadata needed for Phase 2 row synchronization</li>
 * </ul>
 *
 * <p>
 * Column change tracking: The {@link #wasChangedByUser} flag for each column is
 * determined by comparing the column's RGA node replica ID with the internal
 * replica ID. If they differ, the column was added by a user (external
 * replica); if they match, the column was added by the source (internal
 * replica).
 */
public class SchemaCopyImpl implements SchemaCopy {

	private final IntendedChangePublisher intendedChangePublisher;
	private final List<ColumnCopyItem> schema;
	private final List<Column> finalSchema;
	private final LogicalTimestamp columnOrderArrId;
	private final LogicalTimestamp columnNamesVecId;
	private int nextColumnIndex;
	private boolean hasSchemaChange;
	private boolean hasUpdatedColumnNameVec;

	/**
	 * Creates a new schema copy implementation for synchronization with the CRDT
	 * replica.
	 *
	 * <p>
	 * Initializes the copy schema from the {@link GridHeader} and determines which
	 * columns were changed by users vs. added by the source. A column is considered
	 * "changed by user" if its RGA node replica ID differs from the internal
	 * replica ID (meaning it was added from an external replica, not the source).
	 *
	 * @param intendedChangePublisher the publisher for applying schema changes to
	 *                                the CRDT
	 * @param copyHandler             the reader providing access to the current
	 *                                copy state
	 */
	public SchemaCopyImpl(IntendedChangePublisher intendedChangePublisher, CopyHandler copyHandler) {
		this.intendedChangePublisher = intendedChangePublisher;
		GridHeader header = copyHandler.getHeader();
		columnOrderArrId = header.getColumnOrderArrId();
		columnNamesVecId = header.getColumnNamesVecId();
		long internalReplica = copyHandler.getConnectionInfo().getReplicaId();

		this.schema = new ArrayList<>();
		for (Column c : header.getOrderedColumns()) {
			boolean wasChangedByUser = !c.getColumnOrderNodeId().getRep().equals(internalReplica);
			ColumnCopyItem item = new ColumnCopyItem().setColumnName(c.getName()).setWasChangedByUser(wasChangedByUser)
					.setColumnOrderNodeId(c.getColumnOrderNodeIdAsLogical());
			schema.add(item);
		}

		this.finalSchema = new ArrayList<>(header.getOrderedColumns());
		nextColumnIndex = calculateNextColumnIndex();
		hasSchemaChange = false;
		hasUpdatedColumnNameVec = false;
	}

	/**
	 * Calculates the next available vector index for new columns. Vector indices
	 * must be unique within the CRDT to identify columns in the vector\-based row
	 * storage. Returns one plus the maximum existing vector index.
	 *
	 * @return the next available vector index for a new column
	 */
	int calculateNextColumnIndex() {
		return finalSchema.stream().mapToInt(Column::getVectorIndex).max().orElse(-1) + 1;
	}

	/**
	 * Streams all columns from the copy schema for comparison with the source
	 * schema. Called during Phase 1 of {@link SynchronizationLogic#synchronize} to
	 * process each copy column and match it with the corresponding source column.
	 *
	 * @return a stream of copy columns
	 */
	@Override
	public Stream<ColumnCopyItem> streamItems() {
		return schema.stream();
	}

	/**
	 * Determines whether a column was deleted by the user from the copy. This is
	 * used during Phase 2 of {@link SynchronizationLogic#synchronize} to decide
	 * whether to remove the column from the source (push user deletion) or add it
	 * to the copy (external addition).
	 *
	 * <p>
	 * TODO: Find a way to determine if a column was deleted by the user. For now,
	 * returns false which means if a column was deleted in the grid, it will be
	 * added back from the source during synchronization.
	 *
	 * @param key the column name to check
	 * @return false (always, until user deletion tracking is implemented)
	 */
	@Override
	public boolean wasDeletedByUser(String key) {
		/*
		 * TODO: find a way to determine if a column was deleted by the user. For now,
		 * return false which means if a column was deleted in the grid, it will be
		 * added back from the source during synchronization.
		 * 
		 */
		return false;
	}

	/**
	 * Removes a column from the copy schema. Called during Phase 1 of
	 * {@link SynchronizationLogic#synchronize} when a column exists in the copy but
	 * not in the source, and was not changed by the user (pulling external deletion
	 * from source).
	 *
	 * <p>
	 * Publishes a {@link DeleteColumn} change to the CRDT using the column's RGA
	 * node ID, then removes the column from the final schema used in Phase 2.
	 *
	 * @param item the copy column to remove
	 */
	@Override
	public void removeItem(ColumnCopyItem item) {
		intendedChangePublisher.publish(new DeleteColumn(columnOrderArrId, item.getColumnOrderNodeId()));
		finalSchema.removeIf(column -> column.getName().equals(item.getColumnName()));
		hasSchemaChange = true;
	}

	/**
	 * Adds a column to the copy schema. Called during Phase 2 of
	 * {@link SynchronizationLogic#synchronize} when a column exists in the source
	 * but not in the copy, and was not deleted by the user (pulling external
	 * addition from source).
	 *
	 * <p>
	 * Creates a new {@link Column} with a unique vector index, publishes an
	 * {@link AddColumn} change to the CRDT with the column name and vector index,
	 * then adds the column to the final schema. The vector index enables the CRDT
	 * to store column values efficiently in vector\-based row storage.
	 *
	 * @param item the source column to add to the copy
	 */
	@Override
	public void addItem(ColumnSourceItem item) {
		Column newColumn = new Column().setName(item.getColumnName()).setVectorIndex(nextColumnIndex);
		intendedChangePublisher.publish(new AddColumn(columnOrderArrId, columnOrderArrId, (long) nextColumnIndex));
		finalSchema.add(newColumn);
		nextColumnIndex++;
		hasSchemaChange = true;
	}
	

	/**
	 * Gets the final synchronized schema after Phase 1 schema synchronization
	 * completes. This schema represents the agreed\-upon column structure between
	 * copy and source, used during Phase 2 for:
	 * <ul>
	 * <li>Mapping column names to vector indices for CRDT row operations</li>
	 * <li>Validating row data structure during comparison</li>
	 * <li>Enabling cell\-level comparison during row merging</li>
	 * </ul>
	 *
	 * @return ordered list of columns in the synchronized schema with CRDT metadata
	 */
	@Override
	public List<Column> getFinalSchema() {
		return finalSchema;
	}
	
	/**
	 * Finalizes schema synchronization by publishing column name updates to the CRDT.
	 * Must be called after Phase 1 schema synchronization completes and before using
	 * the final schema in Phase 2.
	 * 
	 * <p>
	 * This ensures the CRDT's column name vector is synchronized with any schema
	 * changes (additions/deletions) made during Phase 1.
	 */
	@Override
	public void close() throws Exception {
	    if (hasSchemaChange && !hasUpdatedColumnNameVec) {
	        Map<Integer, String> indexToNameMap = finalSchema.stream()
	                .collect(Collectors.toMap(Column::getVectorIndex, Column::getName));
	        intendedChangePublisher.publish(new UpdateColumnNames(columnNamesVecId, indexToNameMap));
	        hasUpdatedColumnNameVec = true;
	    }
	}

}
