package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.query.EntityQuery;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResults;

/**
 * Abstraction for executing an entity query.
 * @author John
 *
 */
public interface EntityQueryManager {

	/**
	 * Execute the passed entity query.
	 * @param query
	 * @param user
	 * @return
	 */
	public EntityQueryResults executeQuery(EntityQuery query, UserInfo user);
}
