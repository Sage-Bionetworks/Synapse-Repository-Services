package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class OAuthManagerImplTest {

	@Autowired
	private OAuthManager oAuthManager;
	
	@Test
	public void testGoogleBinding() {
		OAuthProviderBinding binding = oAuthManager.getBinding(OAuthProvider.GOOGLE_OAUTH_2_0);
		assertNotNull(binding);
		assertNotNull(binding.getAuthorizationUrl("redirectUrl"));
	}
	
	@Test
	public void testORCIDBinding() {
		OAuthProviderBinding binding = oAuthManager.getBinding(OAuthProvider.ORCID);
		assertNotNull(binding);
		assertNotNull(binding.getAuthorizationUrl("redirectUrl"));
	}

}
