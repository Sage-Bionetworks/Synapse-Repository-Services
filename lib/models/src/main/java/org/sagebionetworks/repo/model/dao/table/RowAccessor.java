package org.sagebionetworks.repo.model.dao.table;

import org.sagebionetworks.repo.model.table.Row;

public interface RowAccessor {

	public String getCell(String columnId);

	public Row getRow();
}