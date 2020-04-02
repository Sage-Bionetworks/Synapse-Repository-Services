package org.sagebionetworks.repo.manager.oauth.claimprovider;

import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.springframework.beans.factory.annotation.Autowired;

public class OrcidClaimProvider implements OIDCClaimProvider {
	@Autowired
	private UserProfileManager userProfileManager;

	@Override
	public OIDCClaimName getName() {
		return OIDCClaimName.orcid;
	}

	@Override
	public String getDescription() {
		return "To see the ORCID you have linked to your Synapse account, if any";
	}

	@Override
	public Object getClaim(String userId, OIDCClaimsRequestDetails details) {
		return userProfileManager.getOrcid(Long.parseLong(userId));
	}

}
