package org.sagebionetworks.util;

import java.util.Collection;

import org.apache.commons.validator.routines.UrlValidator;

public class ValidateArgument {

	private static UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_2_SLASHES + UrlValidator.ALLOW_ALL_SCHEMES);

	public static void required(Object fieldValue, String fieldName) {
		if (fieldValue == null) {
			throw new IllegalArgumentException(fieldName + " is required.");
		}
	}

	public static void requiredNotEmpty(String fieldValue, String fieldName) {
		if (fieldValue == null || fieldValue.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " is required and must not be the empty string.");
		}
	}

	public static void requiredNotBlank(String fieldValue, String fieldName) {
		requiredNotEmpty(fieldValue, fieldName);
		for (int i = 0; i < fieldValue.length(); i++) {
			if (!Character.isWhitespace(fieldValue.charAt(i))) {
				return;
			}
		}
		throw new IllegalArgumentException(fieldName + " is required and must not be a blank string.");
	}

	public static void requiredNotEmpty(Collection<?> fieldValue, String fieldName) {
		if (fieldValue == null || fieldValue.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " is required and must not be empty.");
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

	public static void validUrl(String url, String fieldDescriptor) {
		if (!urlValidator.isValid(url)) {
			throw new IllegalArgumentException(fieldDescriptor + " is not a valid url: " + url);
		}
	}

	public static void validExternalUrl(String url) {
		validUrl(url, "The External URL");
	}
}
