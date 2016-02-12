package org.sagebionetworks.util;

/**
 * Simple parameterized callback.
 *
 * @param <T>
 */
public interface Callback<T> {

	/**
	 * Called for each value.
	 * 
	 * @param value
	 */
	public void invoke(T value);
}
