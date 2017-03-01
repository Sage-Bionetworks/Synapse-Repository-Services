package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBODataAccessRequest implements MigratableDatabaseObject<DBODataAccessRequest, DBODataAccessRequest>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", DATA_ACCESS_REQUEST_ID, true).withIsBackupId(true),
			new FieldColumn("accessRequirementId", DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID),
			new FieldColumn("researchProjectId", DATA_ACCESS_REQUEST_RESEARCH_PROJECT_ID),
			new FieldColumn("createdBy", DATA_ACCESS_REQUEST_CREATED_BY),
			new FieldColumn("createdOn", DATA_ACCESS_REQUEST_CREATED_ON),
			new FieldColumn("modifiedBy", DATA_ACCESS_REQUEST_MODIFIED_BY),
			new FieldColumn("modifiedOn", DATA_ACCESS_REQUEST_MODIFIED_ON),
			new FieldColumn("etag", DATA_ACCESS_REQUEST_ETAG).withIsEtag(true),
			new FieldColumn("accessors", DATA_ACCESS_REQUEST_ACCESSORS),
			new FieldColumn("requestSerialized", DATA_ACCESS_REQUEST_REQUEST_SERIALIZED)
		};

	private Long id;
	private Long accessRequirementId;
	private Long researchProjectId;
	private Long createdBy;
	private Date createdOn;
	private Long modifiedBy;
	private Date modifiedOn;
	private String etag;
	private String accessors;
	private byte[] requestSerialized;

	@Override
	public String toString() {
		return "DBODataAccessRequest [id=" + id + ", accessRequirementId=" + accessRequirementId
				+ ", researchProjectId=" + researchProjectId + ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + ", etag=" + etag + ", accessors="
				+ accessors + ", requestSerialized=" + Arrays.toString(requestSerialized) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessRequirementId == null) ? 0 : accessRequirementId.hashCode());
		result = prime * result + ((accessors == null) ? 0 : accessors.hashCode());
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result + ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + Arrays.hashCode(requestSerialized);
		result = prime * result + ((researchProjectId == null) ? 0 : researchProjectId.hashCode());
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
		DBODataAccessRequest other = (DBODataAccessRequest) obj;
		if (accessRequirementId == null) {
			if (other.accessRequirementId != null)
				return false;
		} else if (!accessRequirementId.equals(other.accessRequirementId))
			return false;
		if (accessors == null) {
			if (other.accessors != null)
				return false;
		} else if (!accessors.equals(other.accessors))
			return false;
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
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
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
		if (!Arrays.equals(requestSerialized, other.requestSerialized))
			return false;
		if (researchProjectId == null) {
			if (other.researchProjectId != null)
				return false;
		} else if (!researchProjectId.equals(other.researchProjectId))
			return false;
		return true;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getAccessRequirementId() {
		return accessRequirementId;
	}

	public void setAccessRequirementId(Long accessRequirementId) {
		this.accessRequirementId = accessRequirementId;
	}

	public Long getResearchProjectId() {
		return researchProjectId;
	}

	public void setResearchProjectId(Long researchProjectId) {
		this.researchProjectId = researchProjectId;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Date getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getAccessors() {
		return accessors;
	}

	public void setAccessors(String accessors) {
		this.accessors = accessors;
	}

	public byte[] getRequestSerialized() {
		return requestSerialized;
	}

	public void setRequestSerialized(byte[] requestSerialized) {
		this.requestSerialized = requestSerialized;
	}

	@Override
	public TableMapping<DBODataAccessRequest> getTableMapping() {
		return new TableMapping<DBODataAccessRequest>(){

			@Override
			public DBODataAccessRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBODataAccessRequest dbo = new DBODataAccessRequest();
				dbo.setId(rs.getLong(DATA_ACCESS_REQUEST_ID));
				dbo.setAccessRequirementId(rs.getLong(DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID));
				dbo.setResearchProjectId(rs.getLong(DATA_ACCESS_REQUEST_RESEARCH_PROJECT_ID));
				dbo.setCreatedBy(rs.getLong(DATA_ACCESS_REQUEST_CREATED_BY));
				dbo.setCreatedOn(new Date(rs.getTimestamp(DATA_ACCESS_REQUEST_CREATED_ON).getTime()));
				dbo.setModifiedBy(rs.getLong(DATA_ACCESS_REQUEST_MODIFIED_BY));
				dbo.setModifiedOn(new Date(rs.getTimestamp(DATA_ACCESS_REQUEST_MODIFIED_ON).getTime()));
				dbo.setEtag(rs.getString(DATA_ACCESS_REQUEST_ETAG));
				dbo.setAccessors(rs.getString(DATA_ACCESS_REQUEST_ACCESSORS));
				Blob blob = rs.getBlob(DATA_ACCESS_REQUEST_REQUEST_SERIALIZED);
				dbo.setRequestSerialized(blob.getBytes(1, (int) blob.length()));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_DATA_ACCESS_REQUEST;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DATA_ACCESS_REQUEST;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBODataAccessRequest> getDBOClass() {
				return DBODataAccessRequest.class;
			}
			
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DATA_ACCESS_REQUEST;
	}

	@Override
	public MigratableTableTranslation<DBODataAccessRequest, DBODataAccessRequest> getTranslator() {
		return new MigratableTableTranslation<DBODataAccessRequest, DBODataAccessRequest>(){

			@Override
			public DBODataAccessRequest createDatabaseObjectFromBackup(DBODataAccessRequest backup) {
				return backup;
			}

			@Override
			public DBODataAccessRequest createBackupFromDatabaseObject(DBODataAccessRequest dbo) {
				return dbo;
			}
			
		};
	}

	@Override
	public Class<? extends DBODataAccessRequest> getBackupClass() {
		return DBODataAccessRequest.class;
	}

	@Override
	public Class<? extends DBODataAccessRequest> getDatabaseObjectClass() {
		return DBODataAccessRequest.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
