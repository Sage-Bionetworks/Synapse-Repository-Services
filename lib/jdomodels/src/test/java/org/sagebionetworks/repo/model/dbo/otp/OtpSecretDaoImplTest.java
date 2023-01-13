package org.sagebionetworks.repo.model.dbo.otp;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class OtpSecretDaoImplTest {
	
	@Autowired
	private OtpSecretDao dao;
		
	private Long userId;

	@BeforeEach
	public void before() {
		dao.truncateAll();
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	}
	
	@AfterEach
	public void after() {
		dao.truncateAll();
	}
	
	@Test
	public void testStoreSecret() {
		String secret = "my secret";
		
		DBOOtpSecret expected = new DBOOtpSecret();
		expected.setSecret(secret);
		expected.setActive(false);
		expected.setUserId(userId);
		
		// Call under test
		DBOOtpSecret result = dao.storeSecret(userId, secret);
		
		assertNotNull(result.getId());
		assertNotNull(result.getCreatedOn());
		assertNotNull(result.getEtag());
		
		expected.setId(result.getId());
		expected.setCreatedOn(result.getCreatedOn());
		expected.setEtag(result.getEtag());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testStoreSecretWithExistingActive() {
		String secret = "my secret";
		
		// First store and activate
		DBOOtpSecret existing = dao.activateSecret(userId, dao.storeSecret(userId, secret).getId());
		
		// Call under test
		DBOOtpSecret result = dao.storeSecret(userId, secret);
		
		assertNotEquals(existing, result);
		assertFalse(result.getActive());
		assertTrue(dao.getSecret(userId, existing.getId()).get().getActive());
	}
	
	@Test
	public void testStoreSecretWithExistingInactive() {
		String secret = "my secret";
		
		// First store one secret inactive
		DBOOtpSecret existing = dao.storeSecret(userId, secret);
		
		// Call under test
		DBOOtpSecret result = dao.storeSecret(userId, "new secret");
		
		assertNotEquals(existing, result);
		assertFalse(result.getActive());
		assertTrue(dao.getSecret(userId, existing.getId()).isEmpty());
	}
	
	@Test
	public void testActivateSecret() {
		String secret = "my secret";
		
		// First store one secret inactive
		DBOOtpSecret existing1 = dao.storeSecret(userId, secret);
		DBOOtpSecret existing2 = dao.storeSecret(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), "another secret");
		
		// Call under test
		DBOOtpSecret updated = dao.activateSecret(userId, existing1.getId());
		
		assertEquals(existing2, dao.getSecret(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), existing2.getId()).get());
				
		assertEquals(existing1.getUserId(), updated.getUserId());
		assertEquals(existing1.getCreatedOn(), updated.getCreatedOn());
		assertEquals(existing1.getSecret(), updated.getSecret());
		
		assertNotEquals(existing1.getEtag(), updated.getEtag());
		assertNotEquals(existing1.getActive(), updated.getActive());
	}
	
	@Test
	public void testActivateSecretWithExistingActive() {
		String secret = "my secret";
		
		dao.activateSecret(userId, dao.storeSecret(userId, secret).getId());
		DBOOtpSecret newSecret = dao.storeSecret(userId, "new secret");
		
		String result = assertThrows(IllegalStateException.class, () -> {
			// Call under test
			dao.activateSecret(userId, newSecret.getId());
		}).getMessage();
		
		assertEquals("An active secret already exists", result);
	}
	
	@Test
	public void testActivateSecretWithInvalidSecretId() {
		String secret = "my secret";
		
		// First store one secret inactive
		dao.storeSecret(userId, secret);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.activateSecret(userId, -1L);
		}).getMessage();
		
		assertEquals("Invalid secret id", result);
	}
	
	@Test
	public void testActivateSecretWithInvalidUserId() {
		String secret = "my secret";
		
		// First store one secret inactive
		DBOOtpSecret existing = dao.storeSecret(userId, secret);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.activateSecret(123L, existing.getId());
		}).getMessage();
		
		assertEquals("Invalid secret id", result);
	}
	
	@Test
	public void testGetSecret() {
		String secret = "my secret";
		
		// Call under test
		assertTrue(dao.getSecret(userId, 123L).isEmpty());
		
		DBOOtpSecret existing1 = dao.storeSecret(userId, secret);
		DBOOtpSecret existing2 = dao.storeSecret(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), "another secret");
		
		// Call under test
		assertEquals(existing1, dao.getSecret(userId, existing1.getId()).get());
		assertTrue(dao.getSecret(userId, existing2.getId()).isEmpty());
	}
	
	@Test
	public void testGetActiveSecret() {
		// Call under test
		assertTrue(dao.getActiveSecret(userId).isEmpty());

		String secret = "my secret";
				
		// First store one secret inactive
		DBOOtpSecret existing = dao.storeSecret(userId, secret);
		
		// Call under test
		assertTrue(dao.getActiveSecret(userId).isEmpty());
		
		dao.activateSecret(userId, existing.getId());
		
		// Call under test
		assertEquals(existing.getId(), dao.getActiveSecret(userId).get().getId());
		
	}
	
	@Test
	public void testHasActiveSecret() {
		String secret = "my secret";
		
		// First store one secret inactive
		DBOOtpSecret existing = dao.storeSecret(userId, secret);
				
		// Call under test
		assertFalse(dao.hasActiveSecret(userId));
		
		dao.activateSecret(userId, existing.getId());
		
		// Call under test
		assertTrue(dao.hasActiveSecret(userId));
		
	}
	
	@Test
	public void testDeleteSecret() {
		String secret = "my secret";
		
		DBOOtpSecret existing1 = dao.storeSecret(userId, secret);
		DBOOtpSecret existing2 = dao.storeSecret(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), "another secret");
		
		// Call under test
		dao.deleteSecret(userId, existing1.getId());
		
		assertTrue(dao.getSecret(userId, existing1.getId()).isEmpty());
		assertEquals(existing2, dao.getSecret(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), existing2.getId()).get());
	}
	
	@Test
	public void testDeleteSecretWithInvalidUserId() {
		String secret = "my secret";
		
		DBOOtpSecret existing1 = dao.storeSecret(userId, secret);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			dao.deleteSecret(123L, existing1.getId());
		}).getMessage();
		
		assertEquals("Invalid secret id", result);
	}
	
	@Test
	public void testDeleteSecretWithInvalidSecretId() {
		String secret = "my secret";
		
		dao.storeSecret(userId, secret);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			dao.deleteSecret(userId, -1L);
		}).getMessage();
		
		assertEquals("Invalid secret id", result);
	}
	
	@Test
	public void testDeleteSecrets() {
		String secret = "my secret";
		
		DBOOtpSecret existing1 = dao.storeSecret(userId, secret);
		DBOOtpSecret existing2 = dao.storeSecret(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), "another secret");
				
		// Call under test
		dao.deleteSecrets(userId);
		
		assertTrue(dao.getSecret(userId, existing1.getId()).isEmpty());
		assertEquals(existing2, dao.getSecret(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), existing2.getId()).get());
	}

}
