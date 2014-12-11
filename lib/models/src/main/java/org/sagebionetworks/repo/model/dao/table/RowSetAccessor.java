package org.sagebionetworks.repo.model.dao.table;

import java.util.Map;
import java.util.Set;

public abstract class RowSetAccessor {

	public RowAccessor getRow(Long rowId) {
		return getRowIdToRowMap().get(rowId);
	}

	public Iterable<RowAccessor> getRows() {
		return getRowIdToRowMap().values();
	}

	public Set<Long> getRowIds() {
		return getRowIdToRowMap().keySet();
	}

	public abstract Map<Long, RowAccessor> getRowIdToRowMap();
}
