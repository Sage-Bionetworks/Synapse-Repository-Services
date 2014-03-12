package org.sagebionetworks.repo.manager.principal;

import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation of the PrincipalManager.
 * @author John
 *
 */
public class PrincipalManagerImpl implements PrincipalManager {
	
	@Autowired
	PrincipalAliasDAO principalAliasDAO;


	@Override
	public boolean isAliasAvailable(String alias) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		return this.principalAliasDAO.isAliasAvailable(alias);
	}

	@Override
	public boolean isAliasValid(String alias, AliasType type) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		if(type == null) throw new IllegalArgumentException("AliasType cannot be null");
		// Check the value
		try {
			AliasEnum.valueOf(type.name()).validateAlias(alias);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

}
