package org.sagebionetworks.repo.manager.oauth.claimprovider;

import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.springframework.beans.factory.annotation.Autowired;

//https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims
public class FamilyNameClaimProvider implements OIDCClaimProvider {
	@Autowired
	private UserProfileManager userProfileManager;

	@Override
	public OIDCClaimName getName() {
		return OIDCClaimName.family_name;
	}

	@Override
	public String getDescription() {
		return "To see your last name, if you share it with Synapse";
	}

	@Override
	public Object getClaim(String userId, OIDCClaimsRequestDetails details) {
		return userProfileManager.getUserProfile(userId).getLastName();
	}

}
