package org.sagebionetworks.auth.services;

import static org.junit.Assert.assertFalse;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;

class OpenIDConnectServiceImplTest {
	
	private  OpenIDConnectServiceImpl oidcServiceImpl = new OpenIDConnectServiceImpl();

	@Test
	void testGetOIDCJsonWebKeySet() throws Exception {
		JsonWebKeySet jwks = oidcServiceImpl.getOIDCJsonWebKeySet();
		assertFalse(jwks.getKeys().isEmpty());
		// TODO check that JWKS can validate a signed token
	}

}
