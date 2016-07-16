package org.sagebionetworks.repo.model.dao.asynch;

/**
 * Progress data for asynchronous jobs.
 *
 */
public class AsynchProgress {
	
	Long progressCurrent;
	Long progressTotal;
	String message;
	
	public AsynchProgress(){
	}
	
	public AsynchProgress(Long progressCurrent, Long progressTotal,
			String message) {
		super();
		this.progressCurrent = progressCurrent;
		this.progressTotal = progressTotal;
		this.message = message;
	}
	/**
	 * Assume percent complete = progressCurrent/progressTotal.
	 * @return
	 */
	public Long getProgressCurrent() {
		return progressCurrent;
	}
	/**
	 * Assume percent complete = progressCurrent/progressTotal.
	 * @param progressCurrent
	 */
	public void setProgressCurrent(Long progressCurrent) {
		this.progressCurrent = progressCurrent;
	}
	/**
	 * Assume percent complete = progressCurrent/progressTotal.
	 * @return
	 */
	public Long getProgressTotal() {
		return progressTotal;
	}
	/**
	 * Assume percent complete = progressCurrent/progressTotal.
	 * @param progressTotal
	 */
	public void setProgressTotal(Long progressTotal) {
		this.progressTotal = progressTotal;
	}
	/**
	 * The progress message to display to the caller.
	 * @return
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * The progress message to display to the caller.
	 * @param message
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result
				+ ((progressCurrent == null) ? 0 : progressCurrent.hashCode());
		result = prime * result
				+ ((progressTotal == null) ? 0 : progressTotal.hashCode());
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
		AsynchProgress other = (AsynchProgress) obj;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (progressCurrent == null) {
			if (other.progressCurrent != null)
				return false;
		} else if (!progressCurrent.equals(other.progressCurrent))
			return false;
		if (progressTotal == null) {
			if (other.progressTotal != null)
				return false;
		} else if (!progressTotal.equals(other.progressTotal))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "AsynchProgress [progressCurrent=" + progressCurrent
				+ ", progressTotal=" + progressTotal + ", message=" + message
				+ "]";
	}
	
}
