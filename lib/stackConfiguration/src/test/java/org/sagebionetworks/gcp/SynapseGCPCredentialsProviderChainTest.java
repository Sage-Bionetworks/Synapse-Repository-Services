package org.sagebionetworks.gcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.gcp.AbstractSynapseGcpCredentialsProvider.ORG_SAGEBIONETWORKS_STACK_GCP_SVC_ACCOUNT_KEY;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sagebionetworks.PropertyProvider;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SynapseGCPCredentialsProviderChainTest {

	@Mock
	PropertyProvider mockPropertyProvider;

	@Mock
	CredentialsProvider mockDefaultProvider;

	Properties settings;
	Properties system;

	private static final String PROJECT_ID = "project-id";
	private static final String PRIVATE_KEY_ID = "abcdef1237959ce2f3b625ea5dadd144d5e20512";
	private static final String PRIVATE_KEY =  // This must be a well-formatted PKCS8 key.
			"-----BEGIN PRIVATE KEY-----\\n"
			+ "MIIBVgIBADANBgkqhkiG9w0BAQEFAASCAUAwggE8AgEAAkEAq7BFUpkGp3+LQmlQ\\n"
			+ "Yx2eqzDV+xeG8kx/sQFV18S5JhzGeIJNA72wSeukEPojtqUyX2J0CciPBh7eqclQ\\n"
			+ "2zpAswIDAQABAkAgisq4+zRdrzkwH1ITV1vpytnkO/NiHcnePQiOW0VUybPyHoGM\\n"
			+ "/jf75C5xET7ZQpBe5kx5VHsPZj0CBb3b+wSRAiEA2mPWCBytosIU/ODRfq6EiV04\\n"
			+ "lt6waE7I2uSPqIC20LcCIQDJQYIHQII+3YaPqyhGgqMexuuuGx+lDKD6/Fu/JwPb\\n"
		 	+ "5QIhAKthiYcYKlL9h8bjDsQhZDUACPasjzdsDEdq8inDyLOFAiEAmCr/tZwA3qeA\\n"
			+ "ZoBzI10DGPIuoKXBd3nk/eBxPkaxlEECIQCNymjsoI7GldtujVnr1qT+3yedLfHK\\n"
			+ "srDVjIT3LsvTqw==\\n"
			+ "-----END PRIVATE KEY-----";
	private static final String CLIENT_ID = "100";


	// We can use the email field to discern between configurations.
	private static final String EMAIL_FOR_DEFAULT = "default@gserviceaccount.com";
	private static final String EMAIL_FOR_SETTINGS = "settings@gserviceaccount.com";
	private static final String EMAIL_FOR_SYSTEM = "system@gserviceaccount.com";

	private static final String getSecret(String email) {
		return 	"{\n" +
				"  \"type\": \"service_account\",\n" +
				"  \"project_id\": \"" + PROJECT_ID + "\",\n" +
				"  \"private_key_id\": \"" + PRIVATE_KEY_ID + "\",\n" +
				"  \"private_key\": \"" + PRIVATE_KEY + "\",\n" +
				"  \"client_email\": \"" + email + "\",\n" +
				"  \"client_id\": \"" + CLIENT_ID + "\"\n" +
				"}";
	}

	@BeforeEach
	public void before() throws Exception {
		//setup settings
		settings = new Properties();
		settings.put(ORG_SAGEBIONETWORKS_STACK_GCP_SVC_ACCOUNT_KEY, getSecret(EMAIL_FOR_SETTINGS));

		// setup system
		system = new Properties();
		system.put(ORG_SAGEBIONETWORKS_STACK_GCP_SVC_ACCOUNT_KEY, getSecret(EMAIL_FOR_SYSTEM));
		
		when(mockDefaultProvider.getCredentials())
				.thenReturn(ServiceAccountCredentials.fromStream(
						new ByteArrayInputStream(getSecret(EMAIL_FOR_DEFAULT)
								.getBytes(StandardCharsets.UTF_8))));
	}

	/**
	 * Case where settings exists with valid values.
	 */
	@Test
	public void testChainWithSettings() throws Exception {
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(settings);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(null);
		
		SynapseGCPCredentialsProviderChain chain = createChain();
		// call under test
		ServiceAccountCredentials creds = (ServiceAccountCredentials) chain.getCredentials();
		assertNotNull(creds);
		assertEquals(PROJECT_ID, creds.getProjectId());
		assertEquals(PRIVATE_KEY_ID, creds.getPrivateKeyId());
		assertEquals(EMAIL_FOR_SETTINGS, creds.getClientEmail());
		assertEquals(CLIENT_ID, creds.getClientId());
		assertNotNull(creds.getPrivateKey());

		// default provider should not be invoked
		verify(mockDefaultProvider, never()).getCredentials();
	}

	/**
	 * Case where system exists with valid values.
	 */
	@Test
	public void testChainWithSystem() throws Exception {
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(null);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(system);

		SynapseGCPCredentialsProviderChain chain = createChain();
		// call under test
		ServiceAccountCredentials creds = (ServiceAccountCredentials) chain.getCredentials();
		assertNotNull(creds);
		assertEquals(PROJECT_ID, creds.getProjectId());
		assertEquals(PRIVATE_KEY_ID, creds.getPrivateKeyId());
		assertEquals(EMAIL_FOR_SYSTEM, creds.getClientEmail());
		assertEquals(CLIENT_ID, creds.getClientId());
		assertNotNull(creds.getPrivateKey());
		// default provider should not be invoked
		verify(mockDefaultProvider, never()).getCredentials();
	}

	/**
	 * Case where the default provider creates the credentials.
	 */
	@Test
	public void testChainWithDefault() throws Exception {
		// both system and settings are null
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(null);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(null);

		SynapseGCPCredentialsProviderChain chain = createChain();
		// call under test
		ServiceAccountCredentials creds = (ServiceAccountCredentials) chain.getCredentials();
		assertNotNull(creds);
		assertEquals(PROJECT_ID, creds.getProjectId());
		assertEquals(PRIVATE_KEY_ID, creds.getPrivateKeyId());
		assertEquals(EMAIL_FOR_DEFAULT, creds.getClientEmail());
		assertEquals(CLIENT_ID, creds.getClientId());
		assertNotNull(creds.getPrivateKey());
		// default provider should be invoked
		verify(mockDefaultProvider, times(1)).getCredentials();
	}

	/**
	 * Case settings exists but does not contain the keys. Should load default
	 */
	@Test
	public void testChainWithSettingsMissingValues() throws Exception {
		settings.clear();
		// both system and settings are null
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(settings);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(null);

		SynapseGCPCredentialsProviderChain chain = createChain();
		// call under test
		ServiceAccountCredentials creds = (ServiceAccountCredentials) chain.getCredentials();
		assertNotNull(creds);
		assertEquals(PROJECT_ID, creds.getProjectId());
		assertEquals(PRIVATE_KEY_ID, creds.getPrivateKeyId());
		assertEquals(EMAIL_FOR_DEFAULT, creds.getClientEmail());
		assertEquals(CLIENT_ID, creds.getClientId());
		assertNotNull(creds.getPrivateKey());
		// default provider should be invoked
		verify(mockDefaultProvider, times(1)).getCredentials();
	}

	/**
	 * Case settings exists but is missing the id;
	 *
	 */
	@Test
	public void testChainWithSettingsMissingSecret() throws Exception {
		settings.remove(ORG_SAGEBIONETWORKS_STACK_GCP_SVC_ACCOUNT_KEY);
		// both system and settings are null
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(settings);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(null);

		SynapseGCPCredentialsProviderChain chain = createChain();
		// call under test
		ServiceAccountCredentials creds = (ServiceAccountCredentials) chain.getCredentials();
		assertNotNull(creds);
		assertEquals(PROJECT_ID, creds.getProjectId());
		assertEquals(PRIVATE_KEY_ID, creds.getPrivateKeyId());
		assertEquals(EMAIL_FOR_DEFAULT, creds.getClientEmail());
		assertEquals(CLIENT_ID, creds.getClientId());
		assertNotNull(creds.getPrivateKey());
		// default provider should be used
		verify(mockDefaultProvider, times(1)).getCredentials();
	}

	/**
	 * Case settings exists but is missing the key;
	 *
	 */
	@Test
	public void testChainWithSettingsMissingKey() throws Exception {
		settings.remove(ORG_SAGEBIONETWORKS_STACK_GCP_SVC_ACCOUNT_KEY);
		// both system and settings are null
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(settings);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(null);

		SynapseGCPCredentialsProviderChain chain = createChain();
		// call under test
		ServiceAccountCredentials creds = (ServiceAccountCredentials) chain.getCredentials();
		assertNotNull(creds);
		assertEquals(PROJECT_ID, creds.getProjectId());
		assertEquals(PRIVATE_KEY_ID, creds.getPrivateKeyId());
		assertEquals(EMAIL_FOR_DEFAULT, creds.getClientEmail());
		assertEquals(CLIENT_ID, creds.getClientId());
		assertNotNull(creds.getPrivateKey());
		// default provider should be invoked
		verify(mockDefaultProvider, times(1)).getCredentials();
	}

	/**
	 * Case system exists but does not contain the keys.
	 *
	 */
	@Test
	public void testChainWithSystemMissingValues() throws Exception {
		system.clear();
		// both system and settings are null
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(null);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(system);

		SynapseGCPCredentialsProviderChain chain = createChain();
		// call under test
		ServiceAccountCredentials creds = (ServiceAccountCredentials) chain.getCredentials();
		assertNotNull(creds);
		assertEquals(PROJECT_ID, creds.getProjectId());
		assertEquals(PRIVATE_KEY_ID, creds.getPrivateKeyId());
		assertEquals(EMAIL_FOR_DEFAULT, creds.getClientEmail());
		assertEquals(CLIENT_ID, creds.getClientId());
		assertNotNull(creds.getPrivateKey());
		// default provider should be invoked
		verify(mockDefaultProvider, times(1)).getCredentials();
	}

	/**
	 * Case system exists but does not contain the ID.
	 */
	@Test
	public void testChainWithSystemMissingId() throws Exception {
		system.remove(ORG_SAGEBIONETWORKS_STACK_GCP_SVC_ACCOUNT_KEY);
		// both system and settings are null
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(null);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(system);

		SynapseGCPCredentialsProviderChain chain = createChain();
		// call under test
		ServiceAccountCredentials creds = (ServiceAccountCredentials) chain.getCredentials();
		assertNotNull(creds);
		assertEquals(PROJECT_ID, creds.getProjectId());
		assertEquals(PRIVATE_KEY_ID, creds.getPrivateKeyId());
		assertEquals(EMAIL_FOR_DEFAULT, creds.getClientEmail());
		assertEquals(CLIENT_ID, creds.getClientId());
		assertNotNull(creds.getPrivateKey());
		// default provider should be invoked
		verify(mockDefaultProvider, times(1)).getCredentials();
	}

	/**
	 * Case system exists but does not contain the keys.
	 */
	@Test
	public void testChainWithSystemMissingKey() throws Exception {
		system.remove(ORG_SAGEBIONETWORKS_STACK_GCP_SVC_ACCOUNT_KEY);
		// both system and settings are null
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(null);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(system);

		SynapseGCPCredentialsProviderChain chain = createChain();
		// call under test
		ServiceAccountCredentials creds = (ServiceAccountCredentials) chain.getCredentials();
		assertNotNull(creds);
		assertEquals(PROJECT_ID, creds.getProjectId());
		assertEquals(PRIVATE_KEY_ID, creds.getPrivateKeyId());
		assertEquals(EMAIL_FOR_DEFAULT, creds.getClientEmail());
		assertEquals(CLIENT_ID, creds.getClientId());
		assertNotNull(creds.getPrivateKey());
		// default provider should be invoked
		verify(mockDefaultProvider, times(1)).getCredentials();
	}

	@Test
	public void testGetCredentialsWithException() throws Exception {
		system.remove(ORG_SAGEBIONETWORKS_STACK_GCP_SVC_ACCOUNT_KEY);
		settings.remove(ORG_SAGEBIONETWORKS_STACK_GCP_SVC_ACCOUNT_KEY);
		when(mockDefaultProvider.getCredentials()).thenReturn(null);
		SynapseGCPCredentialsProviderChain chain = createChain();
		assertThrows(RuntimeException.class, chain::getCredentials);
	}

	/**
	 * Create a new chain.
	 * @return
	 */
	SynapseGCPCredentialsProviderChain createChain() {
		return new SynapseGCPCredentialsProviderChain(mockPropertyProvider, mockDefaultProvider);
	}
}
