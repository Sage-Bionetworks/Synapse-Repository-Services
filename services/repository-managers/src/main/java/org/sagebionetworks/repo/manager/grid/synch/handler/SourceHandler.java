package org.sagebionetworks.repo.manager.grid.synch.handler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReader;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItem;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

/**
 * Handler for reading from and writing to the source of truth during grid
 * synchronization. Provides both read access to the current source state and
 * write operations for applying changes from the copy (CRDT replica).
 *
 * <p>
 * This handler bridges the synchronization logic with the actual source
 * implementation, allowing the source to be any external source (EntityView,
 * Table, RecordSet, etc.) while maintaining a consistent interface for
 * synchronization.
 */
public interface SourceHandler extends AutoCloseable {

	/**
	 * Gets a disk-based reader for streaming all rows from the source. Used during
	 * Phase 2 (row synchronization) to compare source rows with copy rows without
	 * loading all data into memory (O(n) memory usage).
	 *
	 * @return a reader that streams rows from the source
	 * @throws IOException if reading from the source fails
	 */
	RowSourceItemReader getSourceRowReader() throws IOException;

	/**
	 * Gets the unique key used to identify a row in the source system. This key is
	 * used to match rows between copy and source during synchronization.
	 *
	 * @param rowView the row from the copy
	 * @return the source system's identifier for this row
	 */
	String getRowKey(RowCopyItem rowView);

	/**
	 * Adds a new row to the source. Called during synchronization when a row exists
	 * in the copy but not in the source, and the row was changed by the user
	 * (pushing the user's addition to the source).
	 *
	 * @param copy the row to add to the source
	 */
	void addNewRowToSource(RowSourceItem copy);

	/**
	 * Gets the current schema (column names) from the source. Used during Phase 1
	 * (schema synchronization) to compare source columns with copy columns.
	 *
	 * @return ordered list of column names defining the source schema
	 */
	List<String> getCurrentSourceSchema();

	/**
	 * Adds a new column to the source schema. Called during Phase 1 when a column
	 * exists in the copy but not in the source, and the column was added by the
	 * user (pushing the user's schema change to the source).
	 *
	 * @param name the name of the column to add
	 */
	void addColumnToSource(String name);

	/**
	 * Applies only the cells that changed in the copy to the source row. This
	 * prevents data loss when external changes occur in the source between reading
	 * and writing. By only updating cells that actually changed in the copy, any
	 * source cells that were modified after we read the row will be preserved.
	 *
	 * <p>
	 * Called during Phase 2 when merging cell-level conflicts between copy and
	 * source rows.
	 *
	 * @param rowId        the identifier of the row to update
	 * @param changedCells map of column names to new values (only cells that
	 *                     changed in the copy)
	 */
	void applyCellChangesFromCopyToSource(String rowId, Map<String, ConValue> changedCells);

	/**
	 * Deletes a column from the source schema. Called during Phase 1 when a column
	 * exists in the source but not in the copy, and the column was deleted by the
	 * user (pushing the user's schema change to the source).
	 *
	 * @param columnName the name of the column to delete
	 */
	void removeColumn(String columnName);

	/**
	 * Removes a row from the source. Called during synchronization when a row
	 * exists in the source but not in the copy, and the row was deleted by the user
	 * (pushing the user's deletion to the source).
	 *
	 * @param fetchRow the row to remove from the source
	 */
	void removeRow(RowSourceItem fetchRow);

	/**
	 * Provide all error messages generated during the synchronization process to be
	 * forwarded to the caller.
	 * 
	 * @return
	 */
	List<String> getErrorMessages();

}
