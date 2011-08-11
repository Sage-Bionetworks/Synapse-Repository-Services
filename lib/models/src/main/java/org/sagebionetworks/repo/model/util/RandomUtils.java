package org.sagebionetworks.repo.model.util;

import java.util.Date;
import java.util.Random;

public class RandomUtils {
	
	// By starting with a real time we get stable random dates.
	private static final long STABLE_TIME = 1312832819234L;
	private static final int MILLISECONDS_PER_WEEK = 1000*60*60*24*7;
	
	/**
	 * If we just use a random long for a date, the results are unstable.
	 * Therefore, this method creates a random date
	 * @param rand
	 * @return
	 */
	public static Date createRandomStableDate(Random rand){
		int delta = rand.nextInt(MILLISECONDS_PER_WEEK);
		long time = STABLE_TIME - delta;
		return new Date(time);
	}
	
	/**
	 * Create a random blob using the maximum size.  Note: The size of the blob 
	 * is random and will be between 1 and max.
	 * @param rand
	 * @param maxSize
	 * @return
	 */
	public static byte[] createRandomBlob(Random rand, int maxSize){
		int size = rand.nextInt(maxSize);
		// Exclude zero
		size++;
		byte[] array = new byte[size];
		rand.nextBytes(array);
		return array;
	}

}
