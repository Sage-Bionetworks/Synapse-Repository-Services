package org.sagebionetworks.repo.model.dao.table;

import java.util.Map;
import java.util.Set;

import org.sagebionetworks.util.ValidateArgument;

public class RowSetAccessor {
	
	Map<Long, RowAccessor> rowIdToRowMap;

	public RowSetAccessor(Map<Long, RowAccessor> rowIdToRowMap) {
		ValidateArgument.required(rowIdToRowMap, "rowIdToRowMap");
		this.rowIdToRowMap = rowIdToRowMap;
	}

	public RowAccessor getRow(Long rowId) {
		return getRowIdToRowMap().get(rowId);
	}

	public Iterable<RowAccessor> getRows() {
		return getRowIdToRowMap().values();
	}

	public Set<Long> getRowIds() {
		return getRowIdToRowMap().keySet();
	}

	public Map<Long, RowAccessor> getRowIdToRowMap(){
		return rowIdToRowMap;
	}
}
