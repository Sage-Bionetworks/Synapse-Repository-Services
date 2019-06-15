package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_LAST_TABLE_CHANGE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_CHANGE_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_ERROR_DETAILS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_ERROR_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_PROGRESS_CURRENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_PROGRESS_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_PROGRESS_TOTAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_RESET_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_RUNTIME_MS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_STATUS_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_TABLE_STATUE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STATUS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
/**
 * DBO to track a table's status.
 * 
 * This is not a migrate-able table as each stack has its own table status.
 * 
 * @author John
 *
 */
public class DBOTableStatus implements DatabaseObject<DBOTableStatus>{
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("tableId", COL_TABLE_STATUS_ID).withIsPrimaryKey(true),
			new FieldColumn("version", COL_TABLE_STATUS_VERSION).withIsPrimaryKey(true),
			new FieldColumn("state", COL_TABLE_STATUS_STATE),
			new FieldColumn("resetToken", COL_TABLE_STATUS_RESET_TOKEN),
			new FieldColumn("lastTableChangeEtag", COL_TABLE_LAST_TABLE_CHANGE_ETAG),
			new FieldColumn("startedOn", COL_TABLE_STATUS_STARTED_ON),
			new FieldColumn("changedOn", COL_TABLE_STATUS_CHANGE_ON),
			new FieldColumn("progressMessage", COL_TABLE_STATUS_PROGRESS_MESSAGE),
			new FieldColumn("progressCurrent", COL_TABLE_STATUS_PROGRESS_CURRENT),
			new FieldColumn("progressTotal", COL_TABLE_STATUS_PROGRESS_TOTAL),
			new FieldColumn("errorMessage", COL_TABLE_STATUS_ERROR_MESSAGE),
			new FieldColumn("errorDetails", COL_TABLE_STATUS_ERROR_DETAILS),
			new FieldColumn("totalRunTimeMS", COL_TABLE_STATUS_RUNTIME_MS)
	};

	private Long tableId;
	private Long version;
	private String state;
	private String resetToken;
	private String lastTableChangeEtag;
	private Long startedOn;
	private Long changedOn;
	private String progressMessage;
	private Long progressCurrent;
	private Long progressTotal;
	private String errorMessage;
	private byte[] errorDetails;
	private Long totalRunTimeMS;
	
	@Override
	public TableMapping<DBOTableStatus> getTableMapping() {
		return new TableMapping<DBOTableStatus>(){

			@Override
			public DBOTableStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOTableStatus dbo = new DBOTableStatus();
				dbo.setTableId(rs.getLong(COL_TABLE_STATUS_ID));
				dbo.setVersion(rs.getLong(COL_TABLE_STATUS_VERSION));
				dbo.setState(rs.getString(COL_TABLE_STATUS_STATE));
				dbo.setResetToken(rs.getString(COL_TABLE_STATUS_RESET_TOKEN));
				dbo.setLastTableChangeEtag(rs.getString(COL_TABLE_LAST_TABLE_CHANGE_ETAG));
				dbo.setStartedOn(rs.getLong(COL_TABLE_STATUS_STARTED_ON));
				dbo.setChangedOn(rs.getLong(COL_TABLE_STATUS_CHANGE_ON));
				dbo.setProgressMessage(rs.getString(COL_TABLE_STATUS_PROGRESS_MESSAGE));
				dbo.setProgressCurrent(rs.getLong(COL_TABLE_STATUS_PROGRESS_CURRENT));
				if(rs.wasNull()) {
					dbo.setProgressCurrent(null);
				}
				dbo.setProgressTotal(rs.getLong(COL_TABLE_STATUS_PROGRESS_TOTAL));
				if(rs.wasNull()) {
					dbo.setProgressTotal(null);
				}
				dbo.setErrorMessage(rs.getString(COL_TABLE_STATUS_ERROR_MESSAGE));
				dbo.setErrorDetails(rs.getBytes(COL_TABLE_STATUS_ERROR_DETAILS));
				dbo.setTotalRunTimeMS(rs.getLong(COL_TABLE_STATUS_RUNTIME_MS));
				if(rs.wasNull()) {
					dbo.setTotalRunTimeMS(null);
				}
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_STATUS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_TABLE_STATUE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOTableStatus> getDBOClass() {
				return DBOTableStatus.class;
			}};
	}

	public Long getStartedOn() {
		return startedOn;
	}

	public void setStartedOn(Long startedOn) {
		this.startedOn = startedOn;
	}

	public String getState() {
		return state;
	}

	public String getResetToken() {
		return resetToken;
	}

	public String getLastTableChangeEtag() {
		return lastTableChangeEtag;
	}

	public void setLastTableChangeEtag(String lastTableChangeEtag) {
		this.lastTableChangeEtag = lastTableChangeEtag;
	}

	public void setResetToken(String resetToken) {
		this.resetToken = resetToken;
	}


	public void setState(String state) {
		this.state = state;
	}

	public Long getChangedOn() {
		return changedOn;
	}

	public void setChangedOn(Long changedOn) {
		this.changedOn = changedOn;
	}

	public String getProgressMessage() {
		return progressMessage;
	}

	public void setProgressMessage(String progressMessage) {
		this.progressMessage = progressMessage;
	}

	public Long getProgressCurrent() {
		return progressCurrent;
	}

	public void setProgressCurrent(Long progressCurrent) {
		this.progressCurrent = progressCurrent;
	}

	public Long getProgressTotal() {
		return progressTotal;
	}

	public void setProgressTotal(Long progressTotal) {
		this.progressTotal = progressTotal;
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

	public Long getTotalRunTimeMS() {
		return totalRunTimeMS;
	}

	public void setTotalRunTimeMS(Long totalRunTimeMS) {
		this.totalRunTimeMS = totalRunTimeMS;
	}

	public Long getTableId() {
		return tableId;
	}

	public void setTableId(Long tableId) {
		this.tableId = tableId;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changedOn == null) ? 0 : changedOn.hashCode());
		result = prime * result + Arrays.hashCode(errorDetails);
		result = prime * result + ((errorMessage == null) ? 0 : errorMessage.hashCode());
		result = prime * result + ((lastTableChangeEtag == null) ? 0 : lastTableChangeEtag.hashCode());
		result = prime * result + ((progressCurrent == null) ? 0 : progressCurrent.hashCode());
		result = prime * result + ((progressMessage == null) ? 0 : progressMessage.hashCode());
		result = prime * result + ((progressTotal == null) ? 0 : progressTotal.hashCode());
		result = prime * result + ((resetToken == null) ? 0 : resetToken.hashCode());
		result = prime * result + ((startedOn == null) ? 0 : startedOn.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result + ((tableId == null) ? 0 : tableId.hashCode());
		result = prime * result + ((totalRunTimeMS == null) ? 0 : totalRunTimeMS.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		DBOTableStatus other = (DBOTableStatus) obj;
		if (changedOn == null) {
			if (other.changedOn != null)
				return false;
		} else if (!changedOn.equals(other.changedOn))
			return false;
		if (!Arrays.equals(errorDetails, other.errorDetails))
			return false;
		if (errorMessage == null) {
			if (other.errorMessage != null)
				return false;
		} else if (!errorMessage.equals(other.errorMessage))
			return false;
		if (lastTableChangeEtag == null) {
			if (other.lastTableChangeEtag != null)
				return false;
		} else if (!lastTableChangeEtag.equals(other.lastTableChangeEtag))
			return false;
		if (progressCurrent == null) {
			if (other.progressCurrent != null)
				return false;
		} else if (!progressCurrent.equals(other.progressCurrent))
			return false;
		if (progressMessage == null) {
			if (other.progressMessage != null)
				return false;
		} else if (!progressMessage.equals(other.progressMessage))
			return false;
		if (progressTotal == null) {
			if (other.progressTotal != null)
				return false;
		} else if (!progressTotal.equals(other.progressTotal))
			return false;
		if (resetToken == null) {
			if (other.resetToken != null)
				return false;
		} else if (!resetToken.equals(other.resetToken))
			return false;
		if (startedOn == null) {
			if (other.startedOn != null)
				return false;
		} else if (!startedOn.equals(other.startedOn))
			return false;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		if (tableId == null) {
			if (other.tableId != null)
				return false;
		} else if (!tableId.equals(other.tableId))
			return false;
		if (totalRunTimeMS == null) {
			if (other.totalRunTimeMS != null)
				return false;
		} else if (!totalRunTimeMS.equals(other.totalRunTimeMS))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOTableStatus [tableId=" + tableId + ", version=" + version + ", state=" + state + ", resetToken="
				+ resetToken + ", lastTableChangeEtag=" + lastTableChangeEtag + ", startedOn=" + startedOn
				+ ", changedOn=" + changedOn + ", progressMessage=" + progressMessage + ", progressCurrent="
				+ progressCurrent + ", progressTotal=" + progressTotal + ", errorMessage=" + errorMessage
				+ ", errorDetails=" + Arrays.toString(errorDetails) + ", totalRunTimeMS=" + totalRunTimeMS + "]";
	}
	
}
