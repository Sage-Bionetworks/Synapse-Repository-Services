package org.sagebionetworks;
import org.apache.logging.log4j.Logger;

public interface LoggerProvider {

	/**
	 * Get a logger for the given classname.
	 * @param className
	 * @return
	 */
	public Logger getLogger(String className);
}
