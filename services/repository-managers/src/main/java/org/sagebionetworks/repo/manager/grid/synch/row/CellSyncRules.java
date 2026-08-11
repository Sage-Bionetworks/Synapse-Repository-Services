package org.sagebionetworks.repo.manager.grid.synch.row;

import java.util.Objects;
import java.util.Set;

import org.sagebionetworks.repo.manager.grid.synch.core.SyncRules;

/**
 * Keying and matching rules for the nested cell-level synchronization used to
 * resolve a row conflict. Cells are keyed and matched by column name/value. A
 * source cell absent from the copy counts as a user deletion when the user
 * cleared that column in the copy (supplied as {@code userDeletedCells}).
 */
public class CellSyncRules implements SyncRules<CellCopyItem, CellSourceItem> {

	private final Set<String> userDeletedCells;

	public CellSyncRules(Set<String> userDeletedCells) {
		this.userDeletedCells = userDeletedCells;
	}

	@Override
	public String getKey(CellCopyItem item) {
		return item.getName();
	}

	@Override
	public boolean matches(CellCopyItem copyItem, CellSourceItem sourceItem) {
		return Objects.equals(copyItem.getValue(), sourceItem.getValue());
	}

	@Override
	public boolean wasDeletedByUser(CellSourceItem sourceItem) {
		return userDeletedCells.contains(sourceItem.getKey());
	}

}
