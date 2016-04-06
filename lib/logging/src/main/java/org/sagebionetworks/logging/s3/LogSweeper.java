package org.sagebionetworks.logging.s3;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class sweeps file from a local directory to S3.
 * 
 * @author John
 * 
 */
public class LogSweeper {
	Logger log = LogManager.getLogger(LogSweeper.class);
	/**
	 * Only file that have not been modified for the past 10 seconds will be swept.
	 */
	public static final long MIN_FILE_AGE_MS = 1000*10;
	private File logDir;
	private long lockExpiresMs;
	private LogDAO logDAO;

	/**
	 * Create a new sweeper each time.
	 * 
	 * @param logDir
	 *            The directory that contains the log.gz files.
	 * @param lockExpiresMs
	 *            This sweeper only runs if it can acquire a lock on the logging
	 *            directory. When a lock already exists on the logging
	 *            directory, the lock will expire when this amount of time has
	 *            elapsed since the file was created.
	 * @param logDAO The log DAO.
	 */
	public LogSweeper(File logDir, long lockExpiresMs, LogDAO logDAO) {
		super();
		this.logDir = logDir;
		this.lockExpiresMs = lockExpiresMs;
		this.logDAO = logDAO;
	}

	/**
	 * Sweep all log files to S3.
	 * 
	 * @return
	 */
	public List<String> sweepAllfiles() {
		// try to get the lock
		File lock = new File(logDir, ".sweep.lock");
		try {
			if (lock.createNewFile()) {
				if(log.isTraceEnabled()){
					log.trace("Acquired directory lock: "+lock.getAbsolutePath());
				}
				// we are the lock holder.
				try {
					// we can now sweep.
					return sweepAllGZipFiles();
				} finally {
					// unconditionally release the lock.
					lock.delete();
				}
			} else {
				if(log.isTraceEnabled()){
					log.trace("Directory lock already held");
				}

				// If here then the lock file already exists. Has the lock
				// expired?
				long heldForMS = System.currentTimeMillis()	- lock.lastModified();
				if (heldForMS > lockExpiresMs) {
					log.debug("Directory lock already held but has expired so will be deleted.");
					// release the lock as it is expired.
					lock.delete();
				}
				// Any time there is a lock terminate.
				return new LinkedList<String>();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * There is where the actual log sweeping occurs.
	 * @throws IOException 
	 */
	private List<String> sweepAllGZipFiles() throws IOException {
		File[] toSweep = logDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				// We only care about GZIP files.
				return file.getName().toLowerCase().trim().endsWith(".gz");
			}
		});
		List<String> keys = new LinkedList<String>();
		for (File file : toSweep) {
			// Only sweep a file if we can lock it
			long now = System.currentTimeMillis();
			// Only sweep a file that is done being modified
			if(file.lastModified() + MIN_FILE_AGE_MS < now){
				log.debug("Sweeping file: "+file.getAbsolutePath());
				// Now sweep the file
				String key = sweepFile(file);
				keys.add(key);
			}else{
				log.debug("File has been recently modified so it will not be swept yet: "+file.getAbsolutePath());
			}
		}
		return keys;
	}

	/**
	 * Sweep a single file.
	 * 
	 * @param toSweep
	 */
	private String sweepFile(File toSweep) {
		if (toSweep == null)
			throw new IllegalArgumentException("toSweep cannot be null");
		// Save this file to S3 using its last modified by date as the timestamp
		String key = logDAO.saveLogFile(toSweep, toSweep.lastModified());
		// Delete the local file.
		toSweep.delete();
		return key;
	}

}
