package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ACTIVITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ANNOS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_COLUMN_MODEL_IDS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_COMMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_LABEL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_REF_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_SCOPE_IDS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_USER_ANNOTATIONS_V1_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_REVISION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REVISION;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.jdo.AnnotationUtils;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * The DatabaseObject for Revision.
 * 
 * @author jmhill
 *
 */
public class DBORevision implements MigratableDatabaseObject<DBORevision, DBORevision> {
	public static final int MAX_COMMENT_LENGTH = 256;

	static final MigratableTableTranslation<DBORevision, DBORevision> TRANSLATOR = new BasicMigratableTableTranslation<DBORevision>() {
		@Override
		public DBORevision createDatabaseObjectFromBackup(DBORevision backup){
			if (backup.getAnnotations() != null){
				try {
					NamedAnnotations namedAnnotations = AnnotationUtils.decompressedAnnotations(backup.getAnnotations());

					backup.setEntityPropertyAnnotations(AnnotationUtils.compressAnnotationsV1(namedAnnotations.getPrimaryAnnotations()));
					backup.setUserAnnotationsV1(AnnotationUtils.compressAnnotationsV1(namedAnnotations.getAdditionalAnnotations()));

					backup.setAnnotations(null);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			return backup;
		}
	};
	
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
		new FieldColumn("scopeIds", COL_REVISION_SCOPE_IDS),
		new FieldColumn("annotations", COL_REVISION_ANNOS_BLOB),
		new FieldColumn("entityPropertyAnnotations", COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB),
		new FieldColumn("userAnnotationsV1", COL_REVISION_USER_ANNOTATIONS_V1_BLOB),
		new FieldColumn("reference", COL_REVISION_REF_BLOB)
		};

	@Override
	public TableMapping<DBORevision> getTableMapping() {
		return new TableMapping<DBORevision>(){
			@Override
			public DBORevision mapRow(ResultSet rs, int rowNum)	throws SQLException {
				boolean includeAnnotations = true;
				DBORevisionMapper mapper = new DBORevisionMapper(includeAnnotations);
				return mapper.mapRow(rs, rowNum);
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
	private byte[] scopeIds;
	private byte[] annotations;
	private byte[] entityPropertyAnnotations;
	private byte[] userAnnotationsV1;
	private byte[] reference;
	// used for migration only

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
	public byte[] getReference() {
		return reference;
	}
	public void setReference(byte[] reference) {
		this.reference = reference;
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
	
	public byte[] getScopeIds() {
		return scopeIds;
	}
	public void setScopeIds(byte[] scopeIds) {
		this.scopeIds = scopeIds;
	}
	public Long getFileHandleId() {
		return fileHandleId;
	}
	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}

	public byte[] getEntityPropertyAnnotations() {
		return entityPropertyAnnotations;
	}

	public void setEntityPropertyAnnotations(byte[] entityPropertyAnnotations) {
		this.entityPropertyAnnotations = entityPropertyAnnotations;
	}

	public byte[] getUserAnnotationsV1() {
		return userAnnotationsV1;
	}

	public void setUserAnnotationsV1(byte[] userAnnotationsV1) {
		this.userAnnotationsV1 = userAnnotationsV1;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.NODE_REVISION;
	}
	@Override
	public MigratableTableTranslation<DBORevision, DBORevision> getTranslator() {
		return TRANSLATOR;
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
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((activityId == null) ? 0 : activityId.hashCode());
		result = prime * result + Arrays.hashCode(annotations);
		result = prime * result + Arrays.hashCode(entityPropertyAnnotations);
		result = prime * result + Arrays.hashCode(userAnnotationsV1);
		result = prime * result + Arrays.hashCode(columnModelIds);
		result = prime * result + ((comment == null) ? 0 : comment.hashCode());
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result
				+ ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + Arrays.hashCode(reference);
		result = prime * result
				+ ((revisionNumber == null) ? 0 : revisionNumber.hashCode());
		result = prime * result + Arrays.hashCode(scopeIds);
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
		if (!Arrays.equals(entityPropertyAnnotations, other.entityPropertyAnnotations))
			return false;
		if (!Arrays.equals(userAnnotationsV1, other.userAnnotationsV1))
			return false;
		if (!Arrays.equals(columnModelIds, other.columnModelIds))
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
		if (!Arrays.equals(reference, other.reference))
			return false;
		if (revisionNumber == null) {
			if (other.revisionNumber != null)
				return false;
		} else if (!revisionNumber.equals(other.revisionNumber))
			return false;
		if (!Arrays.equals(scopeIds, other.scopeIds))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DBORevision [owner=" + owner + ", revisionNumber="
				+ revisionNumber + ", activityId=" + activityId + ", label="
				+ label + ", comment=" + comment + ", modifiedBy=" + modifiedBy
				+ ", modifiedOn=" + modifiedOn + ", fileHandleId="
				+ fileHandleId + ", columnModelIds="
				+ Arrays.toString(columnModelIds) + ", scopeIds="
				+ Arrays.toString(scopeIds) + ", annotations="
				+ Arrays.toString(annotations) + ", entityPropertyAnnotations="
				+ Arrays.toString(entityPropertyAnnotations) + ", userAnnotationsV1="
				+ Arrays.toString(userAnnotationsV1) + ", reference="
				+ Arrays.toString(reference) + "]";
	}
	
}
