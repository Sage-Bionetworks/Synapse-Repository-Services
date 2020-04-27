package org.sagebionetworks.table.cluster;

import java.util.Objects;

import org.sagebionetworks.repo.model.table.ColumnModel;

/**
 * Metadata used to create SQL to copy data from object replication/annotation
 * tables to a table's index.
 *
 */
public class ColumnMetadata {

	private ColumnModel columnModel;
	private String columnNameForId;
	private String selectColumnName;
	private boolean isObjectReplicationField;

	public ColumnMetadata(ColumnModel columnModel, String selectColumnName, String columnNameForId, boolean isObjectReplicationField) {
		this.columnModel = columnModel;
		this.selectColumnName = selectColumnName;
		this.columnNameForId = columnNameForId;
		this.isObjectReplicationField = isObjectReplicationField;
	}

	public ColumnModel getColumnModel() {
		return columnModel;
	}

	public void setColumnModel(ColumnModel columnModel) {
		this.columnModel = columnModel;
	}
	
	public String getSelectColumnName() {
		return selectColumnName;
	}
	
	public void setSelectColumnName(String selectColumnName) {
		this.selectColumnName = selectColumnName;
	}
	
	public String getColumnNameForId() {
		return columnNameForId;
	}

	public void setColumnNameForId(String columnNameForId) {
		this.columnNameForId = columnNameForId;
	}
	
	public boolean isObjectReplicationField() {
		return isObjectReplicationField;
	}
	
	public void setObjectReplicationField(boolean isObjectReplicationField) {
		this.isObjectReplicationField = isObjectReplicationField;
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnModel, columnNameForId, isObjectReplicationField, selectColumnName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ColumnMetadata other = (ColumnMetadata) obj;
		return Objects.equals(columnModel, other.columnModel) && Objects.equals(columnNameForId, other.columnNameForId)
				&& isObjectReplicationField == other.isObjectReplicationField
				&& Objects.equals(selectColumnName, other.selectColumnName);
	}

		
}
