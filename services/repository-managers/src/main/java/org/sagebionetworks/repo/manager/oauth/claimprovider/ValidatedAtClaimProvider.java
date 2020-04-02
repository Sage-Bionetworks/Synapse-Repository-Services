package org.sagebionetworks.repo.manager.oauth.claimprovider;

import java.util.Date;

import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.verification.VerificationHelper;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.springframework.beans.factory.annotation.Autowired;

public class ValidatedAtClaimProvider implements OIDCClaimProvider {
	@Autowired
	private UserProfileManager userProfileManager;

	@Override
	public OIDCClaimName getName() {
		return OIDCClaimName.validated_at;
	}

	@Override
	public String getDescription() {
		return "If you are a validated user, to see the date when your profile was validated";
	}

	@Override
	public Object getClaim(String userId, OIDCClaimsRequestDetails details) {
		VerificationSubmission verificationSubmission = userProfileManager.getCurrentVerificationSubmission(Long.parseLong(userId));
		Date approvalDate = VerificationHelper.getApprovalDate(verificationSubmission);
		if (approvalDate==null) {
			return null;
		} else {
			return approvalDate.getTime()/1000L;
		}
	}

}
