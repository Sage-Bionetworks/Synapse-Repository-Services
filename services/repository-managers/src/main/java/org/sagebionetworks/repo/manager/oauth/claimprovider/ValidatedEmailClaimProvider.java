package org.sagebionetworks.repo.manager.oauth.claimprovider;

import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.verification.VerificationHelper;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.springframework.beans.factory.annotation.Autowired;

public class ValidatedEmailClaimProvider implements OIDCClaimProvider {
	@Autowired
	private UserProfileManager userProfileManager;

	@Autowired
	private NotificationEmailDAO notificationEmailDao;
	
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
		if (verificationSubmission==null || !VerificationHelper.isVerified(verificationSubmission)) {
			return null;
		}
		if (verificationSubmission.getNotificationEmail()!=null) {
			return verificationSubmission.getNotificationEmail();
		}	
			
		// For legacy verification submissions there will be no 'notification email'.  Normally a user
		// has just one registered email address, so we can simply return the singleton value from the captured list of emails.
		if (verificationSubmission.getEmails().size()==1) {
			return verificationSubmission.getEmails().get(0);
		}
		// most of the remaining can be disambiguated by the current notification email:
		String currentNotificationEmail = notificationEmailDao.getNotificationEmailForPrincipal(Long.parseLong(userId));
		if (verificationSubmission.getEmails().contains(currentNotificationEmail)) {
			return currentNotificationEmail;
		}
		
		// For a tiny number we'll just return the first of the list
		return verificationSubmission.getEmails().get(0);

	}

}
