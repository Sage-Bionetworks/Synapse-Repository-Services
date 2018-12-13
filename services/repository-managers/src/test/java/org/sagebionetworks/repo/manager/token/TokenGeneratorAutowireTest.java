package org.sagebionetworks.repo.manager.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.auth.NewUserSignedToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TokenGeneratorAutowireTest {

	@Autowired
	TokenGenerator tokenGenerator;
	@Autowired
	StackConfiguration stackConfiguration;
	
	@Test
	public void testSignAndValidate() {
		NewUserSignedToken token = new NewUserSignedToken();
		token.setEmail("email@company.org");
		token.setFirstName("first");
		token.setLastName("last");
		// Call under test
		tokenGenerator.signToken(token);
		assertNotNull(token.getVersion());
		assertNotNull(token.getExpiresOn());
		assertEquals(new Long(stackConfiguration.getCurrentHmacSigningKeyVersion()), token.getVersion());
		assertNotNull(token.getHmac());
		// call under test
		tokenGenerator.validateToken(token);
	}
}
