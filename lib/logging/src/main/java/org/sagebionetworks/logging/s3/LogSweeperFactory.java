package org.sagebionetworks.logging.s3;

import java.io.File;
import java.util.List;

/**
 * Abstraction for the log sweeper.
 * 
 * @author John
 *
 */
public interface LogSweeperFactory {

	/**
	 * This can be called from a timer.
	 * @return
	 */
	public List<String> sweepLogs();

	
	/**
	 * The directory where the log files are expected to be found
	 * @return
	 */
	public File getLoggingDirectory();
}
