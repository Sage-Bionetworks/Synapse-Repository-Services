package org.sagebionetworks.repo.model.dbo.form;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_FILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_REJECTION_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_REVIEWED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_REVIEWED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_SUBMITTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_FORM_DATA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FORM_DATA;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOFormData implements MigratableDatabaseObject<DBOFormData, DBOFormData> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_FORM_DATA_ID, true).withIsBackupId(true),
			new FieldColumn("etag", COL_FORM_DATA_ETAG).withIsEtag(true),
			new FieldColumn("name", COL_FORM_DATA_NAME),
			new FieldColumn("createdOn", COL_FORM_DATA_CREATED_ON),
			new FieldColumn("createdBy", COL_FORM_DATA_CREATED_BY),
			new FieldColumn("modifiedOn", COL_FORM_DATA_MODIFIED_ON),
			new FieldColumn("groupId", COL_FORM_DATA_GROUP_ID),
			new FieldColumn("fileHandleId", COL_FORM_DATA_FILE_ID).withHasFileHandleRef(true),
			new FieldColumn("submittedOn", COL_FORM_DATA_SUBMITTED_ON),
			new FieldColumn("reviewedOn", COL_FORM_DATA_REVIEWED_ON),
			new FieldColumn("reviewedBy", COL_FORM_DATA_REVIEWED_BY),
			new FieldColumn("state", COL_FORM_DATA_STATE),
			new FieldColumn("rejectionMessage", COL_FORM_DATA_REJECTION_MESSAGE), };

	private Long id;
	private String etag;
	private String name;
	private Timestamp createdOn;
	private Long createdBy;
	private Timestamp modifiedOn;
	private Long groupId;
	private Long fileHandleId;
	private Timestamp submittedOn;
	private Timestamp reviewedOn;
	private Long reviewedBy;
	private String state;
	private String rejectionMessage;

	@Override
	public TableMapping<DBOFormData> getTableMapping() {
		return new TableMapping<DBOFormData>() {

			@Override
			public DBOFormData mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOFormData dbo = new DBOFormData();
				dbo.setId(rs.getLong(COL_FORM_DATA_ID));
				dbo.setEtag(rs.getString(COL_FORM_DATA_ETAG));
				dbo.setName(rs.getString(COL_FORM_DATA_NAME));
				dbo.setCreatedOn(rs.getTimestamp(COL_FORM_DATA_CREATED_ON));
				dbo.setCreatedBy(rs.getLong(COL_FORM_DATA_CREATED_BY));
				dbo.setModifiedOn(rs.getTimestamp(COL_FORM_DATA_MODIFIED_ON));
				dbo.setGroupId(rs.getLong(COL_FORM_DATA_GROUP_ID));
				dbo.setFileHandleId(rs.getLong(COL_FORM_DATA_FILE_ID));
				dbo.setSubmittedOn(rs.getTimestamp(COL_FORM_DATA_SUBMITTED_ON));
				dbo.setReviewedOn(rs.getTimestamp(COL_FORM_DATA_REVIEWED_ON));
				dbo.setReviewedBy(rs.getLong(COL_FORM_DATA_REVIEWED_BY));
				if (rs.wasNull()) {
					dbo.setReviewedBy(null);
				}
				dbo.setState(rs.getString(COL_FORM_DATA_STATE));
				dbo.setRejectionMessage(rs.getString(COL_FORM_DATA_REJECTION_MESSAGE));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_FORM_DATA;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_FORM_DATA;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOFormData> getDBOClass() {
				return DBOFormData.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.FORM_DATA;
	}

	@Override
	public MigratableTableTranslation<DBOFormData, DBOFormData> getTranslator() {
		return new BasicMigratableTableTranslation<DBOFormData>();
	}

	@Override
	public Class<? extends DBOFormData> getBackupClass() {
		return DBOFormData.class;
	}

	@Override
	public Class<? extends DBOFormData> getDatabaseObjectClass() {
		return DBOFormData.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Timestamp getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public Long getGroupId() {
		return groupId;
	}

	public Long getFileHandleId() {
		return fileHandleId;
	}

	public Timestamp getSubmittedOn() {
		return submittedOn;
	}

	public void setSubmittedOn(Timestamp submittedOn) {
		this.submittedOn = submittedOn;
	}

	public Long getReviewedBy() {
		return reviewedBy;
	}

	public void setReviewedBy(Long reviewedBy) {
		this.reviewedBy = reviewedBy;
	}

	public String getRejectionMessage() {
		return rejectionMessage;
	}

	public void setRejectionMessage(String rejectionMessage) {
		this.rejectionMessage = rejectionMessage;
	}

	public Timestamp getReviewedOn() {
		return reviewedOn;
	}

	public void setReviewedOn(Timestamp reviewedOn) {
		this.reviewedOn = reviewedOn;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((rejectionMessage == null) ? 0 : rejectionMessage.hashCode());
		result = prime * result + ((reviewedBy == null) ? 0 : reviewedBy.hashCode());
		result = prime * result + ((reviewedOn == null) ? 0 : reviewedOn.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result + ((submittedOn == null) ? 0 : submittedOn.hashCode());
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
		DBOFormData other = (DBOFormData) obj;
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
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (groupId == null) {
			if (other.groupId != null)
				return false;
		} else if (!groupId.equals(other.groupId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (rejectionMessage == null) {
			if (other.rejectionMessage != null)
				return false;
		} else if (!rejectionMessage.equals(other.rejectionMessage))
			return false;
		if (reviewedBy == null) {
			if (other.reviewedBy != null)
				return false;
		} else if (!reviewedBy.equals(other.reviewedBy))
			return false;
		if (reviewedOn == null) {
			if (other.reviewedOn != null)
				return false;
		} else if (!reviewedOn.equals(other.reviewedOn))
			return false;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		if (submittedOn == null) {
			if (other.submittedOn != null)
				return false;
		} else if (!submittedOn.equals(other.submittedOn))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOFormData [id=" + id + ", etag=" + etag + ", name=" + name + ", createdOn=" + createdOn
				+ ", createdBy=" + createdBy + ", modifiedOn=" + modifiedOn + ", groupId=" + groupId + ", fileHandleId="
				+ fileHandleId + ", submittedOn=" + submittedOn + ", reviewedOn=" + reviewedOn + ", reviewedBy="
				+ reviewedBy + ", state=" + state + ", rejectionMessage=" + rejectionMessage + "]";
	}

}
