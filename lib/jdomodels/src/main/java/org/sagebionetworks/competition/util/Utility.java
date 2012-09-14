package org.sagebionetworks.competition.util;

public class Utility {
	
	/**
	 * Ensure that an object is not null.
	 * 
	 * @param o
	 * @param objectName
	 */
	public static void ensureNotNull(Object o, String objectName) {
		if (o == null) 
			throw new IllegalArgumentException(objectName + " cannot be null.");
	}

}
