package org.sagebionetworks.repo.manager.oauth;

import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.securitytools.EncryptionUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class OIDCOPairedIDManagerImpl implements OIDCPairedIDManager {

	@Autowired
	private OAuthClientDao oauthClientDao;

	// As per, https://openid.net/specs/openid-connect-core-1_0.html#PairwiseAlg
	@Override
	public String getPPIDFromUserId(String userId, String clientId) {
		if (OAuthClientManager.SYNAPSE_OAUTH_CLIENT_ID.equals(clientId)) {
			return userId;
		}
		String sectorIdentifierSecret = oauthClientDao.getSectorIdentifierSecretForClient(clientId);
		return EncryptionUtils.encrypt(userId, sectorIdentifierSecret);
	}

	@Override
	public String getUserIdFromPPID(String ppid, String clientId) {
		if (OAuthClientManager.SYNAPSE_OAUTH_CLIENT_ID.equals(clientId)) {
			return ppid;
		}
		String sectorIdentifierSecret = oauthClientDao.getSectorIdentifierSecretForClient(clientId);
		return EncryptionUtils.decrypt(ppid, sectorIdentifierSecret);
	}
	


}
