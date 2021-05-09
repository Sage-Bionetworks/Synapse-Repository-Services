package org.sagebionetworks.repo.model.dbo;

import java.util.Objects;

/**
 * Binds a java field name to a database column.
 * 
 * This object is immutable.
 * 
 * https://sagebionetworks.jira.com/wiki/pages/viewpage.action?pageId=21168197
 * 
 * @author jmhill
 *
 */
public class FieldColumn {
	
	private String fieldName;
	private String columnName;
	private boolean isPrimaryKey = false;
	private boolean isEtag = false;
	private boolean isBackupId = false;
	private boolean isSelfForeignKey = false;
	private boolean hasFileHandleRef = false;
	
	/**
	 * @param fieldName - the name of the field as declared in the DBO class.
	 * @param columnName - the name of the column in the database
	 */
	public FieldColumn(String fieldName, String columnName) {
		this(fieldName, columnName, false);
	}
	
	/**
	 * @param fieldName - the name of the field as declared in the DBO class.
	 * @param columnName - the name of the column in the database
	 * @param isPrimary - Set to true if this column is part of the primary key.
	 */
	public FieldColumn(String fieldName, String columnName, boolean isPrimary) {
		super();
		this.fieldName = fieldName;
		this.columnName = columnName;
		this.isPrimaryKey = isPrimary;
	}

	/**
	 * The name of the Java field.
	 * @return
	 */
	public String getFieldName() {
		return fieldName;
	}
	
	/**
	 * The name of the database column.
	 * @return
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * Boolean used to mark a column as part of the primary key.
	 * Note: More than one column can be part of the primary key
	 * @return
	 */
	public boolean isPrimaryKey() {
		return isPrimaryKey;
	}
	
	/**
	 * Is this column the etag?
	 * This is only used for MigratableDatabaseObject.
	 * @return
	 */
	public boolean isEtag(){
		return isEtag;
	}
	
	/**
	 * Is this column the backup ID.
	 * Note: The backup ID is the same as the primary key as only as the primary key only includes one column.
	 * For tables that have primary keys that span multiple columns a separate single backup column ID must be provided.
	 * For such cases the backup ID should be a single delimited string of the primary key columns. The backup ID must be unique.
	 * This is only used for MigratableDatabaseObject.
	 * 
	 * @return
	 */
	public boolean isBackupId(){
		return isBackupId;
	}
	
	/**
	 * Is this column the etag?
	 * This is only used for MigratableDatabaseObject.
	 * @param isEtag
	 * @return
	 */
	public FieldColumn withIsEtag(boolean isEtag){
		this.isEtag = isEtag;
		return this;
	}
	
	/**
	 * Is this column the backup ID.
	 * Note: The backup ID is the same as the primary key as only as the primary key only includes one column.
	 * For primary tables that have primary keys that span multiple columns a separate single backup column ID must be provided.
	 * For such cases the backup ID should be a single delimited string of the primary key columns. The backup ID must be unique.
	 * This is only used for MigratableDatabaseObject.
	 * @param isBackupId
	 * @return
	 */
	public FieldColumn withIsBackupId(boolean isBackupId){
		this.isBackupId = isBackupId;
		return this;
	}
	
	/**
	 * Does this column have a Foreign Key constraint to another column on this table?
	 * For example, 'parentId' can be a reference to another row within the same table.
	 * In this example, the 'parentId' column should be set to isSelfForeignKey = true;
	 * @param isSelfForeignKey
	 * @return
	 */
	public FieldColumn withIsSelfForeignKey(boolean isSelfForeignKey){
		this.isSelfForeignKey = isSelfForeignKey;
		return this;
	}
	
	/**
	 * Is this column part of the primary key?
	 * @param isPrimaryKey
	 * @return
	 */
	public FieldColumn withIsPrimaryKey(boolean isPrimaryKey) {
		this.isPrimaryKey = isPrimaryKey;
		return this;
	}
	
	/**
	 * Does this column have a Foreign Key constraint to another column on this table?
	 * For example, 'parentId' can be a reference to another row within the same table.
	 * In this example, the 'parentId' column should be set to isSelfForeignKey = true;
	 * @return
	 */
	public boolean isSelfForeignKey(){
		return this.isSelfForeignKey;
	}
	
	/**
	 * @return True if this column has reference to file handle id(s) (e.g. in the form of a FK, or as part of a serialzied field), false otherwise
	 */
	public boolean hasFileHandleRef() {
		return hasFileHandleRef;
	}
	
	/**
	 * Does this column point to a file handle id or is it serialized and contains references to file handles? If so set this to true
	 * 
	 * @param hasFileHandleRef True if this column has a reference to file handle id(s), false otherwise
	 * @return This {@link FieldColumn} reference for chaining 
	 */
	public FieldColumn withHasFileHandleRef(boolean hasFileHandleRef) {
		this.hasFileHandleRef = hasFileHandleRef;
		return this;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(columnName, fieldName, isBackupId, isEtag, hasFileHandleRef, isPrimaryKey, isSelfForeignKey);
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
		FieldColumn other = (FieldColumn) obj;
		return Objects.equals(columnName, other.columnName) && Objects.equals(fieldName, other.fieldName) && isBackupId == other.isBackupId
				&& isEtag == other.isEtag && hasFileHandleRef == other.hasFileHandleRef && isPrimaryKey == other.isPrimaryKey
				&& isSelfForeignKey == other.isSelfForeignKey;
	}

	@Override
	public String toString() {
		return "FieldColumn [fieldName=" + fieldName + ", columnName=" + columnName + ", isPrimaryKey=" + isPrimaryKey + ", isEtag="
				+ isEtag + ", isBackupId=" + isBackupId + ", isSelfForeignKey=" + isSelfForeignKey + ", isFileHandleRef=" + hasFileHandleRef
				+ "]";
	}

}
