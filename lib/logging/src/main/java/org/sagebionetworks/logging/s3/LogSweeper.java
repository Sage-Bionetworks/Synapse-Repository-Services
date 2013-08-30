package org.sagebionetworks.logging.s3;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

/**
 * This class sweeps file from a local directory to S3.
 * 
 * @author John
 * 
 */
public class LogSweeper {

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
				// we are the lock holder.
				try {
					// we can now sweep.
					return sweepAllGZipFiles();
				} finally {
					// unconditionally release the lock.
					lock.delete();
				}
			} else {
				// If here then the lock file already exists. Has the lock
				// expired?
				long heldForMS = System.currentTimeMillis()
						- lock.lastModified();
				if (heldForMS > lockExpiresMs) {
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
	 */
	private List<String> sweepAllGZipFiles() {
		File[] toSweep = logDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				// We only care about GZIP files.
				return pathname.getName().toLowerCase().trim().endsWith(".gz");
			}
		});
		List<String> keys = new LinkedList<String>();
		for (File file : toSweep) {
			String key = sweepFile(file);
			keys.add(key);
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
