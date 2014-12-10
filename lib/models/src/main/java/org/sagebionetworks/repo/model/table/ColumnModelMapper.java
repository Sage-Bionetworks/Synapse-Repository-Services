package org.sagebionetworks.repo.model.table;

import java.util.List;

public interface ColumnModelMapper extends SelectColumnMapper {

	public ColumnModel getColumnModelById(String columnId);

	public List<ColumnModel> getColumnModels();

	public int columnModelCount();
}
