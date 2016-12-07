package org.sagebionetworks.repo.model;

public enum AnnotationNameSpace {
	/**
	 * The direct fields of an entity are stored in the primary name-space.
	 */
	PRIMARY,
	/**
	 * All additional annotations belong to this name-space.
	 */
	ADDITIONAL
}