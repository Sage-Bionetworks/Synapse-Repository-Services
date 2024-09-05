package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.model.LogEntry;

/**
 * Abstraction for a AsynchronousJobServices
 * 
 * @author marcel-blonk
 *
 */
public interface LogService {

	/**
	 * Log an entry
	 */
	void log(LogEntry logEntry, String userAgent);
}
