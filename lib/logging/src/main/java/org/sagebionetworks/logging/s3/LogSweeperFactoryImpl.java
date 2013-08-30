package org.sagebionetworks.logging.s3;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;

/**
 * This is the autowired bean that can be called from a timer.
 * 
 * @author John
 *
 */
public class LogSweeperFactoryImpl implements LogSweeperFactory {
	
	@Autowired
	private AmazonS3Client s3Client;
	private File logDirectory;
	private long lockExpiresMs;
	private int stackInstanceNumber;
	private String bucketName;

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
	 * Injected via Spring
	 * @param instanceNumber
	 */
	public void setStackInstanceNumber(int stackInstanceNumber) {
		this.stackInstanceNumber = stackInstanceNumber;
	}

	/**
	 * Injected via Spring
	 * @param bucketName
	 */
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	/**
	 * This method can be called from a timer.
	 */
	@Override
	public List<String> sweepLogs() {
		// Create the sweeper and run it.
		LogSweeper sweeper =  new LogSweeper(logDirectory, lockExpiresMs, s3Client, stackInstanceNumber, bucketName);
		return sweeper.sweepAllfiles();
	}
	
	/**
	 * Called when the bean is initialized.
	 */
	public void initialize() {
		if (bucketName == null)
			throw new IllegalArgumentException("bucketName has not been set and cannot be null");
		// Create the bucket if it does not exist
		s3Client.createBucket(bucketName);
	}

	@Override
	public String getLogBucketName() {
		return this.bucketName;
	}

	@Override
	public File getLoggingDirectory() {
		return this.logDirectory;
	}

}
