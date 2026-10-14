package org.sagebionetworks.repo.manager.grid.synch.handler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

/**
 * Handles writing changes to a source. Current implementations include:
 * <ul>
 * <li><b>In-place</b> (entity view): cell changes are written directly to the
 * source (annotations); row/column membership cannot be changed.</li>
 * <li><b>RecordSetSourceWriter</b>: the source is never mutated in place;
 * instead the surviving rows are accumulated ({@link #beginPush},
 * {@link #recordFinalRowState}, {@link #completePush}) into a new exported
 * artifact.</li>
 * </ul>
 */
public interface SourceWriter extends AutoCloseable {

	/**
	 * @return true if rows can be added to or removed from this source.
	 */
	default boolean canAddRemoveRows() {
		return true;
	}

	/**
	 * @return true if columns can be added to or removed from this source
	 */
	default boolean canAddRemoveColumns() {
		return true;
	}

	/**
	 * Pushes a user-added row to the source. Only called when
	 * {@link #canAddRemoveRows()} is true.
	 *
	 * @param row the row to add to the source
	 */
	void addNewRowToSource(RowSourceItem row);

	/**
	 * Pushes a user-deleted row to the source. Only called when
	 * {@link #canAddRemoveRows()} is true.
	 *
	 * @param row the row to remove from the source
	 */
	void removeRow(RowSourceItem row);

	/**
	 * Pushes a user-added column to the source. Only called when
	 * {@link #canAddRemoveColumns()} is true.
	 *
	 * @param columnName the column to add to the source
	 */
	void addColumnToSource(String columnName);

	/**
	 * Pushes a user-deleted column to the source. Only called when
	 * {@link #canAddRemoveColumns()} is true.
	 *
	 * @param columnName the column to remove from the source
	 */
	void removeColumn(String columnName);

	/**
	 * Applies only the cells that changed in the copy to the source row, preserving
	 * source cells changed externally. Called during a row conflict merge.
	 *
	 * @param rowId        the identifier of the row to update
	 * @param changedCells the cells that changed in the copy, keyed by column name
	 */
	void applyCellChangesFromCopyToSource(String rowId, Map<String, ConValue> changedCells);

	/**
	 * Prepares any push artifact this writer builds during the merge. Called once,
	 * after Phase 1 schema synchronization and before the row merge.
	 *
	 * @param callback    the async-job progress callback (used for the job id)
	 * @param finalSchema the synchronized schema produced by Phase 1
	 * @throws IOException if the push artifact cannot be opened
	 */
	default void beginPush(AsyncJobProgressCallback callback, List<Column> finalSchema)
			throws IOException {
		// no-op by default
	}

	/**
	 * Notification that a single grid row has completed the merge with the given final
	 * cell values. A writer building a pushed artifact captures the row here.
	 *
	 * @param finalCells the row's final cell values, keyed by column name
	 */
	default void recordFinalRowState(Map<String, ConValue> finalCells) {
		// no-op by default
	}

	/**
	 * Flushes any push artifact accumulated during the merge and returns the new
	 * source version number.
	 *
	 * @return the new source version number, or empty if no push was performed
	 * @throws Exception if the push fails
	 */
	default Optional<Long> completePush() throws Exception {
		return Optional.empty();
	}

	/**
	 * @return the error messages generated while writing to the source, forwarded to
	 *         the caller of the synchronization.
	 */
	List<String> getErrorMessages();

	@Override
	default void close() throws Exception {
		// no-op by default
	}

}
