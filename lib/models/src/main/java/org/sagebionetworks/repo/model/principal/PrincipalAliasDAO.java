package org.sagebionetworks.repo.model.principal;

import java.util.List;
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
	 * List the Aliases bound to a principal of the given type.
	 * @param principalId
	 * @param typesToInclude If null then all types will be returned.  When provided only, AliasType in this set will be returned.
	 * @return
	 */
	public List<PrincipalAlias> listPrincipalAliases(Long principalId, Set<AliasType> typesToInclude);
	
	/**
	 * Remove an alias from a principal.
	 * @param principalId
	 * @param aliasId
	 * @return
	 */
	public boolean removeAliasFromPrincipal(Long principalId, Long aliasId);
}
