package org.sagebionetworks.tool.progress;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents the sum of multiple progress objects.
 * 
 * @author John
 * 
 */
public class AggregateProgress implements Progress {

	public static long NANO_SECS_PER_MIL_SEC = 1000000;
	
	/**
	 * This object is an aggregation of these parts.
	 */
	private List<Progress> parts;

	private volatile long startNano = System.nanoTime();

	/**
	 * Create a new progress object and start the timer.
	 */
	public AggregateProgress() {
		parts = Collections.synchronizedList(new LinkedList<Progress>());
	}

	/**
	 * Add a new part to this progress.
	 * 
	 * @param toAdd
	 */
	public void addProgresss(Progress toAdd) {
		this.parts.add(toAdd);
	}

	@Override
	public long getCurrent() {
		// The current is always the sum of the parts.
		long current = 0;
		synchronized (parts) {
			for (Progress part : parts) {
				current += part.getCurrent();
			}
		}
		return current;
	}

	@Override
	public long getTotal() {
		// The total is always the sum of the parts.
		long total = 0;
		synchronized (parts) {
			for (Progress part : parts) {
				total += part.getTotal();
			}
		}
		return total;
	}

	@Override
	public long getElapseTimeMS() {
		return (System.nanoTime() - startNano)/ NANO_SECS_PER_MIL_SEC;
	}

	@Override
	public StatusData getCurrentStatus() {
		return new StatusData(this);
	}

}
