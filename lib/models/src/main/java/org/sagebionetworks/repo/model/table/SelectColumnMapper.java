package org.sagebionetworks.repo.model.table;

import java.util.List;

public interface SelectColumnMapper {

	public SelectColumn getSelectColumnById(String columnId);

	public SelectColumn getSelectColumnByName(String name);

	public List<SelectColumn> getSelectColumns();

	public int selectColumnCount();
}
