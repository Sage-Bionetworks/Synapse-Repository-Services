package org.sagebionetworks.repo.model.query;

/**
 * A compound id can include a table name and a field name.
 * 
 * @author jmhill
 *
 */
public class CompoundId {
	String tableName;
	String fieldName;
	public CompoundId(String tableName, String fieldName) {
		super();
		this.tableName = tableName;
		this.fieldName = fieldName;
	}
	public String getTableName() {
		return tableName;
	}
	public String getFieldName() {
		return fieldName;
	}
	@Override
	public String toString() {
		return "CompoundId [tableName=" + tableName + ", fieldName="
				+ fieldName + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fieldName == null) ? 0 : fieldName.hashCode());
		result = prime * result
				+ ((tableName == null) ? 0 : tableName.hashCode());
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
		CompoundId other = (CompoundId) obj;
		if (fieldName == null) {
			if (other.fieldName != null)
				return false;
		} else if (!fieldName.equals(other.fieldName))
			return false;
		if (tableName == null) {
			if (other.tableName != null)
				return false;
		} else if (!tableName.equals(other.tableName))
			return false;
		return true;
	}
	
}