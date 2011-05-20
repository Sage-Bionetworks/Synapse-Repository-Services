package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.web.NotFoundException;

public interface UserGroupDAO extends BaseDAO<UserGroup> {
	
	/**
	 * @return the group matching the given name, and the given 'individual' property
	 */
	public UserGroup findGroup(String name, boolean isIndividual) throws DatastoreException;

	/**
	 * @return the NON-individual groups for the given group names
	 */
	public Map<String, UserGroup> getGroupsByNames(Collection<String> groupName) throws DatastoreException;

}
