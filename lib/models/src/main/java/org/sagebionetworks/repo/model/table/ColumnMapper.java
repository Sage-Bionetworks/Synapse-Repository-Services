package org.sagebionetworks.repo.model.table;

import java.util.List;

public interface ColumnMapper extends ColumnModelMapper {

	public List<SelectColumnAndModel> getSelectColumnAndModels();
}
