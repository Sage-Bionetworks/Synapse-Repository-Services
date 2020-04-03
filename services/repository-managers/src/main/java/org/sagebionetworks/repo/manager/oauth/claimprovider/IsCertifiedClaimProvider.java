package org.sagebionetworks.repo.manager.oauth.claimprovider;

import org.sagebionetworks.repo.manager.UserInfoHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.springframework.beans.factory.annotation.Autowired;


public class IsCertifiedClaimProvider implements OIDCClaimProvider {
	@Autowired
	private UserManager userManager;

	@Override
	public OIDCClaimName getName() {
		return OIDCClaimName.is_certified;
	}

	@Override
	public String getDescription() {
		return "To see whether you are a certified Synapse user";
	}

	@Override
	public Object getClaim(String userId, OIDCClaimsRequestDetails details) {
		return UserInfoHelper.isCertified(userManager.getUserInfo(Long.parseLong(userId)));
	}

}
