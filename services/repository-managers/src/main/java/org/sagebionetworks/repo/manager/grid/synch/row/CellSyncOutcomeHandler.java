package org.sagebionetworks.repo.manager.grid.synch.row;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.grid.synch.core.SyncOutcomeHandler;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

/**
 * Accumulates the merged cells for a single row conflict. The nested cell-level
 * {@code synchronize} run reports each cell's outcome here; the resulting
 * {@link #getMergedCells() merged cells} are written back to the grid and the
 * {@link #getUserChangedCells() user-changed cells} are pushed to the source by
 * the enclosing {@link RowSyncOutcomeHandler}.
 *
 * <p>
 * Cell-level conflict resolution is user-wins: when a cell exists in both sides
 * but differs, the user's value is kept if the user changed it, otherwise the
 * source value wins. Cells can always be added/removed at the cell level, so no
 * capability gating applies here.
 */
public class CellSyncOutcomeHandler implements SyncOutcomeHandler<CellCopyItem, CellSourceItem> {

	private final Map<String, ConValue> mergedCells;
	private final Map<String, ConValue> userChangedCells;
	private final Set<String> userWonCells;

	public CellSyncOutcomeHandler(List<CellCopyItem> copyCells) {
		this.mergedCells = copyCells.stream()
				.collect(Collectors.toMap(CellCopyItem::getName, CellCopyItem::getValue, (a, b) -> b, HashMap::new));
		this.userChangedCells = new HashMap<>();
		this.userWonCells = new HashSet<>();
	}

	@Override
	public void onCopyAndSourceMatch(CellCopyItem copyItem, CellSourceItem sourceItem) {
		// Copy and source agree; the merged cell already holds the copy value.
	}

	@Override
	public void onCopyAndSourceConflict(CellCopyItem copyItem, CellSourceItem sourceItem) {
		// User's change wins, otherwise take the source value.
		mergedCells.put(copyItem.getName(), copyItem.wasChangedByUser() ? copyItem.getValue() : sourceItem.getValue());
		if (copyItem.wasChangedByUser()) {
			userChangedCells.put(copyItem.getName(), copyItem.getValue());
			userWonCells.add(copyItem.getName());
		}
	}

	@Override
	public void onNewCopyItem(CellCopyItem copyItem, String key) {
		// Cell the user added is pushed to the source; it already sits in mergedCells.
		// Cells are never excluded from matching, so this only ever fires for a
		// genuine user addition.
		userChangedCells.put(key, copyItem.getValue());
	}

	@Override
	public void onDeletedFromSource(CellCopyItem copyItem) {
		// Cell was removed from the source - drop it from the merged result.
		mergedCells.remove(copyItem.getName());
	}

	@Override
	public void onDeletedFromCopy(CellSourceItem sourceItem) {
		// User cleared the cell - push a null to the source to clear it there too.
		userChangedCells.put(sourceItem.getColumnName(), null);
	}

	@Override
	public void onNewSourceItem(CellSourceItem sourceItem) {
		// Cell was added in the source - pull it into the merged result.
		mergedCells.put(sourceItem.getColumnName(), sourceItem.getValue());
	}

	public Map<String, ConValue> getMergedCells() {
		return mergedCells;
	}

	public Map<String, ConValue> getUserChangedCells() {
		return userChangedCells;
	}

	public Set<String> getUserWonCells() {
		return userWonCells;
	}

	/**
	 * The cells the user cleared (changed to null/undefined) in the copy. A source
	 * cell for one of these columns that is absent from the copy is treated as a
	 * user deletion rather than a source-side value to pull in.
	 */
	static Set<String> getUserDeletedCells(RowCopyItem copyItem) {
		return copyItem.getCells().stream()
				.filter(cell -> cell.wasChangedByUser() && cell.getValue() != null
						&& (ConType.UNDEFINED.equals(cell.getValue().getType())
								|| ConType.NULL.equals(cell.getValue().getType())))
				.map(CellCopyItem::getName).collect(Collectors.toSet());
	}

}
