package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_SERIALIZED_OBJECT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_ACTIVITY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACTIVITY;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * @author dburdick
 *
 */
public class DBOActivity implements MigratableDatabaseObject<DBOActivity, DBOActivity>, ObservableEntity {
	private Long id;
	private String eTag;
	private Long createdBy;
	private Long createdOn;
	private Long modifiedBy;
	private Long modifiedOn;
	private byte[] serializedObject;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_ACTIVITY_ID, true).withIsBackupId(true),
		new FieldColumn("eTag", COL_ACTIVITY_ETAG).withIsEtag(true),
		new FieldColumn("createdBy", COL_ACTIVITY_CREATED_BY),
		new FieldColumn("createdOn", COL_ACTIVITY_CREATED_ON),
		new FieldColumn("modifiedBy", COL_ACTIVITY_MODIFIED_BY),
		new FieldColumn("modifiedOn", COL_ACTIVITY_MODIFIED_ON),
		new FieldColumn("serializedObject", COL_ACTIVITY_SERIALIZED_OBJECT)
		};

	@Override
	public TableMapping<DBOActivity> getTableMapping() {
		return new TableMapping<DBOActivity>() {
			// Map a result set to this object
			@Override
			public DBOActivity mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOActivity act = new DBOActivity();
				act.setId(rs.getLong(COL_ACTIVITY_ID));
				act.seteTag(rs.getString(COL_ACTIVITY_ETAG));
				act.setCreatedBy(rs.getLong(COL_ACTIVITY_CREATED_BY));
				act.setCreatedOn(rs.getLong(COL_ACTIVITY_CREATED_ON));
				act.setModifiedBy(rs.getLong(COL_ACTIVITY_MODIFIED_BY));
				act.setModifiedOn(rs.getLong(COL_ACTIVITY_MODIFIED_ON));
				java.sql.Blob blob = rs.getBlob(COL_ACTIVITY_SERIALIZED_OBJECT);
				if(blob != null){
					act.setSerializedObject(blob.getBytes(1, (int) blob.length()));
				}
				return act;
			}

			@Override
			public String getTableName() {
				return TABLE_ACTIVITY;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_ACTIVITY;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOActivity> getDBOClass() {
				return DBOActivity.class;
			}
		};
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
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

	public byte[] getSerializedObject() {
		return serializedObject;
	}

	public void setSerializedObject(byte[] serializedObject) {
		this.serializedObject = serializedObject;
	}

	@Override
	public ObjectType getObjectType() {
		return ObjectType.ACTIVITY;
	}
	@Override
	public String getIdString() {
		return id.toString();
	}

	@Override
	public String getEtag() {
		return eTag;
	}

	public String geteTag() {
		return eTag;
	}

	public void seteTag(String eTag) {
		this.eTag = eTag;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + Arrays.hashCode(serializedObject);
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
		DBOActivity other = (DBOActivity) obj;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
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
		if (!Arrays.equals(serializedObject, other.serializedObject))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOActivity [id=" + id + ", eTag=" + eTag + ", createdBy="
				+ createdBy + ", createdOn=" + createdOn + ", modifiedBy="
				+ modifiedBy + ", modifiedOn=" + modifiedOn
				+ ", serializedObject=" + Arrays.toString(serializedObject)
				+ "]";
	}

	@Override
	public String getParentIdString() {
		return null;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.ACTIVITY;
	}

	@Override
	public MigratableTableTranslation<DBOActivity, DBOActivity> getTranslator() {
		return new BasicMigratableTableTranslation<DBOActivity>();
	}

	@Override
	public Class<? extends DBOActivity> getBackupClass() {
		return DBOActivity.class;
	}

	@Override
	public Class<? extends DBOActivity> getDatabaseObjectClass() {
		return DBOActivity.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
	

}
