package org.sagebionetworks.auth.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;

@ExtendWith(MockitoExtension.class)
public class StackConfigKeyAndSecretProviderTest {

	@Mock
	private StackConfiguration mockConfig;
	
	private StackConfigKeyAndSecretProvider provider;
	
	private static final String SERVICE_NAME = "service";
	private static final String KEY = "key";
	private static final String SECRET = "secret";
	
	@BeforeEach
	public void before() {
		when(mockConfig.getServiceAuthKey(any())).thenReturn(KEY);
		when(mockConfig.getServiceAuthSecret(any())).thenReturn(SECRET);
		
		provider = new StackConfigKeyAndSecretProvider(mockConfig, SERVICE_NAME);
		
		verify(mockConfig).getServiceAuthKey(SERVICE_NAME);
		verify(mockConfig).getServiceAuthSecret(SERVICE_NAME);

		assertNotEquals(KEY, provider.getServiceKeyHash());
		assertNotEquals(SECRET, provider.getServiceSecretHash());
	}
	
	@Test
	public void testGetServiceName() {
		assertEquals(SERVICE_NAME, provider.getServiceName());
	}
	
	@Test
	public void testValidateWithNullOrEmptyKey() {
		
		// Call under test
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			provider.validate(null, SECRET);
		}).getMessage();
		
		assertEquals("key is required and must not be the empty string.", errorMessage);
		
		errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			provider.validate("", SECRET);
		}).getMessage();
		
		assertEquals("key is required and must not be the empty string.", errorMessage);
		
		errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			provider.validate(" ", SECRET);
		}).getMessage();
		
		assertEquals("key is required and must not be a blank string.", errorMessage);
	}
	
	@Test
	public void testValidateWithNullOrEmptySecret() {
		
		// Call under test
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			provider.validate(KEY, null);
		}).getMessage();
		
		assertEquals("secret is required and must not be the empty string.", errorMessage);
		
		errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			provider.validate(KEY, "");
		}).getMessage();
		
		assertEquals("secret is required and must not be the empty string.", errorMessage);
		
		errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			provider.validate(KEY, " ");
		}).getMessage();
		
		assertEquals("secret is required and must not be a blank string.", errorMessage);
	}
	
	@Test
	public void testValidateWithInvalidKey() {
		assertFalse(provider.validate(KEY + "_invalid",  SECRET));
	}
	
	@Test
	public void testValidateWithInvalidSecret() {
		assertFalse(provider.validate(KEY, SECRET + "_invalid"));
	}
	
	@Test
	public void testValidateWithValidCredentials() {
		assertTrue(provider.validate(KEY, SECRET));
	}
	
	@Test
	public void testHashSecretWithoutSaltContainer() {
		String result = StackConfigKeyAndSecretProvider.hashSecret(SECRET, null);
		
		// Hashing a second time should use a different salt
		assertNotEquals(result, StackConfigKeyAndSecretProvider.hashSecret(SECRET, null));
	}
	
	@Test
	public void testHashSecretWithSaltContainer() {
		String hash = StackConfigKeyAndSecretProvider.hashSecret(SECRET, null);
		
		// Call under test
		String result = StackConfigKeyAndSecretProvider.hashSecret(SECRET, hash);

		assertEquals(hash, result);
	}
}
