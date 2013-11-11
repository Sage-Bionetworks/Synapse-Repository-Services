package org.sagebionetworks.bridge.manager;

import org.sagebionetworks.repo.model.InvalidModelException;

public class Validate {

	public static void notSpecifiable(Object fieldValue, String fieldName) {
		if (fieldValue != null) {
			throw new InvalidModelException("'" + fieldName + "' field is not user specifiable.");
		}
	}

	public static void missing(Object fieldValue, String fieldName) {
		if (fieldValue == null) {
			throw new InvalidModelException("'" + fieldName + "' field is missing.");
		}
	}

	public static void required(Object fieldValue, String fieldName) {
		if (fieldValue == null) {
			throw new InvalidModelException("'" + fieldName + "' field is required.");
		}
	}
}
