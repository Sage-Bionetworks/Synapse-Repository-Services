package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_ACCESS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_CONCRETE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_ACCESS_REQUIREMENT_REVISION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT_REVISION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOAccessRequirementRevision implements MigratableDatabaseObject<DBOAccessRequirementRevision, DBOAccessRequirementRevision> {

	private Long ownerId;
	private Long modifiedBy;
	private long modifiedOn;
	private String accessType;
	private String concreteType;
	private byte[] serializedEntity;
	private Long number;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("ownerId", COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID, true).withIsBackupId(true),
		new FieldColumn("number", COL_ACCESS_REQUIREMENT_REVISION_NUMBER, true),
		new FieldColumn("modifiedBy", COL_ACCESS_REQUIREMENT_REVISION_MODIFIED_BY),
		new FieldColumn("modifiedOn", COL_ACCESS_REQUIREMENT_REVISION_MODIFIED_ON),
		new FieldColumn("accessType", COL_ACCESS_REQUIREMENT_REVISION_ACCESS_TYPE),
		new FieldColumn("concreteType", COL_ACCESS_REQUIREMENT_REVISION_CONCRETE_TYPE),
		new FieldColumn("serializedEntity", COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY)
		};

	public Long getOwnerId() {
		return ownerId;
	}


	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}


	public Long getModifiedBy() {
		return modifiedBy;
	}


	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}


	public long getModifiedOn() {
		return modifiedOn;
	}


	public void setModifiedOn(long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}


	public String getAccessType() {
		return accessType;
	}


	public void setAccessType(String accessType) {
		this.accessType = accessType;
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


	public Long getNumber() {
		return number;
	}


	public void setNumber(Long number) {
		this.number = number;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessType == null) ? 0 : accessType.hashCode());
		result = prime * result + ((concreteType == null) ? 0 : concreteType.hashCode());
		result = prime * result + ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result + (int) (modifiedOn ^ (modifiedOn >>> 32));
		result = prime * result + ((number == null) ? 0 : number.hashCode());
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
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
		DBOAccessRequirementRevision other = (DBOAccessRequirementRevision) obj;
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
		if (modifiedBy == null) {
			if (other.modifiedBy != null)
				return false;
		} else if (!modifiedBy.equals(other.modifiedBy))
			return false;
		if (modifiedOn != other.modifiedOn)
			return false;
		if (number == null) {
			if (other.number != null)
				return false;
		} else if (!number.equals(other.number))
			return false;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		if (!Arrays.equals(serializedEntity, other.serializedEntity))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "DBOAccessRequirementRevision [ownerId=" + ownerId + ", modifiedBy=" + modifiedBy + ", modifiedOn="
				+ modifiedOn + ", accessType=" + accessType + ", concreteType=" + concreteType + ", serializedEntity="
				+ Arrays.toString(serializedEntity) + ", number=" + number + "]";
	}


	@Override
	public TableMapping<DBOAccessRequirementRevision> getTableMapping() {
		return new TableMapping<DBOAccessRequirementRevision>() {
			// Map a result set to this object
			@Override
			public DBOAccessRequirementRevision mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOAccessRequirementRevision ar = new DBOAccessRequirementRevision();
				ar.setOwnerId(rs.getLong(COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID));
				ar.setNumber(rs.getLong(COL_ACCESS_REQUIREMENT_REVISION_NUMBER));
				ar.setModifiedBy(rs.getLong(COL_ACCESS_REQUIREMENT_REVISION_MODIFIED_BY));
				ar.setModifiedOn(rs.getLong(COL_ACCESS_REQUIREMENT_REVISION_MODIFIED_ON));
				ar.setAccessType(rs.getString(COL_ACCESS_REQUIREMENT_REVISION_ACCESS_TYPE));
				ar.setConcreteType(rs.getString(COL_ACCESS_REQUIREMENT_REVISION_CONCRETE_TYPE));
				java.sql.Blob blob = rs.getBlob(COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY);
				if(blob != null){
					ar.setSerializedEntity(blob.getBytes(1, (int) blob.length()));
				}
				return ar;
			}

			@Override
			public String getTableName() {
				return TABLE_ACCESS_REQUIREMENT_REVISION;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_ACCESS_REQUIREMENT_REVISION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOAccessRequirementRevision> getDBOClass() {
				return DBOAccessRequirementRevision.class;
			}
		};
	}


	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.ACCESS_REQUIREMENT_REVISION;
	}
	
	public static void copyEntityIdsToAccessRequirement(List<String> entityIds, AccessRequirement ar) {
		if (entityIds==null) return;
		if (ar.getSubjectIds()==null) ar.setSubjectIds(new ArrayList<RestrictableObjectDescriptor>());
		for (String entityId : entityIds) {
			RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
			subjectId.setId(entityId);
			subjectId.setType(RestrictableObjectType.ENTITY);
			if (!ar.getSubjectIds().contains(subjectId)) ar.getSubjectIds().add(subjectId);
		}	
	}

	@Override
	public MigratableTableTranslation<DBOAccessRequirementRevision, DBOAccessRequirementRevision> getTranslator() {
		return new MigratableTableTranslation<DBOAccessRequirementRevision, DBOAccessRequirementRevision>(){

			@Override
			public DBOAccessRequirementRevision createDatabaseObjectFromBackup(
					DBOAccessRequirementRevision backup) {
				return backup;
			}
			
			@Override
			public DBOAccessRequirementRevision createBackupFromDatabaseObject(
					DBOAccessRequirementRevision dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBOAccessRequirementRevision> getBackupClass() {
		return DBOAccessRequirementRevision.class;
	}


	@Override
	public Class<? extends DBOAccessRequirementRevision> getDatabaseObjectClass() {
		return DBOAccessRequirementRevision.class;
	}


	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
}
