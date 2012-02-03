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
	 * Get the EntityHeaders of the entities which refer to a given target, regardless of the target's revision
	 * @param targetId the Node ID of the target
	 * @return a List of EntityHeaders
	 * 
	 */
	public Collection<EntityHeader> getReferrers(Long targetId, UserInfo userInfo);

	/**
	 * Get the EntityHeader of the entities which refer to a specific revision of a given target
	 * @param targetId the Node ID of the target
	 * @param targetVersion the version of the target
	 * @return a List of EntityHeaders
	 * 
	 */
	public Collection<EntityHeader> getReferrers(Long targetId, int targetVersion, UserInfo userInfo);
}
