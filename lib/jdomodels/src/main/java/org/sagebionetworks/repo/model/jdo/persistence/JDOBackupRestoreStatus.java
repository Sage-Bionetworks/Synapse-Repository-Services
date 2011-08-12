package org.sagebionetworks.repo.model.jdo.persistence;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * JDO object for tracking the progress and results of a system backup.
 * @author John
 *
 */
@PersistenceCapable(detachable = "true", table=SqlConstants.TABLE_BACKUP_STATUS)
public class JDOBackupRestoreStatus {
	
	@Column(name=SqlConstants.COL_BACKUP_ID)
	@PrimaryKey
	private Long id;
	
	@Column(name=SqlConstants.COL_BACKUP_STATUS)
	@Persistent(nullValue = NullValue.EXCEPTION) // cannot be null
	private String status;
	
	@Column(name=SqlConstants.COL_BACKUP_TYPE)
	@Persistent(nullValue = NullValue.EXCEPTION) // cannot be null
	private String type;
	
	@Column(name=SqlConstants.COL_BACKUP_STARTED_BY)
	@Persistent(nullValue = NullValue.EXCEPTION) // cannot be null
	private String startedBy;
	
	@Column(name=SqlConstants.COL_BACKUP_STARTED_ON)
	@Persistent(nullValue = NullValue.EXCEPTION) // cannot be null
	private Long startedOn;
	
	@Column(name=SqlConstants.COL_BACKUP_PROGRESS_MESSAGE)
	@Persistent(nullValue = NullValue.EXCEPTION) // cannot be null
	private String progresssMessage;
	
	@Column(name=SqlConstants.COL_BAKUP_PROGRESS_CURRENT)
	@Persistent(nullValue = NullValue.EXCEPTION) // cannot be null
	private Long progresssCurrent;
	
	@Column(name=SqlConstants.COL_BACKUP_PROGRESS_TOTAL)
	@Persistent(nullValue = NullValue.EXCEPTION) // cannot be null
	private Long progresssTotal;
	
	@Column(name=SqlConstants.COL_BACKUP_ERORR_MESSAGE)
	private String errorMessage;
	
	@Column(name=SqlConstants.COL_BACKUP_ERROR_DETAILS)
	private byte[] errorDetails;
	
	@Column(name=SqlConstants.COL_BACKUP_URL)
	private String backupUrl;
	
	@Column(name=SqlConstants.COL_BACKUP_RUNTIME)
	@Persistent(nullValue = NullValue.EXCEPTION) // cannot be null
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStartedBy() {
		return startedBy;
	}

	public void setStartedBy(String startedBy) {
		this.startedBy = startedBy;
	}

	public Long getStartedOn() {
		return startedOn;
	}

	public void setStartedOn(Long startedOn) {
		this.startedOn = startedOn;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		JDOBackupRestoreStatus other = (JDOBackupRestoreStatus) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	

}
