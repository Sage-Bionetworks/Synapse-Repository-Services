/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ACCESS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CONCRETE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_ACCESS_REQUIREMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * @author brucehoff
 *
 */
public class DBOAccessRequirement implements MigratableDatabaseObject<DBOAccessRequirement, DBOAccessRequirement> {
	private Long id;
	private String eTag;
	private Long createdBy;
	private long createdOn;
	private Long modifiedBy;
	private long modifiedOn;
	private String accessType;
	private String concreteType;
	private byte[] serializedEntity;
	private Long currentRevNumber;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_ACCESS_REQUIREMENT_ID, true).withIsBackupId(true),
		new FieldColumn("eTag", COL_ACCESS_REQUIREMENT_ETAG).withIsEtag(true),
		new FieldColumn("currentRevNumber", COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER),
		new FieldColumn("createdBy", COL_ACCESS_REQUIREMENT_CREATED_BY),
		new FieldColumn("createdOn", COL_ACCESS_REQUIREMENT_CREATED_ON),
		new FieldColumn("accessType", COL_ACCESS_REQUIREMENT_ACCESS_TYPE),
		new FieldColumn("concreteType", COL_ACCESS_REQUIREMENT_CONCRETE_TYPE),
		};


	@Override
	public TableMapping<DBOAccessRequirement> getTableMapping() {
		return new TableMapping<DBOAccessRequirement>() {
			// Map a result set to this object
			@Override
			public DBOAccessRequirement mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOAccessRequirement ar = new DBOAccessRequirement();
				ar.setId(rs.getLong(COL_ACCESS_REQUIREMENT_ID));
				ar.seteTag(rs.getString(COL_ACCESS_REQUIREMENT_ETAG));
				ar.setCurrentRevNumber(rs.getLong(COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER));
				ar.setCreatedBy(rs.getLong(COL_ACCESS_REQUIREMENT_CREATED_BY));
				ar.setCreatedOn(rs.getLong(COL_ACCESS_REQUIREMENT_CREATED_ON));
				ar.setAccessType(rs.getString(COL_ACCESS_REQUIREMENT_ACCESS_TYPE));
				ar.setConcreteType(rs.getString(COL_ACCESS_REQUIREMENT_CONCRETE_TYPE));
				return ar;
			}

			@Override
			public String getTableName() {
				return TABLE_ACCESS_REQUIREMENT;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_ACCESS_REQUIREMENT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOAccessRequirement> getDBOClass() {
				return DBOAccessRequirement.class;
			}
		};
	}


	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
	}


	public String geteTag() {
		return eTag;
	}


	public void seteTag(String eTag) {
		this.eTag = eTag;
	}


	public Long getCurrentRevNumber() {
		return currentRevNumber;
	}


	public void setCurrentRevNumber(Long currentRevNumber) {
		this.currentRevNumber = currentRevNumber;
	}


	public Long getCreatedBy() {
		return createdBy;
	}


	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}



	public Long getModifiedBy() {
		return modifiedBy;
	}


	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public String getAccessType() {
		return accessType;
	}


	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}

	public long getCreatedOn() {
		return createdOn;
	}


	public void setCreatedOn(long createdOn) {
		this.createdOn = createdOn;
	}


	public long getModifiedOn() {
		return modifiedOn;
	}


	public void setModifiedOn(long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public String getConcreteType() {
		return concreteType;
	}


	public void setConcreteType(String concreteType) {
		this.concreteType = concreteType;
	}


	public byte[] getSerializedEntity() {
		return serializedEntity;
	}


	public void setSerializedEntity(byte[] serializedEntity) {
		this.serializedEntity = serializedEntity;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessType == null) ? 0 : accessType.hashCode());
		result = prime * result + ((concreteType == null) ? 0 : concreteType.hashCode());
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + (int) (createdOn ^ (createdOn >>> 32));
		result = prime * result + ((currentRevNumber == null) ? 0 : currentRevNumber.hashCode());
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result + (int) (modifiedOn ^ (modifiedOn >>> 32));
		result = prime * result + Arrays.hashCode(serializedEntity);
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
		DBOAccessRequirement other = (DBOAccessRequirement) obj;
		if (accessType == null) {
			if (other.accessType != null)
				return false;
		} else if (!accessType.equals(other.accessType))
			return false;
		if (concreteType == null) {
			if (other.concreteType != null)
				return false;
		} else if (!concreteType.equals(other.concreteType))
			return false;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn != other.createdOn)
			return false;
		if (currentRevNumber == null) {
			if (other.currentRevNumber != null)
				return false;
		} else if (!currentRevNumber.equals(other.currentRevNumber))
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
		if (modifiedOn != other.modifiedOn)
			return false;
		if (!Arrays.equals(serializedEntity, other.serializedEntity))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "DBOAccessRequirement [id=" + id + ", eTag=" + eTag + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + ", accessType=" + accessType
				+ ", concreteType=" + concreteType + ", serializedEntity=" + Arrays.toString(serializedEntity)
				+ ", currentRevNumber=" + currentRevNumber + "]";
	}


	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.ACCESS_REQUIREMENT;
	}

	@Override
	public MigratableTableTranslation<DBOAccessRequirement, DBOAccessRequirement> getTranslator() {
		return new BasicMigratableTableTranslation<DBOAccessRequirement>();
	}

	@Override
	public Class<? extends DBOAccessRequirement> getBackupClass() {
		return DBOAccessRequirement.class;
	}

	@Override
	public Class<? extends DBOAccessRequirement> getDatabaseObjectClass() {
		return DBOAccessRequirement.class;
	}


	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new DBOSubjectAccessRequirement());
		list.add(new DBOAccessRequirementRevision());
		return list;
	}
}
