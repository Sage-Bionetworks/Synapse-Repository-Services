package org.sagebionetworks.repo.model.dao.table;


public interface RowAccessor {

	public String getCellById(String columnId);

	public Long getRowId();

	public Long getVersionNumber();
}