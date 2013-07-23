package org.sagebionetworks.javadoc.velocity;

import org.apache.velocity.context.Context;

/**
 * Abstraction for getting new Velocity context objects.
 * 
 * @author John
 *
 */
public interface ContextFactory {

	/**
	 * Create a new Velocity context.
	 * 
	 * @return
	 */
	public Context createNewContext();
}
