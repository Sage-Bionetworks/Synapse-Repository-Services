package org.sagebionetworks.util;

import org.apache.commons.validator.routines.UrlValidator;

public class ValidateArgument {

	private static UrlValidatorPatched urlValidator = new UrlValidatorPatched(UrlValidator.ALLOW_2_SLASHES + UrlValidator.ALLOW_ALL_SCHEMES);

	public static void required(Object fieldValue, String fieldName) {
		if (fieldValue == null) {
			throw new IllegalArgumentException(fieldName + " is required.");
		}
	}

	public static void requirement(boolean requirement, String message) {
		if (!requirement) {
			throw new IllegalArgumentException(message);
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
