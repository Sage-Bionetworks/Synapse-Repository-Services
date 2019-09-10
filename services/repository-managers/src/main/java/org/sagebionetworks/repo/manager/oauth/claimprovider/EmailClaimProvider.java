package org.sagebionetworks.repo.manager.oauth.claimprovider;

import java.util.List;

import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.springframework.beans.factory.annotation.Autowired;

public class EmailClaimProvider implements OIDCClaimProvider {
	@Autowired
	private UserProfileManager userProfileManager;

	@Override
	public OIDCClaimName getName() {
		return OIDCClaimName.email;
	}

	@Override
	public String getDescription() {
		return "Your email address";
	}

	@Override
	public Object getClaim(String userId, OIDCClaimsRequestDetails details) {
		UserProfile userProfile = userProfileManager.getUserProfile(userId);
		List<String> emails = userProfile.getEmails();
		if (emails==null || emails.isEmpty()) {
			return null;
		}
		return emails.get(0);
	}

}
