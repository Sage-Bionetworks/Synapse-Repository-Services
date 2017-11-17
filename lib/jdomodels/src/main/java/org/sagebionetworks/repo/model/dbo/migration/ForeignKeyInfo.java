package org.sagebionetworks.repo.model.dbo.migration;

/**
 * Basic information about a foreign key relationship.
 *
 */
public class ForeignKeyInfo {
	
	String constraintName;
	String deleteRule;
	String tableName;
	String referencedTableName;
	
	public String getConstraintName() {
		return constraintName;
	}
	public void setConstraintName(String constraintName) {
		this.constraintName = constraintName;
	}
	public String getDeleteRule() {
		return deleteRule;
	}
	public void setDeleteRule(String deleteRule) {
		this.deleteRule = deleteRule;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getReferencedTableName() {
		return referencedTableName;
	}
	public void setReferencedTableName(String referencedTableName) {
		this.referencedTableName = referencedTableName;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((constraintName == null) ? 0 : constraintName.hashCode());
		result = prime * result + ((deleteRule == null) ? 0 : deleteRule.hashCode());
		result = prime * result + ((referencedTableName == null) ? 0 : referencedTableName.hashCode());
		result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
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
		ForeignKeyInfo other = (ForeignKeyInfo) obj;
		if (constraintName == null) {
			if (other.constraintName != null)
				return false;
		} else if (!constraintName.equals(other.constraintName))
			return false;
		if (deleteRule == null) {
			if (other.deleteRule != null)
				return false;
		} else if (!deleteRule.equals(other.deleteRule))
			return false;
		if (referencedTableName == null) {
			if (other.referencedTableName != null)
				return false;
		} else if (!referencedTableName.equals(other.referencedTableName))
			return false;
		if (tableName == null) {
			if (other.tableName != null)
				return false;
		} else if (!tableName.equals(other.tableName))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "ForeignKeyInfo [constraintName=" + constraintName + ", deleteRule=" + deleteRule + ", tableName="
				+ tableName + ", referencedTableName=" + referencedTableName + "]";
	}
	
	
}
