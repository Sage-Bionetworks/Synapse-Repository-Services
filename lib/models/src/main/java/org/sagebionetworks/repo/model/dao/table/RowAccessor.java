package org.sagebionetworks.repo.model.dao.table;


public interface RowAccessor {

	public String getCellById(Long columnId);

	public Long getRowId();

	public Long getVersionNumber();
}