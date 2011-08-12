package org.sagebionetworks.repo.manager.backup;

/**
 * Used to track how much progress has been made. The total count indicates how
 * much work there is to be done. The current index indicates how much progress
 * has been made.
 * For example: If totalCount = 100; and currentIndex = 50; 
 * then the work is 50% complete.
 * 
 * @author John
 * 
 */
public class Progress {

	private volatile long totalCount=0;
	private volatile long currentIndex =0;
	private volatile String message;
	private volatile boolean terminate = false;

	/**
	 * The total count indicates how much work there is to be done. The current
	 * index indicates how much progress has been made. For example: If
	 * totalCount = 100; and currentIndex = 50; then work is 50% complete.
	 * 
	 * @return
	 */
	public long getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}

	public long getCurrentIndex() {
		return currentIndex;
	}

	public void setCurrentIndex(long currentIndex) {
		this.currentIndex = currentIndex;
	}

	/**
	 * Increment the current index by one.
	 */
	public void incrementProgress() {
		currentIndex++;
	}
	
	public void incrementProgressBy(long size) {
		currentIndex += size;
	}

	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Should the process terminate?
	 * @return
	 */
	public boolean shouldTerminate() {
		return terminate;
	}

	/**
	 * When this is set to true, the process should terminate.
	 * @param terminate
	 */
	public void setTerminate(boolean terminate) {
		this.terminate = terminate;
	}

	public String toString(){
		double percent = ((double)currentIndex/(double)totalCount)*100.0;
		return 	String.format("%1$-30s %2$10d/%3$-10d %4$8.2f %%", message, currentIndex, totalCount, percent);
	}

}
