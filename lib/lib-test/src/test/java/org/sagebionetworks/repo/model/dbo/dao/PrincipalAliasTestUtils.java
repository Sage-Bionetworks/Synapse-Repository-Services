package org.sagebionetworks.repo.model.dbo.dao;

import java.util.List;

import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;

public class PrincipalAliasTestUtils {
	public static void setUpAlias(long principalId, String alias, PrincipalAliasDAO principalAliasDAO, List<PrincipalAlias> aliasesToDelete) throws NotFoundException {
		setUpAlias(principalId, alias, AliasType.USER_EMAIL, principalAliasDAO, aliasesToDelete);
	}
	
	public static void setUpAlias(long principalId, String email, AliasType type, PrincipalAliasDAO principalAliasDAO, List<PrincipalAlias> aliasesToDelete) throws NotFoundException {
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(email);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		alias.setType(type);
		try {
			alias = principalAliasDAO.bindAliasToPrincipal(alias);
			if (aliasesToDelete!=null) aliasesToDelete.add(alias);
		} catch (NameConflictException e) {
			// alias is already set up
		}
	}
	

}
