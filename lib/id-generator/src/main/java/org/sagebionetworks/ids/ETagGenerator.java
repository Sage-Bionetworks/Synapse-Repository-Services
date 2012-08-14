package org.sagebionetworks.ids;

/**
 * Generates e-tags for entities.
 *
 * @author ewu
 */
public interface ETagGenerator {

	/**
	 * Generates a tag for the given entity.
	 */
	String generateETag(TaggableEntity entity);
}
