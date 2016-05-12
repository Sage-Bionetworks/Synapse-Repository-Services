package org.sagebionetworks.repo.model.table;

import java.util.List;

public interface ColumnModelMapper {

	public List<ColumnModel> getColumnModels();

	public int columnModelCount();
}
