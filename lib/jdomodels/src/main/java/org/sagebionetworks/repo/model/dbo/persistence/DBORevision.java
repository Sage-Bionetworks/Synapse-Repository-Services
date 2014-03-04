package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ACTIVITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ANNOS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_COLUMN_MODEL_IDS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_COMMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_LABEL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_REFS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_REVISION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REVISION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * The DatabaseObject for Revision.
 * 
 * @author jmhill
 *
 */
public class DBORevision implements MigratableDatabaseObject<DBORevision, DBORevision> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		// This is a sub-table of node, so it gets backed up with nodes using the node ids
		// so its backup ID is the owner.
		new FieldColumn("owner", COL_REVISION_OWNER_NODE, true).withIsBackupId(true),
		new FieldColumn("revisionNumber", COL_REVISION_NUMBER, true),
		new FieldColumn("activityId", COL_REVISION_ACTIVITY_ID),
		new FieldColumn("label", COL_REVISION_LABEL),
		new FieldColumn("comment", COL_REVISION_COMMENT),
		new FieldColumn("modifiedBy", COL_REVISION_MODIFIED_BY),
		new FieldColumn("modifiedOn", COL_REVISION_MODIFIED_ON),
		new FieldColumn("fileHandleId", COL_REVISION_FILE_HANDLE_ID),
		new FieldColumn("columnModelIds", COL_REVISION_COLUMN_MODEL_IDS),
		new FieldColumn("annotations", COL_REVISION_ANNOS_BLOB),
		new FieldColumn("references", COL_REVISION_REFS_BLOB),
		};

	@Override
	public TableMapping<DBORevision> getTableMapping() {
		return new TableMapping<DBORevision>(){
			@Override
			public DBORevision mapRow(ResultSet rs, int rowNum)	throws SQLException {
				DBORevision rev = new DBORevision();
				rev.setOwner(rs.getLong(COL_REVISION_OWNER_NODE));
				rev.setRevisionNumber(rs.getLong(COL_REVISION_NUMBER));						
				rev.setActivityId(rs.getLong(COL_REVISION_ACTIVITY_ID)); 
				if(rs.wasNull()) rev.setActivityId(null); // getLong returns 0 instead of null
				rev.setLabel(rs.getString(COL_REVISION_LABEL));
				rev.setComment(rs.getString(COL_REVISION_COMMENT));
				rev.setModifiedBy(rs.getLong(COL_REVISION_MODIFIED_BY));
				rev.setModifiedOn(rs.getLong(COL_REVISION_MODIFIED_ON));
				rev.setFileHandleId(rs.getLong(COL_REVISION_FILE_HANDLE_ID));
				if(rs.wasNull()){
					rev.setFileHandleId(null);
				}
				java.sql.Blob blob = rs.getBlob(COL_REVISION_ANNOS_BLOB);
				if(blob != null){
					rev.setAnnotations(blob.getBytes(1, (int) blob.length()));
				}
				blob = rs.getBlob(COL_REVISION_REFS_BLOB);
				if(blob != null){
					rev.setReferences(blob.getBytes(1, (int) blob.length()));
				}
				blob = rs.getBlob(COL_REVISION_COLUMN_MODEL_IDS);
				if(blob != null){
					rev.setColumnModelIds(blob.getBytes(1, (int) blob.length()));
				}
				return rev;
			}

			@Override
			public String getTableName() {
				return TABLE_REVISION;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_REVISION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBORevision> getDBOClass() {
				return DBORevision.class;
			}};
	}
	
	private Long owner;
	private Long revisionNumber;
	private Long activityId;
	private String label;
	private String comment;
	private Long modifiedBy;
	private Long modifiedOn;
	private Long fileHandleId;
	private byte[] columnModelIds;
	private byte[] annotations;
	private byte[] references;

	public Long getOwner() {
		return owner;
	}
	public void setOwner(Long owner) {
		this.owner = owner;
	}
	public Long getRevisionNumber() {
		return revisionNumber;
	}
	public void setRevisionNumber(Long revisionNumber) {
		this.revisionNumber = revisionNumber;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public Long getModifiedBy() {
		return modifiedBy;
	}
	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}
	public Long getModifiedOn() {
		return modifiedOn;
	}
	public void setModifiedOn(Long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}
	public byte[] getAnnotations() {
		return annotations;
	}
	public void setAnnotations(byte[] annotations) {
		this.annotations = annotations;
	}
	public byte[] getReferences() {
		return references;
	}
	public void setReferences(byte[] references) {
		this.references = references;
	}	
	public Long getActivityId() {
		return activityId;
	}
	public void setActivityId(Long activityId) {
		this.activityId = activityId;
	}
	
	public byte[] getColumnModelIds() {
		return columnModelIds;
	}
	public void setColumnModelIds(byte[] columnModelIds) {
		this.columnModelIds = columnModelIds;
	}
	public Long getFileHandleId() {
		return fileHandleId;
	}
	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}
	
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.NODE_REVISION;
	}
	@Override
	public MigratableTableTranslation<DBORevision, DBORevision> getTranslator() {
		// currently we do not have a backup object for node
		return new MigratableTableTranslation<DBORevision, DBORevision>(){

			@Override
			public DBORevision createDatabaseObjectFromBackup(DBORevision backup) {
				return backup;
			}

			@Override
			public DBORevision createBackupFromDatabaseObject(DBORevision dbo) {
				return dbo;
			}};
	}
	@Override
	public Class<? extends DBORevision> getBackupClass() {
		return DBORevision.class;
	}
	@Override
	public Class<? extends DBORevision> getDatabaseObjectClass() {
		return DBORevision.class;
	}
	
	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((activityId == null) ? 0 : activityId.hashCode());
		result = prime * result + Arrays.hashCode(annotations);
		result = prime * result + ((comment == null) ? 0 : comment.hashCode());
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result
				+ ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + Arrays.hashCode(references);
		result = prime * result
				+ ((revisionNumber == null) ? 0 : revisionNumber.hashCode());
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
		DBORevision other = (DBORevision) obj;
		if (activityId == null) {
			if (other.activityId != null)
				return false;
		} else if (!activityId.equals(other.activityId))
			return false;
		if (!Arrays.equals(annotations, other.annotations))
			return false;
		if (comment == null) {
			if (other.comment != null)
				return false;
		} else if (!comment.equals(other.comment))
			return false;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (modifiedBy == null) {
			if (other.modifiedBy != null)
				return false;
		} else if (!modifiedBy.equals(other.modifiedBy))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (!Arrays.equals(references, other.references))
			return false;
		if (revisionNumber == null) {
			if (other.revisionNumber != null)
				return false;
		} else if (!revisionNumber.equals(other.revisionNumber))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DBORevision [owner=" + owner + ", revisionNumber="
				+ revisionNumber + ", activityId=" + activityId + ", label="
				+ label + ", comment=" + comment + ", modifiedBy=" + modifiedBy
				+ ", modifiedOn=" + modifiedOn + ", fileHandleId="
				+ fileHandleId + ", annotations="
				+ Arrays.toString(annotations) + ", references="
				+ Arrays.toString(references) + "]";
	}
	
}
