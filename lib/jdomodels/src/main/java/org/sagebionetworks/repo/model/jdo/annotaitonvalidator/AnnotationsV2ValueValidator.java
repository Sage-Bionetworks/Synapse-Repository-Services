package org.sagebionetworks.repo.model.jdo.annotaitonvalidator;

public interface AnnotationsV2ValueValidator {
	/**
	 * true if value is valid. false otherwise.
	 * @param value
	 * @return true if value is valid. false otherwise.
	 */
	public boolean isValidValue(String value);
}
