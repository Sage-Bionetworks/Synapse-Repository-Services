package org.sagebionetworks.repo.manager.grid.synch.row;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sagebionetworks.repo.manager.grid.synch.core.Copy;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class CellCopyImpl implements Copy<CellCopyItem, CellSourceItem> {

	private final RowCopyItem copyItem;
	private final Map<String, ConValue> mergeCells;
	private final Set<String> userDeletedCells;

	public CellCopyImpl(RowCopyItem copyItem) {
		this.copyItem = copyItem;
		mergeCells = copyItem.getCells().stream()
				.collect(Collectors.toMap(CellCopyItem::getName, CellCopyItem::getValue));
		this.userDeletedCells = getUserDeletedCells(copyItem);
	}

	@Override
	public Stream<CellCopyItem> streamItems() {
		return copyItem.getCells().stream();
	}

	@Override
	public boolean wasDeletedByUser(String key) {
		return userDeletedCells.contains(key);
	}

	@Override
	public void removeItem(CellCopyItem item) {
		// Cell was removed from source - remove from merged result
		mergeCells.remove(item.getName());

	}

	@Override
	public void addItem(CellSourceItem item) {
		// Cell was added to source - add to merged result
		mergeCells.put(item.getColumnName(), item.getValue());
	}

	static Set<String> getUserDeletedCells(RowCopyItem copyItem) {
		return copyItem.getCells().stream()
				.filter(cell -> cell.wasChangedByUser() && cell.getValue() != null
						&& (ConType.UNDEFINED.equals(cell.getValue().getType())
								|| ConType.NULL.equals(cell.getValue().getType())))
				.map(CellCopyItem::getName).collect(Collectors.toSet());
	}
	
	public Map<String, ConValue> getMergedCells(){
		return mergeCells;
	}

}
