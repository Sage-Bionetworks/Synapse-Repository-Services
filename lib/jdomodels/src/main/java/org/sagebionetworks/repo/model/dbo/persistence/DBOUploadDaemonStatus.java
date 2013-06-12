package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_STATUS_ERROR_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_STATUS_FILE_HANDLE_IDS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_STATUS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_STATUS_PERCENT_COMPLETE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_STATUS_RUNTIME_MS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_STATUS_STARTED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_STATUS_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_STATUS_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_UPLOAD_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_UPLOAD_STATUS;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.AutoIncrementDatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.file.State;

public class DBOUploadDaemonStatus implements AutoIncrementDatabaseObject<DBOUploadDaemonStatus> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_UPLOAD_STATUS_ID, true),
		new FieldColumn("state", COL_UPLOAD_STATUS_STATE),
		new FieldColumn("startedBy", COL_UPLOAD_STATUS_STARTED_BY),
		new FieldColumn("startedOn", COL_UPLOAD_STATUS_STARTED_ON),
		new FieldColumn("percentComplete", COL_UPLOAD_STATUS_PERCENT_COMPLETE),
		new FieldColumn("errorMessage", COL_UPLOAD_STATUS_ERROR_MESSAGE),
		new FieldColumn("fileHandleId", COL_UPLOAD_STATUS_FILE_HANDLE_IDS),
		new FieldColumn("runTimeMS", COL_UPLOAD_STATUS_RUNTIME_MS),
	};
	
	private Long id;
	private State state;
	private Long startedBy;
	private Long startedOn;
	private Double percentComplete;
	private String errorMessage;
	private Long fileHandleId;
	private Long runTimeMS;

	@Override
	public TableMapping<DBOUploadDaemonStatus> getTableMapping() {
		return new TableMapping<DBOUploadDaemonStatus>(){

			@Override
			public DBOUploadDaemonStatus mapRow(ResultSet rs, int rowNum)throws SQLException {
				DBOUploadDaemonStatus status = new DBOUploadDaemonStatus();
				status.setId(rs.getLong(COL_UPLOAD_STATUS_ID));
				status.setState(State.valueOf(rs.getString(COL_UPLOAD_STATUS_STATE)).name());
				status.setStartedBy(rs.getLong(COL_UPLOAD_STATUS_STARTED_BY));
				status.setStartedOn(rs.getLong(COL_UPLOAD_STATUS_STARTED_ON));
				status.setPercentComplete(rs.getDouble(COL_UPLOAD_STATUS_PERCENT_COMPLETE));
				status.setErrorMessage(rs.getString(COL_UPLOAD_STATUS_ERROR_MESSAGE));
				status.setFileHandleId(rs.getLong(COL_UPLOAD_STATUS_FILE_HANDLE_IDS));
				if(rs.wasNull()){
					status.setFileHandleId(null);
				}
				status.setRunTimeMS(rs.getLong(COL_UPLOAD_STATUS_RUNTIME_MS));
				return status;
			}

			@Override
			public String getTableName() {
				return TABLE_UPLOAD_STATUS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_UPLOAD_STATUS;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOUploadDaemonStatus> getDBOClass() {
				return DBOUploadDaemonStatus.class;
			}};
	}

	@Override
	public Long getId() {
		return this.id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public String getState() {
		return state.name();
	}

	public void setState(String state) {
		this.state = State.valueOf(state);
	}

	public Long getStartedBy() {
		return startedBy;
	}

	public void setStartedBy(Long startedBy) {
		this.startedBy = startedBy;
	}

	public Long getStartedOn() {
		return startedOn;
	}

	public void setStartedOn(Long startedOn) {
		this.startedOn = startedOn;
	}

	public Double getPercentComplete() {
		return percentComplete;
	}

	public void setPercentComplete(Double percentComplete) {
		this.percentComplete = percentComplete;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Long getFileHandleId() {
		return fileHandleId;
	}

	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}

	public Long getRunTimeMS() {
		return runTimeMS;
	}

	public void setRunTimeMS(Long runTimeMS) {
		this.runTimeMS = runTimeMS;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((errorMessage == null) ? 0 : errorMessage.hashCode());
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((percentComplete == null) ? 0 : percentComplete.hashCode());
		result = prime * result
				+ ((startedBy == null) ? 0 : startedBy.hashCode());
		result = prime * result
				+ ((startedOn == null) ? 0 : startedOn.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
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
		DBOUploadDaemonStatus other = (DBOUploadDaemonStatus) obj;
		if (errorMessage == null) {
			if (other.errorMessage != null)
				return false;
		} else if (!errorMessage.equals(other.errorMessage))
			return false;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (percentComplete == null) {
			if (other.percentComplete != null)
				return false;
		} else if (!percentComplete.equals(other.percentComplete))
			return false;
		if (startedBy == null) {
			if (other.startedBy != null)
				return false;
		} else if (!startedBy.equals(other.startedBy))
			return false;
		if (startedOn == null) {
			if (other.startedOn != null)
				return false;
		} else if (!startedOn.equals(other.startedOn))
			return false;
		if (state != other.state)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOUploadDaemonStatus [id=" + id + ", state=" + state
				+ ", startedBy=" + startedBy + ", startedOn=" + startedOn
				+ ", percentComplete=" + percentComplete + ", errorMessage="
				+ errorMessage + ", fileHandleId=" + fileHandleId
				+ ", runTimeMS=" + runTimeMS + "]";
	}

}
