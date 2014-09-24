package org.sagebionetworks.util;

public class ValidateArgument {

	public static void required(Object fieldValue, String fieldName) {
		if (fieldValue == null) {
			throw new IllegalArgumentException(fieldName + " is required.");
		}
	}

	public static void requiredOneOf(String fieldNames, Object... fieldValues) {
		boolean allNull = true;
		boolean oneNull = false;
		for (Object fieldValue : fieldValues) {
			if (fieldValue != null) {
				allNull = false;
				if (oneNull == true) {
					throw new IllegalArgumentException("Only one of " + fieldNames + " can be specified.");
				}
				oneNull = true;
			}
		}
		if (allNull) {
			throw new IllegalArgumentException("One of " + fieldNames + " is required.");
		}
	}

	public static void optional(String description, String string) {
	}
}
