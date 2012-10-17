package org.sagebionetworks.competition.util;

public class Utility {
	
	/**
	 * Ensure that one or several arguments is/are not null.
	 * 
	 * @param o
	 * @param objectName
	 */
	public static void ensureNotNull(Object ... objects) {
		for (Object o : objects)
			if (o == null)
				throw new IllegalArgumentException("Invalid null argument.");		
	}

}
