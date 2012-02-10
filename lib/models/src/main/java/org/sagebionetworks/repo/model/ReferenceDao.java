package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UserInfo;

public interface ReferenceDao {
	
	/**
	 * Replace all references for a given owner.
	 * @param toReplace
	 * @return
	 * @throws DatastoreException 
	 */
	public Map<String, Set<Reference>> replaceReferences(Long ownerId, Map<String, Set<Reference>> references) throws DatastoreException;
	
	/**
	 * Get all references for a given owner.
	 * @param ownerId
	 * @return
	 */
	public Map<String, Set<Reference>> getReferences(Long ownerId);

	/**
	 * Get the EntityHeaders of the entities which refer to a given target
	 * if targetVersion is not null then return just the referrers of the given specific version of the target
	 * @param targetId the Node ID of the target
	 * @param targetVersion the version of the target
	 * @param offset ZERO based pagination param
	 * @param limit pagination param
	 * @return a List of EntityHeaders
	 * 
	 */
	public EntityHeaderQueryResults getReferrers(Long targetId, Integer targetVersion, UserInfo userInfo, Integer offset, Integer limit);

}
