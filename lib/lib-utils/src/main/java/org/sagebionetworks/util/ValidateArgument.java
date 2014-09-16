package org.sagebionetworks.util;

public class ValidateArgument {

	public static void required(Object fieldValue, String fieldName) {
		if (fieldValue == null) {
			throw new IllegalArgumentException(fieldName + " is required.");
		}
	}

	public static void notNull(Object fieldValue, String fieldName) {
		if (fieldValue == null) {
			throw new IllegalArgumentException(fieldName + " cannot be null.");
		}
	}

	public static void optional(String description, String string) {
	}
}
