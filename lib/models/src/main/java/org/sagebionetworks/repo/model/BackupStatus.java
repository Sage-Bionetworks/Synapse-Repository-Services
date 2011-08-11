package org.sagebionetworks.repo.model;

/**
 * Object used to track the status of a backup.
 * 
 * @author jmhill
 *
 */
public class BackupStatus {
	
	public enum STATUS{
		STARTED,
		IN_PROGRESSS,
		FAILED,
		COMPLETED,
	}
	
	private String id;
	private String status = STATUS.STARTED.name();
	private String progresssMessage;
	private Long progresssCurrent;
	private Long progresssTotal;
	private String errorMessage;
	private String errorDetails;
	private String backupUrl;
	private Long totalTimeMS;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		if(status == null) throw new IllegalArgumentException("Status cannot be null");
		// Validate the status
		try{
			STATUS.valueOf(status);
		}catch(Exception e){
			throw new IllegalArgumentException("Unknown status type: "+status);
		}
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
	public String getErrorDetails() {
		return errorDetails;
	}
	public void setErrorDetails(String errorDetails) {
		this.errorDetails = errorDetails;
	}
	public String getBackupUrl() {
		return backupUrl;
	}
	public void setBackupUrl(String backupUrl) {
		this.backupUrl = backupUrl;
	}
	
	public Long getTotalTimeMS() {
		return totalTimeMS;
	}
	public void setTotalTimeMS(Long totalTimeMS) {
		this.totalTimeMS = totalTimeMS;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((backupUrl == null) ? 0 : backupUrl.hashCode());
		result = prime * result
				+ ((errorDetails == null) ? 0 : errorDetails.hashCode());
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
		result = prime * result + ((status == null) ? 0 : status.hashCode());
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
		BackupStatus other = (BackupStatus) obj;
		if (backupUrl == null) {
			if (other.backupUrl != null)
				return false;
		} else if (!backupUrl.equals(other.backupUrl))
			return false;
		if (errorDetails == null) {
			if (other.errorDetails != null)
				return false;
		} else if (!errorDetails.equals(other.errorDetails))
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
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		return true;
	}

}
