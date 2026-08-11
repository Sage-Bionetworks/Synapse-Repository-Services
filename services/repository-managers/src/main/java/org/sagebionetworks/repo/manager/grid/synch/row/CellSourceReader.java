package org.sagebionetworks.repo.manager.grid.synch.row;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import org.sagebionetworks.repo.manager.grid.synch.core.SourceReader;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

/**
 * Read-only view of a single source row's cells, used for the nested cell-level
 * synchronization that resolves a row conflict. Cells are consumed by column name
 * as they are matched to copy cells.
 */
public class CellSourceReader implements SourceReader<CellSourceItem> {

	private final Map<String, CellSourceItem> sourceMap;

	public CellSourceReader(RowSourceItem sourceItem) {
		this.sourceMap = new HashMap<>();
		for (Entry<String, ConValue> e : sourceItem.getData().entrySet()) {
			sourceMap.put(e.getKey(), new CellSourceItem().setColumnName(e.getKey()).setValue(e.getValue()));
		}
	}

	@Override
	public Optional<CellSourceItem> consume(String key) {
		return Optional.ofNullable(sourceMap.remove(key));
	}

	@Override
	public Stream<CellSourceItem> streamRemaining() {
		return sourceMap.values().stream();
	}

}
