package org.sagebionetworks.repo.model.table;

import java.util.List;

public interface SelectColumnMapper {

	public SelectColumn getSelectColumnById(Long id);

	public SelectColumn getSelectColumnByName(String name);

	public List<SelectColumn> getSelectColumns();

	public int selectColumnCount();
}
