package org.sagebionetworks.ids;

import org.sagebionetworks.repo.model.TaggableEntity;

/**
 * Generates e-tags for taggable entities.
 */
public interface ETagGenerator {

	/**
	 * Generates a e-tag for the given entity. The entity might
	 * (or might not) be used to compute the e-tag.
	 */
	String generateETag(TaggableEntity entity);

	/**
	 * Generates a random e-tag.
	 */
	String generateETag();
}
