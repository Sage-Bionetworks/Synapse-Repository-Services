package org.sagebionetworks.evaluation.util;

import java.util.Collection;

public class EvaluationUtils {

	/**
	 * Ensure that an argument is not null.
	 */
	public static void ensureNotNull(Object o, String name) {
		if (o == null) {
			throw new IllegalArgumentException(name + " cannot be null");
		}
	}
	
	/**
	 * Ensure that a Collection argument is not empty
	 * @param o
	 * @param name
	 */
	public static void ensureNotEmpty(Collection o, String name) {
		if (o == null || o.isEmpty()) {
			throw new IllegalArgumentException(name + " cannot be empty");
		}
	}
}
