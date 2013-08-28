package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface UserGroupDAO extends BaseDAO<UserGroup>{
	
	/**
	 * @return the group matching the given name, and the given 'individual' property
	 */
	public UserGroup findGroup(String name, boolean isIndividual) throws DatastoreException;

	/**
	 * @return the user groups for the given group names
	 */
	public Map<String, UserGroup> getGroupsByNames(Collection<String> groupName) throws DatastoreException;
	
	/**
	 * @return the list of user groups for the given principal IDs
	 */
	public List<UserGroup> get(List<String> ids) throws DatastoreException;

	/**
	 * a variant of the generic 'getAll' query, this allows the caller to
	 * separately retrieve the individual and non-individual groups.
	 */	
	public Collection<UserGroup> getAll(boolean isIndividual) throws DatastoreException;

	/**
	 * a variant of the generic 'getAll' query, this allows the caller to
	 * separately retrieve the individual and non-individual groups.
	 */	
	public Collection<UserGroup> getAllExcept(boolean isIndividual, Collection<String> groupNamesToOmit) throws DatastoreException;

	/**
	 * a variant of the generic 'getInRange' query, this allows the caller to
	 * separately retrieve the individual and non-individual groups.
	 */

	public List<UserGroup> getInRange(long fromIncl, long toExcl, boolean isIndividual) throws DatastoreException;

	/**
	 * this allows the caller to
	 * separately retrieve the individual and non-individual groups,
	 * while specifying names of groups to filter out
	 */

	public List<UserGroup> getInRangeExcept(long fromIncl, long toExcl,
			boolean isIndividual, Collection<String> groupNamesToOmit)
			throws DatastoreException;
	/**
	 * Does a principal exist with this name?
	 * @param name
	 * @return
	 */
	public boolean doesPrincipalExist(String name);

	/**
	 * 
	 * @param name
	 * @return true if deletion occurs
	 */
	public boolean deletePrincipal(String name);
	
	/**
	 * Get the bootstrap users.
	 * @return
	 */
	public List<UserGroupInt> getBootstrapUsers();
	
	/**
	 * Ensure the bootstrap users exist.
	 * @throws Exception 
	 */
	public void bootstrapUsers() throws Exception;
	
	/**
	 * Gets and locks a row of the table
	 */
	public UserGroup getForUpdate(String id);

	/**
	 * Updates the etag the group with the given ID
	 */
	public void touch(String id);

}
