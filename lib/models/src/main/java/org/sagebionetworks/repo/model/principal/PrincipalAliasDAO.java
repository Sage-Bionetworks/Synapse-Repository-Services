package org.sagebionetworks.repo.model.principal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
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
	 * Is an alias available? 
	 * 
	 * @return
	 */
	public boolean isAliasAvailable(String alias);
	/**
	 * Set 
	 * @param aliasId
	 * @param valid
	 * @return
	 */
	public boolean setAliasValid(Long aliasId, boolean valid);
	
	/**
	 * Set the given alias as the default for its type.
	 * @param aliasId
	 * @return
	 */
	public boolean setAliasDefault(Long aliasId);
	
	/**
	 * Remove an alias from a principal.
	 * @param principalId
	 * @param aliasId
	 * @return
	 */
	public boolean removeAliasFromPrincipal(Long principalId, Long aliasId);
}
