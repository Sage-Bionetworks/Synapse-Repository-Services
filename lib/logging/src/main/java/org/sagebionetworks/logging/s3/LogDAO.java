package org.sagebionetworks.logging.s3;

import java.io.File;
import java.io.IOException;

import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

/**
 * An abstraction for the log Data Access Object.
 * 
 * @author jmhill
 *
 */
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
	 * Get a reader that can be used to read one log entry at a time.
	 * @param key
	 * @return
	 * @throws IOException 
	 */
	public LogReader getLogFileReader(String key) throws IOException;
	
	/**
	 * Download a log to the passed destiantion file.
	 * @param key
	 * @return
	 * @throws IOException
	 */
	public GetObjectResponse downloadLogFile(String key, File destination) throws IOException;

	/**
	 * Delete all logs for this Stack Instances.
	 */
	public void deleteAllStackInstanceLogs();
	
	/**
	 * List a page of the log files for this stack.
	 *
	 * @param continuationToken The token of the page to list, null for the first page. Subsequent
	 *                          pages are read with the previous page's
	 *                          {@link ListObjectsV2Response#nextContinuationToken()}, which is null
	 *                          once the last page has been listed.
	 */
	public ListObjectsV2Response listAllStackInstanceLogs(String continuationToken);
	
	/**
	 * Scans all log files in S3 to find a log contains the passed UUID.
	 * 
	 * @param uuidTofind
	 * @return The key of the first log file that contains the passed UUID. If no log is found then null will be returned.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public String findLogContainingUUID(String uuidTofind) throws InterruptedException, IOException;
}
