package org.sagebionetworks.auth.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;

@RunWith(MockitoJUnitRunner.class)
public class OpenIDConnectServiceImplTest {
	
	@InjectMocks
	private OpenIDConnectServiceImpl oidcServiceImpl;
	
	
	@Mock 
	private OIDCTokenHelper oidcTokenHelper;

	@Test
	public void testGetOIDCJsonWebKeySet() throws Exception {
		JsonWebKeySet jwks = new JsonWebKeySet();
		
		// method under test
		oidcServiceImpl.getOIDCJsonWebKeySet();
		
		verify(oidcTokenHelper).getJSONWebKeySet();
	}

}
