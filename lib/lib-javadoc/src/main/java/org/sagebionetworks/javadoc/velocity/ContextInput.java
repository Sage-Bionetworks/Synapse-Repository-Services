package org.sagebionetworks.javadoc.velocity;

import java.util.Optional;

import jdk.javadoc.doclet.DocletEnvironment;

public interface ContextInput {

	/**
	 * Provides access to a new Velocity Context.
	 * 
	 * @return
	 */
	ContextFactory getContextFactory();

	/**
	 * Provides access to the DocletEnvironment.
	 * 
	 * @return
	 */
	DocletEnvironment getDocletEnvironment();

	/**
	 * The name of the controller class provided as an input parameter to this run.
	 * 
	 * @return
	 */
	Optional<String> getAuthControllerName();
}
