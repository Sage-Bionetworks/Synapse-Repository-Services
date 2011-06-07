package org.sagebionetworks.workflow.activity;

/**
 * Simplistic implementation of the observer pattern
 * 
 * @author deflaux
 *
 * @param <T>
 */
public interface SimpleObserver<T> {
	/**
	 * @param object
	 * @throws Exception
	 */
	public void update(T object) throws Exception;
}
