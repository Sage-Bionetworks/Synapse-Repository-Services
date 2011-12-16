package org.sagebionetworks.tool.migration.Progress;

import org.sagebionetworks.tool.migration.Constants;

/**
 * Keeps track of of basic progress.
 * 
 * @author jmhill
 *
 */
public class BasicProgress implements Progress{
	

	/**
	 * These are volatile because they are updated from one thread and read by another.
	 */
	private volatile long current = 0;
	private volatile long total = 0;
	private volatile long startNano = System.nanoTime();
	
	public long getCurrent() {
		return current;
	}
	public void setCurrent(long current) {
		this.current = current;
	}
	public long getTotal() {
		return total;
	}
	public void setTotal(long total) {
		this.total = total;
	}
	@Override
	public long getElapseTimeMS() {
		return (System.nanoTime()-startNano)/Constants.NANO_SECS_PER_MIL_SEC;
	}
	@Override
	public StatusData getCurrentStatus() {
		return new StatusData(this);
	}

}
