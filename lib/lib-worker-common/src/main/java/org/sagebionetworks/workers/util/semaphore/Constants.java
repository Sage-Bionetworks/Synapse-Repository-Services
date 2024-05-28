package org.sagebionetworks.workers.util.semaphore;

public class Constants {

	public static final int MINIMUM_LOCK_TIMEOUT_SEC = 2;
	
	/**
	 * Sleep and throttle frequency.
	 */
	public static final long THROTTLE_SLEEP_FREQUENCY_MS = 2000;
	public static final String WRITER_LOCK_SUFFIX = "_WRITER_LOCK";
	public static final String READER_LOCK_SUFFIX = "_READER_LOCK";
	public static final int WRITER_MAX_LOCKS = 1;
	
	public static String createWriterLockKey(final String lockKey){
		return lockKey + Constants.WRITER_LOCK_SUFFIX;
	}

	public static String createReaderLockKey(final String lockKey){
		return lockKey + Constants.READER_LOCK_SUFFIX;
	}
}
