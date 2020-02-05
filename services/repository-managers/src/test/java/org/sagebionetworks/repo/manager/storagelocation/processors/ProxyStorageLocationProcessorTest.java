package org.sagebionetworks.repo.manager.storagelocation.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

@ExtendWith(MockitoExtension.class)
public class ProxyStorageLocationProcessorTest {

	@InjectMocks
	private ProxyStorageLocationProcessor processor;

	@Mock
	private ProxyStorageLocationSettings mockStorageLocation;
	
	@Mock
	private StorageLocationSetting mockUnsupportedStorageLocation;

	@Mock
	private UserInfo mockUserInfo;

	@Test
	public void testSupports() {
		assertTrue(processor.supports(mockStorageLocation.getClass()));
	}
	
	@Test
	public void testSupportsFalse() {
		assertFalse(processor.supports(mockUnsupportedStorageLocation.getClass()));
	}

	@Test
	public void testBeforeCreate() {
		String proxyUrl = "https://example.com";
		String secretKey = RandomStringUtils.randomAlphabetic(36);

		when(mockStorageLocation.getProxyUrl()).thenReturn(proxyUrl);
		when(mockStorageLocation.getSecretKey()).thenReturn(secretKey);

		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);
	}

	@Test
	public void testBeforeCreateWithNullProxyUrl() {
		String proxyUrl = null;

		when(mockStorageLocation.getProxyUrl()).thenReturn(proxyUrl);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			processor.beforeCreate(mockUserInfo, mockStorageLocation);
		});

		assertEquals("The proxyUrl is required.", ex.getMessage());
	}

	@Test
	public void testBeforeCreateWithNullSecret() {
		String proxyUrl = "https://example.com";
		String secretKey = null;

		when(mockStorageLocation.getProxyUrl()).thenReturn(proxyUrl);
		when(mockStorageLocation.getSecretKey()).thenReturn(secretKey);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			processor.beforeCreate(mockUserInfo, mockStorageLocation);
		});

		assertEquals("The secretKey is required.", ex.getMessage());
	}

	@Test
	public void testBeforeCreateWithSecretTooShort() {
		String proxyUrl = "https://example.com";
		String secretKey = "shortSecret";

		when(mockStorageLocation.getProxyUrl()).thenReturn(proxyUrl);
		when(mockStorageLocation.getSecretKey()).thenReturn(secretKey);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			processor.beforeCreate(mockUserInfo, mockStorageLocation);
		});

		assertEquals("SecretKey must be at least: 36 characters but was: 11", ex.getMessage());
	}

	@Test
	public void testBeforeCreateWithInvalidProxyUrl() {
		String proxyUrl = "invalid url";
		String secretKey = RandomStringUtils.randomAlphabetic(36);

		when(mockStorageLocation.getProxyUrl()).thenReturn(proxyUrl);
		when(mockStorageLocation.getSecretKey()).thenReturn(secretKey);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			processor.beforeCreate(mockUserInfo, mockStorageLocation);
		});

		assertTrue(ex.getMessage().startsWith("The proxyUrl is malformed: "));
	}

	@Test
	public void testBeforeCreateWithNoHttps() {
		String proxyUrl = "ftp://example.com";
		String secretKey = RandomStringUtils.randomAlphabetic(36);

		when(mockStorageLocation.getProxyUrl()).thenReturn(proxyUrl);
		when(mockStorageLocation.getSecretKey()).thenReturn(secretKey);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			processor.beforeCreate(mockUserInfo, mockStorageLocation);
		});

		assertEquals("The proxyUrl protocol must be be HTTPS", ex.getMessage());
	}

}
