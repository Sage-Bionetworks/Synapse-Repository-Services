package org.sagebionetworks.repo.manager.grid.synch.row;

import java.util.Arrays;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.grid.synch.core.SyncRules;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReference;

/**
 * Keying and matching rules for row synchronization. Rows are keyed by
 * the source system's identifier ({@link SourceHandler#getRowKey}) and matched by
 * content hash, so a mismatch triggers cell-level conflict resolution. Row
 * matchability and user-deletion detection are delegated to the
 * {@link SourceHandler}.
 */
public class RowSyncRules implements SyncRules<RowCopyItem, RowSourceItemReference> {

	private final SourceHandler sourceHandler;

	public RowSyncRules(SourceHandler sourceHandler) {
		this.sourceHandler = sourceHandler;
	}

	@Override
	public String getKey(RowCopyItem copyItem) {
		return sourceHandler.getRowKey(copyItem);
	}

	@Override
	public boolean matches(RowCopyItem copyItem, RowSourceItemReference sourceItem) {
		RowSourceItem copySynch = createSynchRow(copyItem, sourceItem.getKey());
		return Arrays.equals(copySynch.getHash(), sourceItem.getHash());
	}

	@Override
	public boolean isExcludedFromMatching(RowCopyItem copyItem, String key) {
		return sourceHandler.isUnmatchableCopyRow(copyItem, key);
	}

	@Override
	public boolean wasDeletedByUser(RowSourceItemReference sourceItem) {
		String key = sourceItem.getKey();
		// This step is the final check to decide if a copy can delete a row.
		// We intentionally exclude rows that have changed since the last synced baseline
		// to avoid silently dropping changes to data that the user that started the sync
		// may not know about.
		return sourceHandler.wasInSyncedBaseline(key) && !sourceHandler.changedSinceBaseline(key);
	}

	/**
	 * Converts a copy row to a {@link RowSourceItem} (a hashable, map-based
	 * representation) for hash comparison and for pushing user additions to the
	 * source.
	 */
	static RowSourceItem createSynchRow(RowCopyItem copy, String key) {
		return new RowSourceItem(
				copy.getCells().stream().collect(
						Collectors.toMap(CellCopyItem::getName, CellCopyItem::getValue, (v1, v2) -> v2, TreeMap::new)),
				key, copy.getSynapseRow().orElse(null));
	}

}
