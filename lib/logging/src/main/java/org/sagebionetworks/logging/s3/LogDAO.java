package org.sagebionetworks.logging.s3;

import java.io.File;

public interface LogDAO {

	/**
	 * Save a log file to S3.
	 * 
	 * @param toSave
	 * @param timestamp
	 * @return
	 */
	public String saveLogFile(File toSave, long timestamp);
	
	/**
	 * Delete a log file using its key
	 * @param key
	 */
	public void deleteLogFile(String key);

	/**
	 * Delete all logs for this Stack Instances.
	 */
	public void deleteAllStackInstanceLogs();
}
