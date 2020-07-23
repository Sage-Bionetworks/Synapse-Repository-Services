/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ACCESSOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_EXPIRED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_REQUIREMENT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_SUBMITTER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_ACCESS_APPROVAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_APPROVAL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOAccessApproval implements MigratableDatabaseObject<DBOAccessApproval, DBOAccessApproval> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_ACCESS_APPROVAL_ID, true).withIsBackupId(true),
		new FieldColumn("eTag", COL_ACCESS_APPROVAL_ETAG).withIsEtag(true),
		new FieldColumn("createdBy", COL_ACCESS_APPROVAL_CREATED_BY),
		new FieldColumn("createdOn", COL_ACCESS_APPROVAL_CREATED_ON),
		new FieldColumn("modifiedBy", COL_ACCESS_APPROVAL_MODIFIED_BY),
		new FieldColumn("modifiedOn", COL_ACCESS_APPROVAL_MODIFIED_ON),
		new FieldColumn("expiredOn", COL_ACCESS_APPROVAL_EXPIRED_ON),
		new FieldColumn("requirementId", COL_ACCESS_APPROVAL_REQUIREMENT_ID),
		new FieldColumn("requirementVersion", COL_ACCESS_APPROVAL_REQUIREMENT_VERSION),
		new FieldColumn("submitterId", COL_ACCESS_APPROVAL_SUBMITTER_ID),
		new FieldColumn("accessorId", COL_ACCESS_APPROVAL_ACCESSOR_ID),
		new FieldColumn("state", COL_ACCESS_APPROVAL_STATE)
		};

	
	private static final TableMapping<DBOAccessApproval> TABLE_MAPPING = new TableMapping<DBOAccessApproval>() {
		// Map a result set to this object
		@Override
		public DBOAccessApproval mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOAccessApproval aa = new DBOAccessApproval();
			aa.setId(rs.getLong(COL_ACCESS_APPROVAL_ID));
			aa.seteTag(rs.getString(COL_ACCESS_APPROVAL_ETAG));
			aa.setCreatedBy(rs.getLong(COL_ACCESS_APPROVAL_CREATED_BY));
			aa.setCreatedOn(rs.getLong(COL_ACCESS_APPROVAL_CREATED_ON));
			aa.setModifiedBy(rs.getLong(COL_ACCESS_APPROVAL_MODIFIED_BY));
			aa.setModifiedOn(rs.getLong(COL_ACCESS_APPROVAL_MODIFIED_ON));
			aa.setExpiredOn(rs.getLong(COL_ACCESS_APPROVAL_EXPIRED_ON));
			aa.setRequirementId(rs.getLong(COL_ACCESS_APPROVAL_REQUIREMENT_ID));
			aa.setRequirementVersion(rs.getLong(COL_ACCESS_APPROVAL_REQUIREMENT_VERSION));
			aa.setSubmitterId(rs.getLong(COL_ACCESS_APPROVAL_SUBMITTER_ID));
			aa.setAccessorId(rs.getLong(COL_ACCESS_APPROVAL_ACCESSOR_ID));
			aa.setState(rs.getString(COL_ACCESS_APPROVAL_STATE));
			return aa;
		}

		@Override
		public String getTableName() {
			return TABLE_ACCESS_APPROVAL;
		}

		@Override
		public String getDDLFileName() {
			return DDL_FILE_ACCESS_APPROVAL;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOAccessApproval> getDBOClass() {
			return DBOAccessApproval.class;
		}
	};
	
	private static final MigratableTableTranslation<DBOAccessApproval, DBOAccessApproval> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();
	
	private Long id;
	private String eTag;
	private Long createdBy;
	private long createdOn;
	private Long modifiedBy;
	private long modifiedOn;
	private long expiredOn;
	private Long requirementId;
	private Long accessorId;
	private Long requirementVersion;
	private Long submitterId;
	private String state;

	@Override
	public TableMapping<DBOAccessApproval> getTableMapping() {
		return TABLE_MAPPING;
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


	public Long getRequirementId() {
		return requirementId;
	}


	public void setRequirementId(Long requirementId) {
		this.requirementId = requirementId;
	}


	public Long getAccessorId() {
		return accessorId;
	}


	public void setAccessorId(Long accessorId) {
		this.accessorId = accessorId;
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

	public long getExpiredOn() {
		return expiredOn;
	}


	public void setExpiredOn(long expiredOn) {
		this.expiredOn = expiredOn;
	}


	public Long getRequirementVersion() {
		return requirementVersion;
	}


	public void setRequirementVersion(Long requirementVersion) {
		this.requirementVersion = requirementVersion;
	}


	public Long getSubmitterId() {
		return submitterId;
	}


	public void setSubmitterId(Long submitterId) {
		this.submitterId = submitterId;
	}


	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.ACCESS_APPROVAL;
	}


	@Override
	public MigratableTableTranslation<DBOAccessApproval, DBOAccessApproval> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}


	@Override
	public Class<? extends DBOAccessApproval> getBackupClass() {
		return DBOAccessApproval.class;
	}


	@Override
	public Class<? extends DBOAccessApproval> getDatabaseObjectClass() {
		return DBOAccessApproval.class;
	}


	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}


	@Override
	public int hashCode() {
		return Objects.hash(accessorId, createdBy, createdOn, eTag, expiredOn, id, modifiedBy, modifiedOn,
				requirementId, requirementVersion, state, submitterId);
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
		DBOAccessApproval other = (DBOAccessApproval) obj;
		return Objects.equals(accessorId, other.accessorId) && Objects.equals(createdBy, other.createdBy)
				&& createdOn == other.createdOn && Objects.equals(eTag, other.eTag) && expiredOn == other.expiredOn
				&& Objects.equals(id, other.id) && Objects.equals(modifiedBy, other.modifiedBy)
				&& modifiedOn == other.modifiedOn && Objects.equals(requirementId, other.requirementId)
				&& Objects.equals(requirementVersion, other.requirementVersion) && Objects.equals(state, other.state)
				&& Objects.equals(submitterId, other.submitterId);
	}


	@Override
	public String toString() {
		return "DBOAccessApproval [id=" + id + ", eTag=" + eTag + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + ", expiredOn=" + expiredOn
				+ ", requirementId=" + requirementId + ", accessorId=" + accessorId + ", requirementVersion="
				+ requirementVersion + ", submitterId=" + submitterId + ", state=" + state + "]";
	}

}
