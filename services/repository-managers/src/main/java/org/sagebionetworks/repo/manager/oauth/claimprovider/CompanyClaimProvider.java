package org.sagebionetworks.repo.manager.oauth.claimprovider;

import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.springframework.beans.factory.annotation.Autowired;

public class CompanyClaimProvider implements OIDCClaimProvider {
	@Autowired
	private UserProfileManager userProfileManager;

	@Override
	public OIDCClaimName getName() {
		return OIDCClaimName.company;
	}

	@Override
	public String getDescription() {
		return "To see your company, if you share it with Synapse";
	}

	@Override
	public Object getClaim(String userId, OIDCClaimsRequestDetails details) {
		return userProfileManager.getUserProfile(userId).getCompany();
	}

}
