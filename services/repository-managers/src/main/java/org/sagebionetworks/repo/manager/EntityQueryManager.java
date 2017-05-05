package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.query.EntityQuery;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResults;
import org.sagebionetworks.repo.model.query.BasicQuery;

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

	/**
	 * Execute the passed query for the given user.
	 * 
	 * @param query
	 * @param userInfo
	 * @return
	 */
	public NodeQueryResults executeQuery(BasicQuery query, UserInfo userInfo);
}
