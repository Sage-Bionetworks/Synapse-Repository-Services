package org.sagebionetworks.repo.manager.oauth.claimprovider;

import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.beans.factory.annotation.Autowired;

public class UserNameClaimProvider implements OIDCClaimProvider {
	@Autowired
	private PrincipalAliasDAO principalAliasDao;
	
	@Override
	public OIDCClaimName getName() {
		return OIDCClaimName.user_name;
	}

	@Override
	public String getDescription() {
		return "To see your Synapse username";
	}

	@Override
	public Object getClaim(String userId, OIDCClaimsRequestDetails details) {
		return principalAliasDao.getUserName(Long.valueOf(userId));
	}

}
