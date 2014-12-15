package org.sagebionetworks.repo.model.table;


public interface SelectColumnAndModel {
	public String getName();

	public ColumnType getColumnType();

	public SelectColumn getSelectColumn();

	public ColumnModel getColumnModel();
}
