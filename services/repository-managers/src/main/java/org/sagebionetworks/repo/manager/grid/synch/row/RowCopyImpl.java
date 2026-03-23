package org.sagebionetworks.repo.manager.grid.synch.row;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.sagebionetworks.repo.manager.grid.internal.replica.change.DeleteArrayNodeChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.InsertRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandler;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReference;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

import com.google.common.base.Functions;

/**
 * Implementation of {@link RowCopy} that provides access to rows from the CRDT
 * replica and operations for modifying the copy during Phase 2 row
 * synchronization.
 *
 * <p>
 * This class bridges the synchronization logic with the underlying CRDT replica
 * by:
 * <ul>
 * <li>Streaming rows from the replica via {@link CopyHandler}</li>
 * <li>Publishing intended changes to the replica via
 * {@link IntendedChangePublisher}</li>
 * <li>Managing CRDT metadata (RGA node IDs) for row ordering and identity</li>
 * <li>Mapping column names to column metadata for proper row construction</li>
 * </ul>
 *
 * <p>
 * Key responsibilities:
 * <ul>
 * <li>Provide streaming access to copy rows during synchronization (memory
 * efficient)</li>
 * <li>Add new rows to the copy when they're added to the source (pulling source
 * additions)</li>
 * <li>Remove rows from the copy when they're deleted from the source (pulling
 * source deletions)</li>
 * <li>Track which rows were deleted by the user (to push deletions to
 * source)</li>
 * </ul>
 */
public class RowCopyImpl implements RowCopy {

	private final IntendedChangePublisher intendedChangePublisher;
	private final CopyHandler copyHandler;
	private final LogicalTimestamp rowsArrayId;
	private final LogicalTimestamp lastRowId;
	private final Map<String, Column> columnNameMap;

	/**
	 * Creates a new row copy implementation for synchronization.
	 *
	 * @param finalSchema             the synchronized schema after Phase 1 (column
	 *                                synchronization), used to map column names to
	 *                                metadata
	 * @param intendedChangePublisher the publisher for sending row changes to the
	 *                                CRDT replica
	 * @param copyHandler             the handler for reading rows from the replica
	 */
	public RowCopyImpl(List<Column> finalSchema, IntendedChangePublisher intendedChangePublisher,
			CopyHandler copyHandler) {
		super();
		this.intendedChangePublisher = intendedChangePublisher;
		this.copyHandler = copyHandler;
		this.rowsArrayId = copyHandler.getHeader().getRowsId();
		this.lastRowId = copyHandler.getLastRowsRgaNodeId();
		this.columnNameMap = finalSchema.stream().collect(Collectors.toMap(Column::getName, Functions.identity()));
	}

	/**
	 * Streams all rows from the copy (CRDT replica) for synchronization with the
	 * source. Uses the {@link CopyHandler} to provide memory-efficient streaming of
	 * rows without loading all data into memory.
	 *
	 * <p>
	 * Called during Phase 1 of {@link SynchronizationLogic#synchronize} to compare
	 * copy rows with source rows.
	 *
	 * @return a stream of rows from the copy
	 */
	@Override
	public Stream<RowCopyItem> streamItems() {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(copyHandler.getRows(),
				Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
	}

	/**
	 * Determines whether a row with the given key was deleted by the user in the
	 * copy. This is used during Phase 2 of {@link SynchronizationLogic#synchronize}
	 * to decide whether to push deletions to the source or pull additions from the
	 * source.
	 *
	 * <p>
	 * <strong>TODO:</strong> Currently returns false, meaning rows deleted from the
	 * copy will be added back from the source during synchronization. A future
	 * implementation needs to track user deletions in the CRDT to properly push
	 * user deletions to the source.
	 *
	 * @param key the source identifier of the row to check
	 * @return true if the user deleted this row, false otherwise (currently always
	 *         false)
	 */
	@Override
	public boolean wasDeletedByUser(String key) {
		/*
		 * TODO: find a way to determine if a row was deleted by the user. For now,
		 * return false which means if a row was deleted in the grid, it will be added
		 * back from the source during synchronization.
		 */
		return false;
	}

	/**
	 * Removes a row from the copy by publishing a delete change to the CRDT
	 * replica. Called during synchronization when a row exists in the copy but not
	 * in the source, and the row was not changed by the user (meaning it was
	 * deleted from the source).
	 *
	 * <p>
	 * The delete uses the row's RGA node ID to maintain consistent CRDT semantics
	 * across replicas.
	 *
	 * @param item the row to remove from the copy
	 */
	@Override
	public void removeItem(RowCopyItem item) {
		intendedChangePublisher.publish(new DeleteArrayNodeChange(rowsArrayId, item.getRgaNodeId()));
	}

	/**
	 * Adds a new row to the copy by publishing an insert change to the CRDT
	 * replica. Called during synchronization when a row exists in the source but
	 * not in the copy, and the row was not deleted by the user (meaning it was
	 * added to the source).
	 *
	 * <p>
	 * The insert operation:
	 * <ul>
	 * <li>Fetches the row data from the source via
	 * {@link RowSourceItemReference#fetchRow()}</li>
	 * <li>Maps column names to column indices using the synchronized schema</li>
	 * <li>Filters out columns that don't exist in the schema</li>
	 * <li>Inserts the row after the last known row (using {@link #lastRowId})</li>
	 * <li>Assigns proper RGA ordering within the rows array</li>
	 * </ul>
	 *
	 * @param sourceItem the header for the source row to add to the copy
	 */
	@Override
	public void addItem(RowSourceItemReference sourceItem) {
		List<ConValue> values = new ArrayList<>();
		List<Integer> valueIndex = new ArrayList<>();
		RowSourceItem synchRow = sourceItem.fetchRow();
		for (Entry<String, ConValue> e : synchRow.getData().entrySet()) {
			Column column = columnNameMap.get(e.getKey());
			if (column != null) {
				values.add(e.getValue());
				valueIndex.add(column.getVectorIndex());
			}
		}
		intendedChangePublisher.publish(new InsertRowChange(rowsArrayId, lastRowId, values,
				valueIndex.toArray(Integer[]::new), synchRow.getSynapseRow().map(SynapseRow::toConValue).orElse(null)));
	}

}
