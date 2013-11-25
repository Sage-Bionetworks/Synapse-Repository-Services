package org.sagebionetworks.ids;


/**
 * Standardize e-tag generation across the code base
 */
public interface ETagGenerator {

	/**
	 * Generates a random e-tag.
	 */
	String generateETag();
}
