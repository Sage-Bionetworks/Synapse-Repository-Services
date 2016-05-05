package org.sagebionetworks.repo.model.principal;

import java.util.List;
import java.util.Set;

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
	 * Find a principal by an alias
	 * @param alias
	 * @return
	 */
	public PrincipalAlias findPrincipalWithAlias(String alias);
	
	/**
	 * Return the principals having the given aliases.
	 * The size of the result is less than or equal to the
	 * size of the passed in set.  Any unknown aliases
	 * are simply omitted from the results
	 * 
	 * @param aliases
	 * @return List of principals matching the given aliases
	 */
	public Set<PrincipalAlias> findPrincipalsWithAliases(Set<String> aliases);
	
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
	public List<PrincipalAlias> listPrincipalAliases(Set<Long> principalIds);
	
	/**
	 * Get all aliases for a principal and type.
	 * @param principalId
	 * @param type When provide, will return only aliases of the given type.  When null, all aliases for a principal will be returned.
	 * @return
	 */
	public List<PrincipalAlias> listPrincipalAliases(Long principalId, AliasType type);
	
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
}
