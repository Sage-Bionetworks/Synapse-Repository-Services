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
	 * Get the directories that will be swept by this factory.
	 * @return
	 */
	List<File> getLogDirectories();

}
