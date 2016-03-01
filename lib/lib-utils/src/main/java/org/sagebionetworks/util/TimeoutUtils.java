package org.sagebionetworks.util;

/**
 * Simple utility to determine if an event has expired. If the utility is
 * injected into a dependency, expiration checks can be tested using a mock
 * TimeoutUtils regardless of real time.
 *
 */
public class TimeoutUtils {

	/**
	 * Given a timeout (MS) and the start time of an event (epoch time), has the
	 * event expired?
	 * 
	 * @param timeoutMS
	 *            The timeout in MS.
	 * @param startEpochTime
	 *            The s
	 * @return
	 */
	public boolean hasExpired(long timeoutMS, long startEpochTime) {
		long now = System.currentTimeMillis();
		long expires = startEpochTime + timeoutMS;
		return now > expires;
	}
}
