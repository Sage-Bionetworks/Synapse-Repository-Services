package org.sagebionetworks.repo.manager.oauth.claimprovider;

import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;

public class UserIdClaimProvider implements OIDCClaimProvider {
	@Override
	public OIDCClaimName getName() {
		return OIDCClaimName.userid;
	}

	@Override
	public String getDescription() {
		return  "To see your Synapse user ID, which can be used to access your public profile";
	}

	@Override
	public Object getClaim(String userId, OIDCClaimsRequestDetails details) {
		return userId;
	}

}
