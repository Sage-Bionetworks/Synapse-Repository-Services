package org.sagebionetworks.repo.model.dbo.dao;

import java.util.List;

import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;

public class PrincipalAliasTestUtils {
	public static void setUpAlias(long principalId, String email, PrincipalAliasDAO principalAliasDAO, List<PrincipalAlias> aliasesToDelete) throws NotFoundException {
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(email);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		alias.setType(AliasType.USER_EMAIL);
		try {
			alias = principalAliasDAO.bindAliasToPrincipal(alias);
			aliasesToDelete.add(alias);
		} catch (NameConflictException e) {
			// alias is already set up
		}
	}
	

}
