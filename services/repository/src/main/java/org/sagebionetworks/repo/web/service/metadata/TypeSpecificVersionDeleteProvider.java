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
	 * @param deleted 
	 */
	public void entityVersionDeleted(T deleted, Long version);
}
