package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.UserInfo;

public interface TypeSpecificGetProvider<T extends Entity> extends EntityProvider<T> {

	/**
	 * Called before an entity is returned.
	 * @param userInfo
	 * @param entityId
	 */
	public void beforeGet(UserInfo userInfo, String entityId);
}
