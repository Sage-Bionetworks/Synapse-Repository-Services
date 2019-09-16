package org.sagebionetworks.repo.manager.oauth.claimprovider;

import java.util.List;

import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.VerificationHelper;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.springframework.beans.factory.annotation.Autowired;

public class ValidatedEmailClaimProvider implements OIDCClaimProvider {
	@Autowired
	private UserProfileManager userProfileManager;

	@Override
	public OIDCClaimName getName() {
		return OIDCClaimName.validated_email;
	}

	@Override
	public String getDescription() {
		return "If you are a validated user, your validated email";
	}

	@Override
	public Object getClaim(String userId, OIDCClaimsRequestDetails details) {
		VerificationSubmission verificationSubmission = userProfileManager.getCurrentVerificationSubmission(Long.parseLong(userId));
		if (VerificationHelper.isVerified(verificationSubmission)) {
			List<String> validatedEmails = verificationSubmission.getEmails();
			if (validatedEmails==null || validatedEmails.isEmpty()) {
				return null;
			} else {
				return validatedEmails.get(0);
			}
		} else {
			return null;
		}
	}

}
