package org.sagebionetworks.repo.model.migration;

import java.util.Iterator;

/**
 * 
 * @author John
 *
 * @param <T>
 */
public interface ResetableIterator <T> extends Iterator<T> {
	
	/**
	 * When reset the pointer must return to the first element in the iteration.
	 */
	public void reset();
}
