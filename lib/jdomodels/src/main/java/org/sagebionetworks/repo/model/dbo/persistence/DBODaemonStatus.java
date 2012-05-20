package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BACKUP_ERORR_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BACKUP_ERROR_DETAILS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BACKUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BACKUP_PROGRESS_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BACKUP_PROGRESS_TOTAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BACKUP_RUNTIME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BACKUP_STARTED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BACKUP_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BACKUP_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BACKUP_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BACKUP_URL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BAKUP_PROGRESS_CURRENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DAEMON_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BACKUP_STATUS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.sagebionetworks.repo.model.dbo.AutoIncrementDatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBODaemonStatus implements AutoIncrementDatabaseObject<DBODaemonStatus> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_BACKUP_ID, true),
			new FieldColumn("status", COL_BACKUP_STATUS),
			new FieldColumn("type", COL_BACKUP_TYPE),
			new FieldColumn("startedBy", COL_BACKUP_STARTED_BY),
			new FieldColumn("startedOn", COL_BACKUP_STARTED_ON),
			new FieldColumn("progresssMessage", COL_BACKUP_PROGRESS_MESSAGE),
			new FieldColumn("progresssCurrent", COL_BAKUP_PROGRESS_CURRENT),
			new FieldColumn("progresssTotal", COL_BACKUP_PROGRESS_TOTAL),
			new FieldColumn("errorMessage", COL_BACKUP_ERORR_MESSAGE),
			new FieldColumn("errorDetails", COL_BACKUP_ERROR_DETAILS),
			new FieldColumn("backupUrl", COL_BACKUP_URL),
			new FieldColumn("totalRunTimeMS", COL_BACKUP_RUNTIME), };

	@Override
	public TableMapping<DBODaemonStatus> getTableMapping() {
		return new TableMapping<DBODaemonStatus>() {

			@Override
			public DBODaemonStatus mapRow(ResultSet rs, int index)
					throws SQLException {
				DBODaemonStatus status = new DBODaemonStatus();
				status.setId(rs.getLong(COL_BACKUP_ID));
				status.setStatus(rs.getString(COL_BACKUP_STATUS));
				status.setType(rs.getString(COL_BACKUP_TYPE));
				status.setStartedBy(rs.getLong(COL_BACKUP_STARTED_BY));
				status.setStartedOn(rs.getLong(COL_BACKUP_STARTED_ON));
				status.setProgresssMessage(rs.getString(COL_BACKUP_PROGRESS_MESSAGE));
				status.setProgresssCurrent(rs.getLong(COL_BAKUP_PROGRESS_CURRENT));
				status.setProgresssTotal(rs.getLong(COL_BACKUP_PROGRESS_TOTAL));
				status.setErrorMessage(rs.getString(COL_BACKUP_ERORR_MESSAGE));
				if(rs.wasNull()){
					status.setErrorMessage(null);
				}
				java.sql.Blob blob = rs.getBlob(COL_BACKUP_ERROR_DETAILS);
				if(blob != null){
					status.setErrorDetails(blob.getBytes(1, (int) blob.length()));
				}
				status.setBackupUrl(rs.getString(COL_BACKUP_URL));
				if(rs.wasNull()){
					status.setBackupUrl(null);
				}
				status.setTotalRunTimeMS(rs.getLong(COL_BACKUP_RUNTIME));
				return status;
			}

			@Override
			public String getTableName() {
				return TABLE_BACKUP_STATUS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DAEMON_STATUS;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBODaemonStatus> getDBOClass() {
				return DBODaemonStatus.class;
			}
		};
	}

	private Long id;
	private String status;
	private String type;
	private Long startedBy;
	private Long startedOn;
	private String progresssMessage;
	private Long progresssCurrent;
	private Long progresssTotal;
	private String errorMessage;
	private byte[] errorDetails;
	private String backupUrl;
	private Long totalRunTimeMS;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public String getProgresssMessage() {
		return progresssMessage;
	}

	public void setProgresssMessage(String progresssMessage) {
		this.progresssMessage = progresssMessage;
	}

	public Long getProgresssCurrent() {
		return progresssCurrent;
	}

	public void setProgresssCurrent(Long progresssCurrent) {
		this.progresssCurrent = progresssCurrent;
	}

	public Long getProgresssTotal() {
		return progresssTotal;
	}

	public void setProgresssTotal(Long progresssTotal) {
		this.progresssTotal = progresssTotal;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public byte[] getErrorDetails() {
		return errorDetails;
	}

	public void setErrorDetails(byte[] errorDetails) {
		this.errorDetails = errorDetails;
	}

	public String getBackupUrl() {
		return backupUrl;
	}

	public void setBackupUrl(String backupUrl) {
		this.backupUrl = backupUrl;
	}

	public Long getTotalRunTimeMS() {
		return totalRunTimeMS;
	}

	public void setTotalRunTimeMS(Long totalRunTimeMS) {
		this.totalRunTimeMS = totalRunTimeMS;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((backupUrl == null) ? 0 : backupUrl.hashCode());
		result = prime * result + Arrays.hashCode(errorDetails);
		result = prime * result
				+ ((errorMessage == null) ? 0 : errorMessage.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime
				* result
				+ ((progresssCurrent == null) ? 0 : progresssCurrent.hashCode());
		result = prime
				* result
				+ ((progresssMessage == null) ? 0 : progresssMessage.hashCode());
		result = prime * result
				+ ((progresssTotal == null) ? 0 : progresssTotal.hashCode());
		result = prime * result
				+ ((startedBy == null) ? 0 : startedBy.hashCode());
		result = prime * result
				+ ((startedOn == null) ? 0 : startedOn.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result
				+ ((totalRunTimeMS == null) ? 0 : totalRunTimeMS.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		DBODaemonStatus other = (DBODaemonStatus) obj;
		if (backupUrl == null) {
			if (other.backupUrl != null)
				return false;
		} else if (!backupUrl.equals(other.backupUrl))
			return false;
		if (!Arrays.equals(errorDetails, other.errorDetails))
			return false;
		if (errorMessage == null) {
			if (other.errorMessage != null)
				return false;
		} else if (!errorMessage.equals(other.errorMessage))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (progresssCurrent == null) {
			if (other.progresssCurrent != null)
				return false;
		} else if (!progresssCurrent.equals(other.progresssCurrent))
			return false;
		if (progresssMessage == null) {
			if (other.progresssMessage != null)
				return false;
		} else if (!progresssMessage.equals(other.progresssMessage))
			return false;
		if (progresssTotal == null) {
			if (other.progresssTotal != null)
				return false;
		} else if (!progresssTotal.equals(other.progresssTotal))
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
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (totalRunTimeMS == null) {
			if (other.totalRunTimeMS != null)
				return false;
		} else if (!totalRunTimeMS.equals(other.totalRunTimeMS))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBODaemonStatus [id=" + id + ", status=" + status + ", type="
				+ type + ", startedBy=" + startedBy + ", startedOn="
				+ startedOn + ", progresssMessage=" + progresssMessage
				+ ", progresssCurrent=" + progresssCurrent
				+ ", progresssTotal=" + progresssTotal + ", errorMessage="
				+ errorMessage + ", errorDetails="
				+ Arrays.toString(errorDetails) + ", backupUrl=" + backupUrl
				+ ", totalRunTimeMS=" + totalRunTimeMS + "]";
	}

}
