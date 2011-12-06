package org.sagebionetworks.repo.model;

import java.util.Date;

/**
 * Object used to track the status of a backup or restore.
 * 
 * @author jmhill
 * 
 */
public class BackupRestoreStatus {

	/**
	 * The possible status states.
	 *
	 */
	public enum STATUS {
		STARTED, IN_PROGRESSS, FAILED, COMPLETED, IN_QUEUE;
	}
	
	/**
	 * The possible status types.
	 */
	public enum TYPE {
		BACKUP, RESTORE
	}

	private String id;
	private String status = STATUS.STARTED.name();
	private String type;
	private String startedBy;
	private Date startedOn;
	private String progresssMessage;
	private long progresssCurrent;
	private long progresssTotal;
	private String errorMessage;
	private String errorDetails;
	private String backupUrl;
	private long totalTimeMS;

	/**
	 * The id assigned to the backup/restore processes.
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * The id assigned to the backup/restore processes.
	 * 
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * The status of the backup/restore. Must be a value from the BackupRestoreStatus.STATUS
	 * enumeration.
	 * 
	 * @return
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * The status of the backup/restore. Must be a value from the BackupRestoreStatus.STATUS
	 * enumeration.
	 * 
	 * @param status
	 */
	public void setStatus(String status) {
		if (status == null)
			throw new IllegalArgumentException("Status cannot be null");
		// Validate the status
		try {
			STATUS.valueOf(status);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unknown status type: " + status);
		}
		this.status = status;
	}
	
	/**
	 * Is this a backup or restore? Values must be from BackupRestoreStatus.TYPE
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * Is this a backup or restore? Values must be from BackupRestoreStatus.TYPE
	 * @return
	 */
	public void setType(String type) {
		if (type == null)
			throw new IllegalArgumentException("Type cannot be null");
		// Validate the status
		try {
			TYPE.valueOf(type);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unknown type: " + type);
		}
		this.type = type;
	}

	/**
	 * The current message of the progress tracker.
	 * 
	 * @return
	 */
	public String getProgresssMessage() {
		return progresssMessage;
	}

	/**
	 * The current message of the progress tracker.
	 * 
	 * @param progresssMessage
	 */
	public void setProgresssMessage(String progresssMessage) {
		this.progresssMessage = progresssMessage;
	}

	/**
	 * The progress current value indicates how much progress has been made. For
	 * example: If progressTotal = 100; and progressCurrent = 50; then the work
	 * is 50% complete.
	 * 
	 * @return
	 */
	public long getProgresssCurrent() {
		return progresssCurrent;
	}

	/**
	 * The progress current value indicates how much progress has been made. For
	 * example: If progressTotal = 100; and progressCurrent = 50; then the work
	 * is 50% complete.
	 * 
	 * @param progresssCurrent
	 */
	public void setProgresssCurrent(long progresssCurrent) {
		this.progresssCurrent = progresssCurrent;
	}

	/**
	 * The progress total indicates the total amount of work to complete. For
	 * example: If progressTotal = 100; and progressCurrent = 50; then the work
	 * is 50% complete.
	 * 
	 * @return
	 */
	public long getProgresssTotal() {
		return progresssTotal;
	}

	/**
	 * The progress total indicates the total amount of work to complete. For
	 * example: If progressTotal = 100; and progressCurrent = 50; then the work
	 * is 50% complete.
	 * @param progresssTotal
	 */
	public void setProgresssTotal(long progresssTotal) {
		this.progresssTotal = progresssTotal;
	}

	/**
	 * This is a one line error message.
	 * 
	 * @return
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * This is a one line error message.
	 * 
	 * @param errorMessage
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/**
	 * This is the full stack trace of the error.
	 * 
	 * @return
	 */
	public String getErrorDetails() {
		return errorDetails;
	}

	/**
	 * This is the full stack trace of the error.
	 * 
	 * @param errorDetails
	 */
	public void setErrorDetails(String errorDetails) {
		this.errorDetails = errorDetails;
	}

	/**
	 * After a backup is completed this URL will point to the backup file.
	 * For a restore, this URL will point to the backup file being used to 
	 * drive the system restoration.
	 * @return
	 */
	public String getBackupUrl() {
		return backupUrl;
	}

	/**
	 * After a backup is completed this URL will point to the backup file.
	 * For a restore, this URL will point to the backup file being used to 
	 * drive the system restoration.
	 * @param backupUrl
	 */
	public void setBackupUrl(String backupUrl) {
		this.backupUrl = backupUrl;
	}

	/**
	 * The total amount of time (MS) spent on this backup/restore
	 * @return
	 */
	public long getTotalTimeMS() {
		return totalTimeMS;
	}

	/**
	 * The total amount of time (MS) spent on this backup/restore
	 * @param totalTimeMS
	 */
	public void setTotalTimeMS(long totalTimeMS) {
		this.totalTimeMS = totalTimeMS;
	}

	/**
	 * The user that started the backup/restore
	 * @return
	 */
	public String getStartedBy() {
		return startedBy;
	}

	/**
	 * The user that started the backup/restore
	 * @param startedBy
	 */
	public void setStartedBy(String startedBy) {
		this.startedBy = startedBy;
	}

	/**
	 * When was the backup/restore started.
	 * @return
	 */
	public Date getStartedOn() {
		return startedOn;
	}

	/**
	 * When was the backup/restore started.
	 * @param startedOn
	 */
	public void setStartedOn(Date startedOn) {
		this.startedOn = startedOn;
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
		result = prime * result
				+ (int) (progresssCurrent ^ (progresssCurrent >>> 32));
		result = prime
				* result
				+ ((progresssMessage == null) ? 0 : progresssMessage.hashCode());
		result = prime * result
				+ (int) (progresssTotal ^ (progresssTotal >>> 32));
		result = prime * result
				+ ((startedBy == null) ? 0 : startedBy.hashCode());
		result = prime * result
				+ ((startedOn == null) ? 0 : startedOn.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + (int) (totalTimeMS ^ (totalTimeMS >>> 32));
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
		BackupRestoreStatus other = (BackupRestoreStatus) obj;
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
		if (progresssCurrent != other.progresssCurrent)
			return false;
		if (progresssMessage == null) {
			if (other.progresssMessage != null)
				return false;
		} else if (!progresssMessage.equals(other.progresssMessage))
			return false;
		if (progresssTotal != other.progresssTotal)
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
		if (totalTimeMS != other.totalTimeMS)
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
		return "BackupRestoreStatus [id=" + id + ", status=" + status
				+ ", type=" + type + ", startedBy=" + startedBy
				+ ", startedOn=" + startedOn + ", progresssMessage="
				+ progresssMessage + ", progresssCurrent=" + progresssCurrent
				+ ", progresssTotal=" + progresssTotal + ", errorMessage="
				+ errorMessage + ", errorDetails=" + errorDetails
				+ ", backupUrl=" + backupUrl + ", totalTimeMS=" + totalTimeMS
				+ "]";
	}
	
	/**
	 * Print the status as a formated string.
	 * @return
	 */
	public String printStatus(){
		double percent = 0.0;
		if(progresssTotal > 0){
			percent = ((double)progresssCurrent/(double)progresssTotal)*100.0;
		}
		return 	String.format("%5$-10s %2$10d/%3$-10d %4$8.2f %% Message: %1$-30s", progresssMessage, progresssCurrent, progresssTotal, percent, status);
	}

}
