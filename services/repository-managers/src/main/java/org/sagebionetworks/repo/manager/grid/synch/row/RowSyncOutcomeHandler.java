package org.sagebionetworks.repo.manager.grid.synch.row;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.DeleteArrayNodeChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.InsertRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.synch.core.SyncOutcomeHandler;
import org.sagebionetworks.repo.manager.grid.synch.core.SynchronizationLogic;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandler;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceWriter;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.base.Functions;

/**
 * Handles row synchronization outcomes. It is responsible for applying row changes
 * to the replica via the {@link IntendedChangePublisher}, resolving row conflicts
 * by a nested cell-level synchronize, pushing changes to the source through the
 * {@link SourceWriter}, and reporting the final state of every row to the writer
 * so a pushed artifact can capture the final merge contents.
 *
 * <p>
 * When {@code preserveUserAttribution == true} (a PULL) the cells the user
 * changed are not rewritten in the grid during a merge, so their user-owned CRDT
 * nodes keep their change-attribution across subsequent syncs.
 */
public class RowSyncOutcomeHandler implements SyncOutcomeHandler<RowCopyItem, RowSourceItemReference> {

	private static final Logger log = LogManager.getLogger(RowSyncOutcomeHandler.class);

	private final SynchronizationLogic logic;
	private final IntendedChangePublisher intendedChangePublisher;
	private final CopyHandler copyHandler;
	private final SourceWriter sourceWriter;
	private final LogicalTimestamp rowsArrayId;
	private final LogicalTimestamp lastRowId;
	private final Map<String, Column> columnNameMap;
	private final boolean preserveUserAttribution;

	public RowSyncOutcomeHandler(SynchronizationLogic logic, IntendedChangePublisher intendedChangePublisher,
			CopyHandler copyHandler, SourceWriter sourceWriter, List<Column> finalSchema, boolean preserveUserAttribution) {
		this.logic = logic;
		this.intendedChangePublisher = intendedChangePublisher;
		this.copyHandler = copyHandler;
		this.sourceWriter = sourceWriter;
		this.rowsArrayId = copyHandler.getHeader().getRowsId();
		this.lastRowId = copyHandler.getLastRowsRgaNodeId();
		this.columnNameMap = finalSchema.stream().collect(Collectors.toMap(Column::getName, Functions.identity()));
		this.preserveUserAttribution = preserveUserAttribution;
	}

	public Stream<RowCopyItem> streamCopyItems() {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(copyHandler.getRows(),
				Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
	}

	@Override
	public void onCopyAndSourceMatch(RowCopyItem copyItem, RowSourceItemReference sourceItemRef) {
		sourceWriter.recordFinalRowState(cellsAsMap(copyItem));
	}

	@Override
	public void onCopyAndSourceConflict(RowCopyItem copyItem, RowSourceItemReference sourceItemRef) {
		String rowKey = sourceItemRef.getKey();
		RowSourceItem sourceItem = sourceItemRef.fetchRow();

		List<CellCopyItem> copyCells = copyItem.getCells().stream()
				.filter(c -> columnNameMap.containsKey(c.getName())).collect(Collectors.toList());
		CellSyncOutcomeHandler cellHandler = new CellSyncOutcomeHandler(copyCells);
		CellSourceReader cellReader = new CellSourceReader(sourceItem);
		CellSyncRules cellRules = new CellSyncRules(CellSyncOutcomeHandler.getUserDeletedCells(copyItem));

		// Resolve the conflict by synchronizing the row's cells (nested reuse of the engine).
		logic.synchronize(copyCells.stream(), cellReader, cellRules, cellHandler);

		// Apply the user's cell changes to the source row.
		if (!cellHandler.getUserChangedCells().isEmpty()) {
			try {
				sourceWriter.applyCellChangesFromCopyToSource(rowKey, cellHandler.getUserChangedCells());
			} catch (IllegalArgumentException ex) {
				log.warn("Failed to merge row: {}.  Row will not be reset in the grid.  Error message: {}", rowKey,
						ex.getMessage());
				return;
			} catch (NotFoundException | UnauthorizedException ex) {
				log.warn("Row: {} will be removed from the grid with message: {}", rowKey, ex.getMessage());
				// Source row was deleted or became unauthorized - delete from copy for consistency.
				intendedChangePublisher.publish(new DeleteArrayNodeChange(rowsArrayId, copyItem.getRgaNodeId()));
				return;
			}
		}

		// Reset the copy row with the merged result, then report it as a surviving row.
		Set<String> excludedCells = preserveUserAttribution ? cellHandler.getUserWonCells() : Set.of();
		resetCopyRow(copyItem.getVectorNodeId(), cellHandler.getMergedCells(), excludedCells,
				copyItem.getMetadataNodeId(), sourceItem.getSynapseRow().map(SynapseRow::toConValue).orElse(null));
		sourceWriter.recordFinalRowState(cellHandler.getMergedCells());
	}

	@Override
	public void onNewCopyItem(RowCopyItem copyItem, String key) {
		if (sourceWriter.canAddRemoveRows()) {
			RowSourceItem synchRow = RowSyncRules.createSynchRow(copyItem, key);
			sourceWriter.addNewRowToSource(synchRow);
			sourceWriter.recordFinalRowState(synchRow.getData());
		} else {
			// The source cannot accept the row - drop it from the grid.
			intendedChangePublisher.publish(new DeleteArrayNodeChange(rowsArrayId, copyItem.getRgaNodeId()));
		}
	}

	@Override
	public void onDeletedFromSource(RowCopyItem copyItem) {
		// Row was deleted from the source - remove it from the grid.
		intendedChangePublisher.publish(new DeleteArrayNodeChange(rowsArrayId, copyItem.getRgaNodeId()));
	}

	@Override
	public void onDeletedFromCopy(RowSourceItemReference sourceItemRef) {
		if (sourceWriter.canAddRemoveRows()) {
			// Push the user's row deletion to the source; do not re-import it.
			sourceWriter.removeRow(sourceItemRef.fetchRow());
		} else {
			// The source cannot remove the row - pull it back into the grid.
			RowSourceItem sourceItem = sourceItemRef.fetchRow();
			addRowToCopy(sourceItem);
			sourceWriter.recordFinalRowState(sourceItem.getData());
		}
	}

	@Override
	public void onNewSourceItem(RowSourceItemReference sourceItemRef) {
		// Row was added to the source - pull it into the grid.
		RowSourceItem sourceItem = sourceItemRef.fetchRow();
		addRowToCopy(sourceItem);
		sourceWriter.recordFinalRowState(sourceItem.getData());
	}

	/**
	 * Inserts a source row into the grid after the last known row, mapping column
	 * names to vector indices and dropping any columns absent from the final schema.
	 */
	private void addRowToCopy(RowSourceItem synchRow) {
		List<ConValue> values = new ArrayList<>();
		List<Integer> valueIndex = new ArrayList<>();
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

	/**
	 * Rewrites a copy row with the merged cell values, omitting any preserved
	 * (user-winning) cells so their user-owned CRDT nodes — and their
	 * change-attribution — survive. When every writable cell is preserved, there is
	 * nothing to rewrite, so no (empty) update is published.
	 */
	void resetCopyRow(LogicalTimestamp vectorNodeId, Map<String, ConValue> mergeCells, Set<String> excludedCells,
			LogicalTimestamp metadataNodeId, ConValue synapseRowConValue) {
		List<ConValue> values = new ArrayList<>();
		List<Integer> valueIndex = new ArrayList<>();
		for (Entry<String, ConValue> e : mergeCells.entrySet()) {
			if (excludedCells.contains(e.getKey())) {
				continue;
			}
			Column column = columnNameMap.get(e.getKey());
			if (column != null) {
				values.add(e.getValue());
				valueIndex.add(column.getVectorIndex());
			}
		}
		if (values.isEmpty() && !excludedCells.isEmpty()) {
			// Every writable cell was a preserved user cell; nothing to rewrite.
			return;
		}
		intendedChangePublisher.publish(new UpdateRowChange(vectorNodeId, values, valueIndex.toArray(Integer[]::new),
				metadataNodeId, synapseRowConValue));
	}

	/**
	 * Extract a copy row's cells into a column-name to value map.
	 */
	static Map<String, ConValue> cellsAsMap(RowCopyItem copyItem) {
		return copyItem.getCells().stream().collect(Collectors.toMap(CellCopyItem::getName, CellCopyItem::getValue,
				(v1, v2) -> v2, LinkedHashMap::new));
	}

}
