package org.sagebionetworks.repo.manager.oauth.claimprovider;

import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;

public class EmailVerifiedClaimProvider implements OIDCClaimProvider {
	@Override
	public OIDCClaimName getName() {
		return OIDCClaimName.email_verified;
	}

	@Override
	public String getDescription() {
		return  "To see whether Synapse verified your email address. (Always true for Synapse.)";
	}

	@Override
	public Object getClaim(String userId, OIDCClaimsRequestDetails details) {
		return Boolean.TRUE;
	}

}
