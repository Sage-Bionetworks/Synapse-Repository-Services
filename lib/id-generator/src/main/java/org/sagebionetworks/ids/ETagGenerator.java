package org.sagebionetworks.ids;

import org.sagebionetworks.repo.model.TaggableEntity;

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
