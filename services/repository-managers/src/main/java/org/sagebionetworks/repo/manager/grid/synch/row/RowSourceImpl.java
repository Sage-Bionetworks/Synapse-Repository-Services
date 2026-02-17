package org.sagebionetworks.repo.manager.grid.synch.row;

import java.util.Arrays;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.sagebionetworks.repo.manager.grid.synch.core.SynchronizationLogic;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;
import org.sagebionetworks.repo.manager.grid.synch.io.RowHeader;
import org.sagebionetworks.repo.manager.grid.synch.io.RowReader;
import org.sagebionetworks.repo.manager.grid.synch.io.SynchRow;

/**
 * Implementation of {@link RowSource} that provides access to rows from the
 * source of truth and operations for modifying the source during Phase 2 row
 * synchronization.
 *
 * <p>
 * This class bridges the synchronization logic with the underlying source
 * system (EntityView, Table, RecordSet, etc.) by:
 * <ul>
 * <li>Streaming source rows via {@link RowReader} for memory-efficient
 * comparison</li>
 * <li>Matching rows using source system identifiers</li>
 * <li>Comparing rows using content hashes to detect changes</li>
 * <li>Applying user changes from copy to source via {@link SourceHandler}</li>
 * </ul>
 *
 * <p>
 * Key responsibilities:
 * <ul>
 * <li>Match copy rows with source rows by key during Phase 1 of
 * {@link SynchronizationLogic#synchronize}</li>
 * <li>Compare matched rows using hash-based equality to detect conflicts</li>
 * <li>Add rows to source when user adds them in the copy (pushing user
 * additions)</li>
 * <li>Remove rows from source when user deletes them in the copy (pushing user
 * deletions)</li>
 * </ul>
 *
 * <p>
 * The hash-based comparison in {@link #matches} enables efficient change
 * detection without comparing individual cells unless a conflict is detected
 * (which triggers cell-level merge via {@link RowMerge}).
 */
public class RowSourceImpl implements RowSource {

	private final SourceHandler sourceHandler;
	private final RowReader rowReader;

	/**
	 * Creates a new row source implementation for synchronization with an external
	 * source system.
	 *
	 * @param sourceHandler the handler for applying changes to the source
	 * @param rowReader     the reader for streaming rows from the source
	 */
	public RowSourceImpl(SourceHandler sourceHandler, RowReader rowReader) {
		super();
		this.sourceHandler = sourceHandler;
		this.rowReader = rowReader;
	}

	/**
	 * Gets the unique key that identifies a copy row in the source system. Uses
	 * {@link SourceHandler#getRowKey} to extract the source system's identifier
	 * from the copy row's metadata. This key is used to match rows between copy and
	 * source during synchronization.
	 *
	 * @param copyItem the row from the copy
	 * @return the source system's identifier for this row
	 */
	@Override
	public String getKey(RowCopyItem copyItem) {
		return sourceHandler.getRowKey(copyItem);
	}

	/**
	 * Consumes (removes and returns) a row from the source by key. Called during
	 * Phase 1 of {@link SynchronizationLogic#synchronize} for each copy row to find
	 * its matching source row. After all copy rows are processed, remaining
	 * unconsumed rows represent additions to the source.
	 *
	 * <p>
	 * Uses {@link RowReader#consumeRow} which efficiently looks up rows in the
	 * disk-based index without loading all source data into memory.
	 *
	 * @param key the source system's identifier for the row
	 * @return the matching source row, or empty if no row with that key exists
	 */
	@Override
	public Optional<RowHeader> consume(String key) {
		return rowReader.consumeRow(key);
	}

	/**
	 * Streams all remaining unconsumed rows from the source. Called during Phase 2
	 * of {@link SynchronizationLogic#synchronize} to process rows that exist in the
	 * source but not in the copy (potential additions or user deletions).
	 *
	 * <p>
	 * Uses {@link RowReader#remainingRows} to stream rows efficiently without
	 * loading all data into memory.
	 *
	 * @return a stream of source rows that weren't consumed during Phase 1
	 */
	@Override
	public Stream<RowHeader> streamRemaining() {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(rowReader.remainingRows(), Spliterator.ORDERED),
				false);
	}

	/**
	 * Adds a new row to the source. Called during Phase 1 of
	 * {@link SynchronizationLogic#synchronize} when a row exists only in the copy
	 * and was changed by the user, pushing the user's addition to the source.
	 *
	 * <p>
	 * Converts the copy row to a {@link SynchRow} format (map of column names to
	 * values) that the source system can understand, then delegates to
	 * {@link SourceHandler#addNewRowToSource}.
	 *
	 * @param copyItem the row from the copy to add to the source
	 */
	@Override
	public void addItem(RowCopyItem copyItem) {
		sourceHandler.addNewRowToSource(createSynchRow(copyItem, getKey(copyItem)));
	}

	/**
	 * Removes a row from the source. Called during Phase 2 of
	 * {@link SynchronizationLogic#synchronize} when a row exists in the source but
	 * not in the copy, and was deleted by the user in the copy, pushing the user's
	 * deletion to the source.
	 *
	 * <p>
	 * Fetches the full row data via {@link RowHeader#fetchRow()} and delegates to
	 * {@link SourceHandler#removeRow}.
	 *
	 * @param toRemove the header for the source row to remove
	 */
	@Override
	public void removeItem(RowHeader toRemove) {
		sourceHandler.removeRow(toRemove.fetchRow());
	}

	/**
	 * Determines whether a copy row and source row match (have identical content).
	 * Uses hash-based comparison for efficient change detection without comparing
	 * individual cells.
	 *
	 * <p>
	 * If the hashes don't match, {@link SynchronizationLogic#synchronize} will
	 * invoke {@link RowMerge} to perform cell-level comparison and conflict
	 * resolution. Hash equality means the rows are identical and no merge is
	 * needed.
	 *
	 * @param copyItem   the row from the copy
	 * @param sourceItem the row header from the source (contains precomputed hash)
	 * @return true if the rows have identical content, false if they differ
	 */
	@Override
	public boolean matches(RowCopyItem copyItem, RowHeader sourceItem) {
		SynchRow copySynch = createSynchRow(copyItem, sourceItem.getKey());
		return Arrays.equals(copySynch.getHash(), sourceItem.getHash());
	}

	/**
	 * Converts a copy row to a {@link SynchRow} format for source operations.
	 * Extracts cell values from the copy row and creates a map-based representation
	 * with a content hash for efficient comparison.
	 *
	 * @param copy the row from the copy
	 * @param key  the source system's identifier for the row
	 * @return a SynchRow representation suitable for source operations
	 */
	private SynchRow createSynchRow(RowCopyItem copy, String key) {
		return new SynchRow(
				copy.getCells().stream().collect(
						Collectors.toMap(CellCopyItem::getName, CellCopyItem::getValue, (v1, v2) -> v2, TreeMap::new)),
				key, copy.getSynapseRow().orElse(null));
	}

}
