package org.sagebionetworks.repo.model.ar;

/**
 * Defines the binding type of an access requirement to a subject.
 *
 */
public enum BindingType {
	// Indicate subjects that are bound dynamically from derived annotations.
	DYNAMIC,
	// Indicates subjects that are bound via the subjects list of the AccessRequirement.
	MANUAL
}
