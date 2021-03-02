package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_ACCESS_REQUIREMENT_REVISION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT_REVISION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOAccessRequirementRevision implements MigratableDatabaseObject<DBOAccessRequirementRevision, DBOAccessRequirementRevision> {

	private Long ownerId;
	private Long modifiedBy;
	private long modifiedOn;
	private byte[] serializedEntity;
	private Long number;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("ownerId", COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID, true).withIsBackupId(true),
		new FieldColumn("number", COL_ACCESS_REQUIREMENT_REVISION_NUMBER, true),
		new FieldColumn("modifiedBy", COL_ACCESS_REQUIREMENT_REVISION_MODIFIED_BY),
		new FieldColumn("modifiedOn", COL_ACCESS_REQUIREMENT_REVISION_MODIFIED_ON),
		new FieldColumn("serializedEntity", COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY).withHasFileHandleRef(true)
		};
	
	private static final TableMapping<DBOAccessRequirementRevision> TABLE_MAPPER = new TableMapping<DBOAccessRequirementRevision>() {
		// Map a result set to this object
		@Override
		public DBOAccessRequirementRevision mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOAccessRequirementRevision ar = new DBOAccessRequirementRevision();
			ar.setOwnerId(rs.getLong(COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID));
			ar.setNumber(rs.getLong(COL_ACCESS_REQUIREMENT_REVISION_NUMBER));
			ar.setModifiedBy(rs.getLong(COL_ACCESS_REQUIREMENT_REVISION_MODIFIED_BY));
			ar.setModifiedOn(rs.getLong(COL_ACCESS_REQUIREMENT_REVISION_MODIFIED_ON));
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
	
	private static final MigratableTableTranslation<DBOAccessRequirementRevision, DBOAccessRequirementRevision> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();
	
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
	public TableMapping<DBOAccessRequirementRevision> getTableMapping() {
		return TABLE_MAPPER;
	}


	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.ACCESS_REQUIREMENT_REVISION;
	}

	@Override
	public MigratableTableTranslation<DBOAccessRequirementRevision, DBOAccessRequirementRevision> getTranslator() {
		return MIGRATION_TRANSLATOR;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(serializedEntity);
		result = prime * result + Objects.hash(modifiedBy, modifiedOn, number, ownerId);
		return result;
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
		DBOAccessRequirementRevision other = (DBOAccessRequirementRevision) obj;
		return Objects.equals(modifiedBy, other.modifiedBy) && modifiedOn == other.modifiedOn && Objects.equals(number, other.number)
				&& Objects.equals(ownerId, other.ownerId) && Arrays.equals(serializedEntity, other.serializedEntity);
	}

	@Override
	public String toString() {
		return "DBOAccessRequirementRevision [ownerId=" + ownerId + ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn
				+ ", serializedEntity=" + Arrays.toString(serializedEntity) + ", number=" + number + "]";
	}
	
}
