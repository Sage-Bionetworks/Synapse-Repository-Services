package org.sagebionetworks.table.cluster.search;

import java.util.Objects;

import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * DTO that represents a value with a {@link ColumnType}
 */
public class TypedCellValue {
	
	private ColumnType columnType;
	private String rawValue;

	public TypedCellValue(ColumnType columnType, String value) {
		this.columnType = columnType;
		this.rawValue = value;
	}
	
	public ColumnType getColumnType() {
		return columnType;
	}
	
	public String getRawValue() {
		return rawValue;
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnType, rawValue);
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
		TypedCellValue other = (TypedCellValue) obj;
		return columnType == other.columnType && Objects.equals(rawValue, other.rawValue);
	}

	

}
