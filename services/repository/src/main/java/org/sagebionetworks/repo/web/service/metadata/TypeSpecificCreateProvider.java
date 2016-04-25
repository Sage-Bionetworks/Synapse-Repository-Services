package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.UserInfo;

public interface TypeSpecificCreateProvider<T extends Entity> extends EntityProvider<T> {

	/**
	 * Called when an entity is created.
	 * @param userInfo
	 * @param entityId
	 */
	public void entityCreated(UserInfo userInfo, T entity);
}
