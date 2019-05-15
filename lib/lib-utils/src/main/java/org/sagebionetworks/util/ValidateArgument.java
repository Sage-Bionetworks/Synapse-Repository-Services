package org.sagebionetworks.util;

import org.apache.commons.validator.routines.UrlValidator;

public class ValidateArgument {

	private static UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_2_SLASHES + UrlValidator.ALLOW_ALL_SCHEMES);

	public static void required(Object fieldValue, String fieldName) {
		if (fieldValue == null) {
			throw new IllegalArgumentException(fieldName + " is required.");
		}
	}

	public static void requiredNotEmpty(String fieldValue, String fieldName){
		if(fieldValue == null || "".equals(fieldValue) ){
			throw new IllegalArgumentException(fieldName + " is required and must not be the empty string.");
		}
	}

	public static void requirement(boolean requirement, String message) {
		if (!requirement) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void requireType(Object fieldValue, Class<?> requiredType, String fieldName) {
		required(fieldValue, fieldName);
		if (!requiredType.isInstance(fieldValue)) {
			throw new IllegalArgumentException("Expected " + fieldName + " to be of type " + requiredType.getName() + ", but it was type "
					+ fieldValue.getClass().getName() + " instead");
		}
	}

	public static void failRequirement(String message) {
		throw new IllegalArgumentException(message);
	}

	public static void optional(String description, String string) {
	}

	public static void validUrl(String url) {
		if (!urlValidator.isValid(url)) {
			throw new IllegalArgumentException("The ExternalURL is not a valid url: " + url);
		}
	}
}
