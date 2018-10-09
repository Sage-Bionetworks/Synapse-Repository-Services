package org.sagebionetworks.repo.model;

import java.util.Set;

public interface AuthorizationDAO  {

	/**
	 * @return true iff some group in 'groups' has explicit permission to access 'resourceId' using access type 'accessType'
	 * @throws DatastoreException 
	 */
	public boolean canAccess(Set<Long> groups, String resourceId, ObjectType resourceType, ACCESS_TYPE accessType) throws DatastoreException;

	/**
	 * Given a set of benefactors, and benefactors, return the sub-set of benefactors the that any given principal can see.
	 * @param groups
	 * @param benefactors
	 * @param entity
	 * @param read
	 * @return
	 */
	public Set<Long> getAccessibleBenefactors(Set<Long> groups, Set<Long> benefactors,
			ObjectType entity, ACCESS_TYPE read);

	/**
	 * Get the ids of the children of the given entity that the passed
	 * set of groups does not have the read permission.
	 * 
	 * @param groups
	 * @param parentId
	 * @return
	 */
	public Set<Long> getNonVisibleChildrenOfEntity(Set<Long> groups,
			String parentId);

}
