package org.sagebionetworks.repo.manager.oauth.claimprovider;

import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.springframework.beans.factory.annotation.Autowired;

public class EmailClaimProvider implements OIDCClaimProvider {
	@Autowired
	private NotificationEmailDAO notificationEmailDao;
	

	@Override
	public OIDCClaimName getName() {
		return OIDCClaimName.email;
	}

	@Override
	public String getDescription() {
		return "To see your primary email address";
	}

	@Override
	public Object getClaim(String userId, OIDCClaimsRequestDetails details) {
		return notificationEmailDao.getNotificationEmailForPrincipal(Long.parseLong(userId));
	}

}
