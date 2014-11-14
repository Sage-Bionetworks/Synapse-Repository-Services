package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.model.Entity;

/**
 * Allows entity specific post delete version actions
 * 
 * @param <T>
 */
public interface TypeSpecificVersionDeleteProvider<T extends Entity> extends EntityProvider<T> {
	
	/**
	 * Called when an entity is deleted.
	 * 
	 * @param deletedId
	 */
	public void entityVersionDeleted(String deletedId, Long version);
}
