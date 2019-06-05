package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.UserInfo;

/**
 * Implement to listen to entity update events.
 *
 * @param <T> Type of Entity to listen to.
 */
public interface TypeSpecificUpdateProvider<T extends Entity> extends EntityProvider<T> {

	/**
	 * Called when an entity is updated.
	 * @param userInfo
	 * @param entityId
	 * @param True if a new version was created as a result of this update.
	 */
	public void entityUpdated(UserInfo userInfo, T entity, boolean wasNewVersionCreated);
}
