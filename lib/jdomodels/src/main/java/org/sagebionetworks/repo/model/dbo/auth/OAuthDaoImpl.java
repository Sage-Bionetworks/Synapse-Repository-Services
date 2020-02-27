package org.sagebionetworks.repo.model.dbo.auth;

import org.sagebionetworks.repo.model.auth.OAuthDao;

public class OAuthDaoImpl implements OAuthDao {

	@Override
	public void saveAuthorizationConsent(Long userId, String clientId, String scope, String claims) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean lookupAuthorizationConsent(Long userId, String clientId, String scope, String claims) {
		// TODO Auto-generated method stub
		return false;
	}

}
