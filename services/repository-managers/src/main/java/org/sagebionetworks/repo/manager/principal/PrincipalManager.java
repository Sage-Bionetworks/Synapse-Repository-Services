package org.sagebionetworks.repo.manager.principal;

import org.sagebionetworks.repo.model.principal.AliasType;

public interface PrincipalManager {

	/**
	 * Is the passed alias available?
	 * 
	 * @param alias
	 * @param type
	 * @return
	 */
	boolean isAliasAvailable(String alias);

	/**
	 * Is the passed alias valid for the passed type?
	 * 
	 * @param alias
	 * @param type
	 * @return
	 */
	boolean isAliasValid(String alias, AliasType type);
}
