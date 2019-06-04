package org.sagebionetworks.repo.model.dbo.principal;

import java.util.List;
import java.util.Set;

/**
 * Abstraction for a DAO used to lookup principals using a prefix.
 * 
 * @author John
 * 
 */
public interface PrincipalPrefixDAO {

	/**
	 * Add a single principal alias for a given principal, for example, a team
	 * name or a username.
	 * 
	 * @param alias
	 * @param principalId The principal ID of the user or team.
	 */
	public void addPrincipalAlias(String alias, Long principalId);

	/**
	 * Set the first and last name for a principal. A concatenation of both
	 * first-last and last-first will be added to the table.
	 * 
	 * @param firstName user's first name
	 * @param lastName user's last name
	 * @param principalId user's principal ID.
	 */
	public void addPrincipalName(String firstName, String lastName,
			Long principalId);

	/**
	 * Clear all data for a principal.
	 * 
	 * @param principalId The principal ID of the user or team.
	 */
	public void clearPrincipal(Long principalId);

	/**
	 * List a single page of users or teams that match a given prefix.
	 * 
	 * @param limit Pagination parameter.
	 * @param offset Pagination parameter.
	 * @return List of principal IDs that match the query ordered alphabetically.
	 */
	List<Long> listPrincipalsForPrefix(String prefix, Long limit, Long offset);
	
	/**
	 * List a single page of users or usergroups that match the given prefix.
	 * 
	 * @param prefix
	 * @param isIndividual True for users, false for user groups.
	 * @param limit
	 * @param offset
	 * @return A list of IDs corresponding to matching users or usergroups.
	 */
	List<Long> listPrincipalsForPrefix(String prefix, boolean isIndividual, Long limit, Long offset);

	/**
	 * List a single page of teams that match the given prefix;
	 *
	 * @param prefix
	 * @param limit
	 * @param offset
	 * @return A list of IDs corresponding to matching teams.
	 */
	List<Long> listTeamsForPrefix(String prefix, Long limit, Long offset);

	/**
	 * For a given team, list all members that share the given prefix.
	 * 
	 * @param prefix Prefix to filter by.
	 * @param teamId The results will only include members of this team.
	 * @param limit Pagination parameter.
	 * @param offset Pagination parameter.
	 * @return List of principal IDs that match the query ordered alphabetically.
	 */
	List<Long> listTeamMembersForPrefix(String prefix, Long teamId, Long limit,
			Long offset);

	/**
	 * For a given team, list all members that share the given prefix, and have or do not have particular principal IDs.
	 * Exclusion of particular principal IDs has precedence over inclusion.
	 * @param prefix Prefix to filter by.
	 * @param teamId The results will only include members of this team.
	 * @param include The set of principal IDs to explicitly include in search. All members with principal IDs that do not match
	 *             IDs in this set will be filtered out. If null, a filter is not applied.
	 * @param exclude The set of principal IDs to explicitly exclude in search. All members with principal IDs that do match
	 *             IDs in this set will not be included in results. If null, a filter is not applied.
	 * @param limit Pagination parameter.
	 * @param offset Pagination parameter.
	 * @return List of principal IDs that match the query and filter parameters, ordered alphabetically.
	 */
	List<Long> listCertainTeamMembersForPrefix(String prefix, Long teamId, Set<Long> include, Set<Long> exclude, Long limit, Long offset);

	/**
	 * For a given team, count all members that share the given prefix.
	 * 
	 * @param prefix Prefix to filter by.
	 * @param teamId
	 * @return
	 */
	Long countTeamMembersForPrefix(String prefix, Long teamId);

	/**
	 * Delete all data in the table.
	 */
	public void truncateTable();

}
