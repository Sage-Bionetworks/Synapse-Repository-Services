package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
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
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STATUS;

import java.util.Arrays;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
/**
 * DBO to track a table's status.
 * 
 * This is not a migrate-able table as each stack has its own table status.
 * 
 * @author John
 *
 */
@Table(name = TABLE_STATUS)
public class DBOTableStatus implements DatabaseObject<DBOTableStatus>{
	
	private static TableMapping<DBOTableStatus> tableMapping = AutoTableMapping.create(DBOTableStatus.class);

	@Field(name = COL_TABLE_STATUS_ID, nullable = false, primary=true)
	private Long tableId;
	
	@Field(name = COL_TABLE_STATUS_STATE, nullable = false)
	private TableStateEnum state;
	
	@Field(name = COL_TABLE_STATUS_RESET_TOKEN, nullable = false, fixedchar=200)
	private String resetToken;
	
	@Field(name = COL_TABLE_LAST_TABLE_CHANGE_ETAG, nullable = true, fixedchar=200)
	private String lastTableChangeEtag;
	
	@Field(name = COL_TABLE_STATUS_STARTED_ON, nullable = false)
	private Long startedOn;
	
	@Field(name = COL_TABLE_STATUS_CHANGE_ON, nullable = false)
	private Long changedOn;
	
	@Field(name = COL_TABLE_STATUS_PROGRESS_MESSAGE, varchar= 1000)
	private String progresssMessage;
	
	@Field(name = COL_TABLE_STATUS_PROGRESS_CURRENT)
	private Long progresssCurrent;
	
	@Field(name = COL_TABLE_STATUS_PROGRESS_TOTAL)
	private Long progresssTotal;
	
	@Field(name = COL_TABLE_STATUS_ERROR_MESSAGE, varchar= 1000)
	private String errorMessage;
	
	@Field(name = COL_TABLE_STATUS_ERROR_DETAILS, blob = "mediumblob")
	private byte[] errorDetails;
	
	@Field(name = COL_TABLE_STATUS_RUNTIME_MS)
	private Long totalRunTimeMS;
	
	@Override
	public TableMapping<DBOTableStatus> getTableMapping() {
		return tableMapping;
	}

	public Long getStartedOn() {
		return startedOn;
	}

	public void setStartedOn(Long startedOn) {
		this.startedOn = startedOn;
	}

	public TableStateEnum getState() {
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


	public void setState(TableStateEnum state) {
		this.state = state;
	}

	public Long getChangedOn() {
		return changedOn;
	}

	public void setChangedOn(Long changedOn) {
		this.changedOn = changedOn;
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

	public Long getTotalRunTimeMS() {
		return totalRunTimeMS;
	}

	public void setTotalRunTimeMS(Long totalRunTimeMS) {
		this.totalRunTimeMS = totalRunTimeMS;
	}

	public static void setTableMapping(TableMapping<DBOTableStatus> tableMapping) {
		DBOTableStatus.tableMapping = tableMapping;
	}

	public Long getTableId() {
		return tableId;
	}

	public void setTableId(Long tableId) {
		this.tableId = tableId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((changedOn == null) ? 0 : changedOn.hashCode());
		result = prime * result + Arrays.hashCode(errorDetails);
		result = prime * result
				+ ((errorMessage == null) ? 0 : errorMessage.hashCode());
		result = prime
				* result
				+ ((lastTableChangeEtag == null) ? 0 : lastTableChangeEtag
						.hashCode());
		result = prime
				* result
				+ ((progresssCurrent == null) ? 0 : progresssCurrent.hashCode());
		result = prime
				* result
				+ ((progresssMessage == null) ? 0 : progresssMessage.hashCode());
		result = prime * result
				+ ((progresssTotal == null) ? 0 : progresssTotal.hashCode());
		result = prime * result
				+ ((resetToken == null) ? 0 : resetToken.hashCode());
		result = prime * result
				+ ((startedOn == null) ? 0 : startedOn.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result + ((tableId == null) ? 0 : tableId.hashCode());
		result = prime * result
				+ ((totalRunTimeMS == null) ? 0 : totalRunTimeMS.hashCode());
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
		if (state != other.state)
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
		return true;
	}

	@Override
	public String toString() {
		return "DBOTableStatus [tableId=" + tableId + ", state=" + state
				+ ", resetToken=" + resetToken + ", lastTableChangeEtag="
				+ lastTableChangeEtag + ", startedOn=" + startedOn
				+ ", changedOn=" + changedOn + ", progresssMessage="
				+ progresssMessage + ", progresssCurrent=" + progresssCurrent
				+ ", progresssTotal=" + progresssTotal + ", errorMessage="
				+ errorMessage + ", errorDetails="
				+ Arrays.toString(errorDetails) + ", totalRunTimeMS="
				+ totalRunTimeMS + "]";
	}
	
}
