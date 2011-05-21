package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface UserGroupDAO extends BaseDAO<UserGroup> {
	
	/**
	 * @return the group matching the given name, and the given 'individual' property
	 */
	public UserGroup findGroup(String name, boolean isIndividual) throws DatastoreException;

	/**
	 * @return the NON-individual groups for the given group names
	 */
	public Map<String, UserGroup> getGroupsByNames(Collection<String> groupName) throws DatastoreException;

	/**
	 * a variant of the generic 'getAll' query, this allows the caller to
	 * separately retrieve the individual and non-individual groups.
	 */	
	public Collection<UserGroup> getAll(boolean isIndividual) throws DatastoreException;

	/**
	 * a variant of the generic 'getInRange' query, this allows the caller to
	 * separately retrieve the individual and non-individual groups.
	 */

	public List<UserGroup> getInRange(long fromIncl, long toExcl, boolean isIndividual) throws DatastoreException;
}
