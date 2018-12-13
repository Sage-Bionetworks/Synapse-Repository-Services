package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationPropertiesImplTest {

	@Mock
	PropertyProvider mockPropertyProvider;
	@Mock
	AWSKMS mockAwsKeyManagerClient;
	@Mock
	AmazonS3 mockS3Client;
	@Mock
	LoggerProvider mockLoggerProvider;
	@Mock
	Logger mockLog;
	@Mock
	S3Object mockS3Object;

	@Captor
	ArgumentCaptor<DecryptRequest> decryptRequestCaprtor;

	ConfigurationPropertiesImpl configuration;

	Properties defaultProps;
	Properties settingProps;
	Properties systemProps;
	Properties secretProps;

	String cmkAlis;
	String keyToBeDecrypted;
	String encryptedValue;
	String base64EncodedCipher;
	String decryptedValue;
	
	String secretsBucket;
	String secretsKey;

	DecryptResult decryptResult;

	@Before
	public void before() throws IOException {
		
		when(mockLoggerProvider.getLogger(anyString())).thenReturn(mockLog);
		
		defaultProps = new Properties();
		defaultProps.setProperty("one", "1-default");
		defaultProps.setProperty("two", "2-default");
		defaultProps.setProperty("three", "3-default");
		defaultProps.setProperty("empty", "");

		settingProps = new Properties();
		settingProps.put("two", "2-settings");
		settingProps.put("three", "3-settings");
		settingProps.put("four", "4-settings");
		settingProps.put("five", "5-settings");

		systemProps = new Properties();
		systemProps.setProperty("two", "2-system");
		systemProps.setProperty("four", "4-system");
		systemProps.setProperty("six", "6-system");

		when(mockPropertyProvider.loadPropertiesFromClasspath(anyString())).thenReturn(defaultProps);
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(settingProps);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(systemProps);

		cmkAlis = "alias/test/foo";
		systemProps.setProperty(ConfigurationPropertiesImpl.PROPERTY_KEY_STACK_CMK_ALIAS, cmkAlis);

		// setup a base64 encoded cipher.
		keyToBeDecrypted = "toBeDecrypted";
		encryptedValue = "This is encrypted";
		base64EncodedCipher = base64Encode(encryptedValue);
		systemProps.setProperty(keyToBeDecrypted, base64EncodedCipher);

		decryptedValue = "The value decrypted";
		// setup the decrypted result
		decryptResult = new DecryptResult()
				.withPlaintext(ByteBuffer.wrap(decryptedValue.getBytes(ConfigurationPropertiesImpl.UTF_8)));
		when(mockAwsKeyManagerClient.decrypt(any(DecryptRequest.class))).thenReturn(decryptResult);
		
		secretsBucket = "aSecretBucket";
		secretsKey = "aSecretKey";
		systemProps.put(ConfigurationPropertiesImpl.ORG_SAGEBIONETWORKS_SECRETS_BUCKET, secretsBucket);
		systemProps.put(ConfigurationPropertiesImpl.ORG_SAGEBIONETWORKS_SECRETS_KEY, secretsKey);
		
		secretProps = new Properties();
		secretProps.put("secretOne", "cipherOne");
		secretProps.put("secretTwo", "cipherTwo");
		
		when(mockS3Client.doesObjectExist(secretsBucket, secretsKey)).thenReturn(true);
		setupSecretInputStream();
		when(mockS3Client.getObject(secretsBucket, secretsKey)).thenReturn(mockS3Object);

		configuration = new ConfigurationPropertiesImpl(mockAwsKeyManagerClient, mockS3Client, mockPropertyProvider, mockLoggerProvider);

		verify(mockPropertyProvider).getMavenSettingsProperties();
		verify(mockPropertyProvider).getSystemProperties();
		verify(mockPropertyProvider)
				.loadPropertiesFromClasspath(ConfigurationPropertiesImpl.DEFAULT_PROPERTIES_FILENAME);
	}

	void setupSecretInputStream() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		secretProps.store(baos, "no comment");
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		when(mockS3Object.getObjectContent()).thenReturn(new S3ObjectInputStream(bais, null));
	}

	@Test
	public void testGetProperty() {
		// calls under test
		assertEquals("1-default", configuration.getProperty("one"));
		assertEquals("2-system", configuration.getProperty("two"));
		assertEquals("3-settings", configuration.getProperty("three"));
		assertEquals("4-system", configuration.getProperty("four"));
		assertEquals("5-settings", configuration.getProperty("five"));
		assertEquals("6-system", configuration.getProperty("six"));
		// secrets should also be loaded
		assertEquals("cipherOne", configuration.getProperty("secretOne"));
		assertEquals("cipherTwo", configuration.getProperty("secretTwo"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetPropertyNullKey() {
		String key = null;
		// calls under test
		configuration.getProperty(key);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetPropertyDoesNotExist() {
		String key = "does not exist";
		// calls under test
		configuration.getProperty(key);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetPropertyEmptyValue() {
		String key = "empty";
		// calls under test
		configuration.getProperty(key);
	}

	/**
	 * Settings will be null for a production stack.
	 * 
	 */
	@Test
	public void testNullSettings() {
		settingProps = null;
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(settingProps);
		configuration.initialize();

		assertEquals("1-default", configuration.getProperty("one"));
		assertEquals("2-system", configuration.getProperty("two"));
		assertEquals("4-system", configuration.getProperty("four"));
		assertEquals("6-system", configuration.getProperty("six"));
	}

	@Test
	public void testGetDecryptedProperty() throws UnsupportedEncodingException {
		// call under test
		String results = configuration.getDecryptedProperty(keyToBeDecrypted);
		assertEquals(decryptedValue, results);
		verify(mockAwsKeyManagerClient).decrypt(decryptRequestCaprtor.capture());
		DecryptRequest request = decryptRequestCaprtor.getValue();
		assertNotNull(request);
		assertNotNull(request.getCiphertextBlob());
		String cipherString = ConfigurationPropertiesImpl.byteBuferToString(request.getCiphertextBlob());
		assertEquals(encryptedValue, cipherString);
		verify(mockLog).info("Decrypting property 'toBeDecrypted'...");
	}

	
	@Test
	public void testGetDecryptedPropertyNoAlias() throws UnsupportedEncodingException {
		// Remove the alis
		systemProps.remove(ConfigurationPropertiesImpl.PROPERTY_KEY_STACK_CMK_ALIAS);
		configuration.initialize();
		
		// call under test
		String results = configuration.getDecryptedProperty(keyToBeDecrypted);
		// the value should not be modified in any way.
		assertEquals(base64EncodedCipher, results);
		// the value should not be decrypted
		verify(mockAwsKeyManagerClient, never()).decrypt(decryptRequestCaprtor.capture());
		verify(mockLog).warn("Property: 'org.sagebionetworks.stack.cmk.alias' does not exist so the value of 'toBeDecrypted' will not be decrypted.");
	}
	
	
	@Test
	public void testGetDecryptedPropertyNullKey() throws UnsupportedEncodingException {
		try {
			String key = null;
			// call under test
			configuration.getDecryptedProperty(key);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Property key cannot be null", e.getMessage());
		}
	}

	@Test
	public void testGetDecryptedPropertyUnknown() throws UnsupportedEncodingException {
		try {
			String key = "unknown";
			// call under test
			configuration.getDecryptedProperty(key);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Property with key: 'unknown' does not exist.", e.getMessage());
		}
	}
	
	@Test
	public void testLoadSecrets() throws IOException {
		setupSecretInputStream();
		// call under test
		Properties propSecrets = configuration.loadSecrets(secretsBucket, secretsKey);
		assertNotNull(propSecrets);
		assertEquals("cipherOne", propSecrets.getProperty("secretOne"));
		assertEquals("cipherTwo", propSecrets.getProperty("secretTwo"));
		verify(mockS3Client, times(2)).doesObjectExist(secretsBucket, secretsKey);
		verify(mockS3Client, times(2)).getObject(secretsBucket, secretsKey);
		verify(mockLog, times(2)).info("Loaded 2 secrets from: aSecretBucket/aSecretKey");
	}
	
	@Test
	public void testLoadSecretsKeyNull() throws IOException {
		setupSecretInputStream();
		secretsKey = null;
		// call under test
		Properties propSecrets = configuration.loadSecrets(secretsBucket, secretsKey);
		assertEquals(null, propSecrets);
		verify(mockS3Client, times(1)).doesObjectExist(anyString(), anyString());
		verify(mockS3Client, times(1)).getObject(anyString(), anyString());
		verify(mockLog).warn(ConfigurationPropertiesImpl.SECRETS_WERE_NOT_LOADED_FROM_S3);
	}
	
	@Test
	public void testLoadSecretsBucketNull() throws IOException {
		setupSecretInputStream();
		secretsBucket = null;
		// call under test
		Properties propSecrets = configuration.loadSecrets(secretsBucket, secretsKey);
		assertEquals(null, propSecrets);
		verify(mockS3Client, times(1)).doesObjectExist(anyString(), anyString());
		verify(mockS3Client, times(1)).getObject(anyString(), anyString());
		verify(mockLog).warn(ConfigurationPropertiesImpl.SECRETS_WERE_NOT_LOADED_FROM_S3);
	}

	@Test
	public void testLoadSecretsDoesNotExist() throws IOException {
		setupSecretInputStream();
		when(mockS3Client.doesObjectExist(secretsBucket, secretsKey)).thenReturn(false);
		// call under test
		Properties propSecrets = configuration.loadSecrets(secretsBucket, secretsKey);
		assertEquals(null, propSecrets);
		verify(mockS3Client, times(2)).doesObjectExist(anyString(), anyString());
		verify(mockS3Client, times(1)).getObject(anyString(), anyString());
		verify(mockLog).warn(ConfigurationPropertiesImpl.SECRETS_WERE_NOT_LOADED_FROM_S3);
		verify(mockLog).warn("S3 Object does not exist with bucket: 'aSecretBucket' and key: 'aSecretKey'");
	}
	
	/**
	 * Helper to create an base 64 encoded string.
	 * 
	 * @param input
	 * @return
	 */
	public String base64Encode(String input) {
		try {
			byte[] bytes = input.getBytes(ConfigurationPropertiesImpl.UTF_8);
			bytes = Base64.getEncoder().encode(bytes);
			return new String(bytes, ConfigurationPropertiesImpl.UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

	}
}
