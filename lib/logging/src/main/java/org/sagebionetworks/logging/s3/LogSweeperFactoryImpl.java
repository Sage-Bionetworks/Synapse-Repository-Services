package org.sagebionetworks.logging.s3;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is the autowired bean that can be called from a timer.
 * 
 * @author John
 *
 */
public class LogSweeperFactoryImpl implements LogSweeperFactory {
	
	@Autowired
	private LogDAO logDAO;
	private File logDirectory;
	private long lockExpiresMs;

	/**
	 * Injected via Spring
	 * @param logDirectory
	 */
	public void setLogDirectory(String logDirectory) {
		this.logDirectory = new File(logDirectory);
	}

	/**
	 * Injected via Spring
	 * @param lockExpiresMs
	 */
	public void setLockExpiresMs(long lockExpiresMs) {
		this.lockExpiresMs = lockExpiresMs;
	}


	/**
	 * This method can be called from a timer.
	 */
	@Override
	public List<String> sweepLogs() {
		// Create the sweeper and run it.
		LogSweeper sweeper =  new LogSweeper(logDirectory, lockExpiresMs, logDAO);
		return sweeper.sweepAllfiles();
	}


	@Override
	public File getLoggingDirectory() {
		return this.logDirectory;
	}

}
