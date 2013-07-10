package org.sagebionetworks.javadoc.velocity;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;

/**
 * Simple implementation of a ContextFactory.
 * 
 * @author John
 *
 */
public class ContextFactoryImpl implements ContextFactory {

	@Override
	public Context createNewContext() {
		return new VelocityContext();
	}

}
