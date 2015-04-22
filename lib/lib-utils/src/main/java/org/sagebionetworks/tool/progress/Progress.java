package org.sagebionetworks.tool.progress;

public interface Progress {
	
	/**
	 * The current progress towards the total.
	 * @return
	 */
	public long getCurrent();
	
	/**
	 * The total progress to be made.
	 * @return
	 */
	public long getTotal();
	
	/**
	 * How much time has elapsed (MS) since created?
	 * @return
	 */
	public long getElapseTimeMS();
	
	/**
	 * Get the current status.
	 * @return
	 */
	public StatusData getCurrentStatus();
	

}
