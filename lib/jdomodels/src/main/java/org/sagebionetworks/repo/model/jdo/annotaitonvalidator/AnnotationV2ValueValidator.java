package org.sagebionetworks.repo.model.jdo.annotaitonvalidator;

public interface AnnotationV2ValueValidator {
	/**
	 * Validates the value
	 * @param value
	 * @return true if value is valid. false otherwise.
	 */
	public boolean validateValue(String value);
}
