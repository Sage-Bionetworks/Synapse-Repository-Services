package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.LogEntry;

/**
 * Abstraction for a AsynchronousJobServices
 * 
 * @author John
 *
 */
public interface LogService {

	/**
	 * Log an entry
	 */
	void log(LogEntry logEntry, String userAgent);
}
