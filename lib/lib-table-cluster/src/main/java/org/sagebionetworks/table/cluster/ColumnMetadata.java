package org.sagebionetworks.table.cluster;

import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityField;

/**
 * Metadata used to create SQL to copy data from entity replication tables
 * to a table's index.
 *
 */
public class ColumnMetadata {

	ColumnModel columnModel;
	EntityField entityField;
	String tableAlias;
	String selectColumnName;
	String columnNameForId;
	int columnIndex;
	AnnotationType annotationType;
	
	public ColumnMetadata(ColumnModel columnModel, EntityField entityField,
			String tableAlias, String selectColumnName, String columnNameForId,
			int columnIndex, AnnotationType annotationType) {
		super();
		this.columnModel = columnModel;
		this.entityField = entityField;
		this.tableAlias = tableAlias;
		this.selectColumnName = selectColumnName;
		this.columnNameForId = columnNameForId;
		this.columnIndex = columnIndex;
		this.annotationType = annotationType;
	}

	public ColumnModel getColumnModel() {
		return columnModel;
	}

	public void setColumnModel(ColumnModel columnModel) {
		this.columnModel = columnModel;
	}

	public EntityField getEntityField() {
		return entityField;
	}

	public void setEntityField(EntityField entityField) {
		this.entityField = entityField;
	}

	public String getTableAlias() {
		return tableAlias;
	}

	public void setTableAlias(String tableAlias) {
		this.tableAlias = tableAlias;
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

	public int getColumnIndex() {
		return columnIndex;
	}

	public void setColumnIndex(int columnIndex) {
		this.columnIndex = columnIndex;
	}

	public AnnotationType getAnnotationType() {
		return annotationType;
	}

	public void setAnnotationType(AnnotationType annotationType) {
		this.annotationType = annotationType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((annotationType == null) ? 0 : annotationType.hashCode());
		result = prime * result + columnIndex;
		result = prime * result
				+ ((columnModel == null) ? 0 : columnModel.hashCode());
		result = prime * result
				+ ((columnNameForId == null) ? 0 : columnNameForId.hashCode());
		result = prime * result
				+ ((entityField == null) ? 0 : entityField.hashCode());
		result = prime
				* result
				+ ((selectColumnName == null) ? 0 : selectColumnName.hashCode());
		result = prime * result
				+ ((tableAlias == null) ? 0 : tableAlias.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColumnMetadata other = (ColumnMetadata) obj;
		if (annotationType != other.annotationType)
			return false;
		if (columnIndex != other.columnIndex)
			return false;
		if (columnModel == null) {
			if (other.columnModel != null)
				return false;
		} else if (!columnModel.equals(other.columnModel))
			return false;
		if (columnNameForId == null) {
			if (other.columnNameForId != null)
				return false;
		} else if (!columnNameForId.equals(other.columnNameForId))
			return false;
		if (entityField != other.entityField)
			return false;
		if (selectColumnName == null) {
			if (other.selectColumnName != null)
				return false;
		} else if (!selectColumnName.equals(other.selectColumnName))
			return false;
		if (tableAlias == null) {
			if (other.tableAlias != null)
				return false;
		} else if (!tableAlias.equals(other.tableAlias))
			return false;
		return true;
	}
	
}
