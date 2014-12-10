package org.sagebionetworks.repo.model.table;

import java.util.List;

public interface ColumnMapper extends ColumnModelMapper {
	public SelectColumnAndModel getSelectColumnAndModelById(String columnId);

	public List<SelectColumnAndModel> getSelectColumnAndModels();
}
