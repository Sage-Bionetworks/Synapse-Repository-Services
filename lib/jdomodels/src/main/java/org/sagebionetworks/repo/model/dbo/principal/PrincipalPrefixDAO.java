package org.sagebionetworks.repo.model.dbo.principal;

import java.util.List;

/**
 * Abstraction for a Dao used to lookup principals using a prefix.
 * @author John
 *
 */
public interface PrincipalPrefixDAO {
	

	/**
	 * Add a single principal alias for a given principal, for example, a team name or a username.
	 * 
	 * @param alias
	 * @param principalId
	 */
	public boolean addPrincipalAlias(String alias, Long principalId);
	
	/**
	 * Set the first and last name for a principal.
	 * @param firstName
	 * @param lastName
	 * @param principalId
	 */
	public boolean addPrincipalName(String firstName, String lastName, Long principalId);
	
	/**
	 * Clear all data for a principal.
	 * 
	 * @param principalId
	 */
	public void clearPrincipal(Long principalId);
	
	/**
	 * List a single page of user that match a given prefix.
	 * 
	 * @param prefix
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<Long> listUsersForPrefix(String prefix, Long limit, Long offset);
	
	/**
	 * Count the nubmer of users that match this prefix
	 * @param prefix
	 * @return
	 */
	Long countUsersForPrefix(String prefix);
	
	/**
	 * List all team members with a given prefix for a given team name.
	 * 
	 * @param prefix
	 * @param teamId
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<Long> listTeamMembersForPrefix(String prefix, Long teamId, Long limit, Long offset);
	
	/**
	 * Count the number of users that have the given prefix and belong to the given team.
	 * @param prefix
	 * @param teamId
	 * @return
	 */
	Long countTeamMembersForPrefix(String prefix, Long teamId);

	/**
	 * Delete all data in the table.
	 */
	public void truncateTable();


}
