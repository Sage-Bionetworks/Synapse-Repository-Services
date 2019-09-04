package org.sagebionetworks.repo.model.principal;

import java.util.Collection;
import java.util.List;

import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for binding alias to principals and looking up principals by alias.
 * 
 * @author John
 *
 */
public interface PrincipalAliasDAO {
	
	/**
	 * Bind an alias to a principal.
	 * 
	 * @param binding
	 * @return
	 * @throws NotFoundException 
	 */
	public PrincipalAlias bindAliasToPrincipal(PrincipalAlias binding) throws NotFoundException;
	
	/**
	 * Get a principal alias by its id.
	 * @param aliasId
	 * @return
	 * @throws NotFoundException 
	 */
	public PrincipalAlias getPrincipalAlias(Long aliasId) throws NotFoundException;

	/**
	 * Find a principal by an alias. If aliasTypes are passed in, will check that the principal is one of the aliasTypes.
	 * @param alias
	 * @param aliasTypes optional.
	 * @return
	 */
	public PrincipalAlias findPrincipalWithAlias(String alias, AliasType... aliasTypes);
	
	/**
	 * List all aliases for a given principal.
	 * @param principalId
	 * @return
	 */
	public List<PrincipalAlias> listPrincipalAliases(Long principalId);
	
	/**
	 * Given a set of principals ID get all aliases.
	 * @param principalIds
	 * @return
	 */
	public List<PrincipalAlias> listPrincipalAliases(Collection<Long> principalIds);
	
	/**
	 * Get the UserGroupHeaders for the given list of principalIds.
	 * 
	 * @param principalIds
	 * @return
	 */
	public List<UserGroupHeader> listPrincipalHeaders(List<Long> principalIds);
	
	/**
	 * Get all aliases for a principal and type.
	 * @param principalId
	 * @param types When provided, will return only aliases of the given types.  When null, all aliases for a principal will be returned.
	 * @return
	 */
	public List<PrincipalAlias> listPrincipalAliases(Long principalId, AliasType... types);
	
	/**
	 * Get all aliases for a principal, type and display value.
	 * @param principalId
	 * @param type
	 * @param displayAlias
	 * @return
	 */
	public List<PrincipalAlias> listPrincipalAliases(Long principalId, AliasType type, String displayAlias);
	
	/**
	 * List all aliases for a given type.
	 * @param type
	 * @return
	 */
	public List<PrincipalAlias> listPrincipalAliases(AliasType type);
	/**
	 * Is an alias available? 
	 * 
	 * @return
	 */
	public boolean isAliasAvailable(String alias);
	
	/**
	 * Remove an alias from a principal.
	 * @param principalId
	 * @param aliasId
	 * @return
	 */
	public boolean removeAliasFromPrincipal(Long principalId, Long aliasId);
	
	/**
	 * Remove all aliases from the given principal.
	 * @param principalId
	 * @return
	 */
	public boolean removeAllAliasFromPrincipal(Long principalId);
	
	/**
	 * There must be exactly one user name
	 * @param principalId
	 * @return
	 * @throw NotFoundException if no user name
	 */
	public String getUserName(Long principalId) throws NotFoundException;

	/**
	 * Get the principal ID for the given alias and alias type
	 * 
	 * @param alias
	 * @param type
	 * @return
	 */
	public long lookupPrincipalID(String alias, AliasType type);

	/**
	 * Get team name for the given principalId
	 * 
	 * @param principalId
	 * @return
	 * @throws NotFoundException
	 */
	public String getTeamName(Long principalId) throws NotFoundException;
	
	/**
	 * Find all principal IDs for the given list of aliases and types. 
	 * @param aliases
	 * @param types
	 * @return
	 */
	List<Long> findPrincipalsWithAliases(Collection<String> aliases, List<AliasType> types);


	/**
	 * Determine if the given alias is bound to the given principal.
	 *
	 * @param alias
	 * @param principalId
	 * @return
	 */
	boolean aliasIsBoundToPrincipal(String alias, String principalId);
}
