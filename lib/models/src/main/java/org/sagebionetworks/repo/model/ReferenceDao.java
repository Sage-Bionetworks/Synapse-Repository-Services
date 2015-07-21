package org.sagebionetworks.repo.model;


public interface ReferenceDao {
	
	/**
	 * Replace reference for a given owner.
	 * @param toReplace
	 * @return
	 * @throws DatastoreException 
	 */
	public Reference replaceReference(Long ownerId, Reference reference) throws DatastoreException;

	/**
	 * Removes the reference associated with the specified owner.
	 * @param ownerId
	 */
	public void deleteReferenceByOwnderId(Long ownerId);

	/**
	 * Get reference for a given owner.
	 * @param ownerId
	 * @return
	 */
	public Reference getReference(Long ownerId);

	/**
	 * Get the EntityHeaders of the entities which refer to a given target
	 * if targetVersion is not null then return just the referrers of the given specific version of the target
	 * @param targetId the Node ID of the target
	 * @param targetVersion the version of the target
	 * @param offset ZERO based pagination param
	 * @param limit pagination param
	 * @return a List of EntityHeaders
	 * @throws DatastoreException 
	 * 
	 */
	public QueryResults<EntityHeader> getReferrers(Long targetId, Integer targetVersion, UserInfo userInfo, Integer offset, Integer limit) throws DatastoreException;

}
