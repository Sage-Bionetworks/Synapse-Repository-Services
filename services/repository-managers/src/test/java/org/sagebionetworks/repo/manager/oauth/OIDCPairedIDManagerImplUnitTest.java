package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.securitytools.EncryptionUtils;

@RunWith(MockitoJUnitRunner.class)
public class OIDCPairedIDManagerImplUnitTest {
	private static final String USER_ID = "101";
	private static final String OAUTH_CLIENT_ID = "123";

	private String clientSpecificEncodingSecret;

	@Mock
	private OAuthClientDao mockOauthClientDao;
	
	@InjectMocks
	private OIDCPairedIDManagerImpl oidcPairedIDManagerImpl;


	@Before
	public void setUp() throws Exception {
		clientSpecificEncodingSecret = EncryptionUtils.newSecretKey();
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
	}
	
	@Test
	public void testPPID() {
		// method under test
		String ppid = oidcPairedIDManagerImpl.getPPIDFromUserId(USER_ID, OAUTH_CLIENT_ID);
		assertEquals(USER_ID, EncryptionUtils.decrypt(ppid, clientSpecificEncodingSecret));
	}

	@Test
	public void testPPIDForSynapse() {
		// method under test
		String ppid = oidcPairedIDManagerImpl.getPPIDFromUserId(USER_ID, OAuthClientManager.SYNAPSE_OAUTH_CLIENT_ID);
		assertEquals(USER_ID, ppid);
	}

	@Test
	public void testGetUserIdFromPPID() {
		String ppid = oidcPairedIDManagerImpl.getPPIDFromUserId(USER_ID, OAUTH_CLIENT_ID);
		
		// method under test		
		assertEquals(USER_ID, oidcPairedIDManagerImpl.getUserIdFromPPID(ppid, OAUTH_CLIENT_ID));
	}

	@Test
	public void testGetUserIdFromPPIDForSynapse() {
		// method under test		
		assertEquals(USER_ID, oidcPairedIDManagerImpl.getUserIdFromPPID(USER_ID, OAuthClientManager.SYNAPSE_OAUTH_CLIENT_ID));
	}


}
