package org.sagebionetworks;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationPropertiesImplTest {

	@Mock
	PropertyProvider mockPropertyProvider;
	@Mock
	AWSKMS mockAwsKeyManagerClient;
	@Mock
	Logger mockLog;

	@Captor
	ArgumentCaptor<DecryptRequest> decryptRequestCaprtor;

	ConfigurationPropertiesImpl configuration;

	Properties defaultProps;
	Properties settingProps;
	Properties systemProps;

	String cmkAlis;
	String keyToBeDecrypted;
	String encryptedValue;
	String base64EncodedCipher;
	String decryptedValue;

	DecryptResult decryptResult;

	@Before
	public void before() throws UnsupportedEncodingException {
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

		configuration = new ConfigurationPropertiesImpl(mockAwsKeyManagerClient, mockPropertyProvider, mockLog);

		verify(mockPropertyProvider).getMavenSettingsProperties();
		verify(mockPropertyProvider).getSystemProperties();
		verify(mockPropertyProvider)
				.loadPropertiesFromClasspath(ConfigurationPropertiesImpl.DEFAULT_PROPERTIES_FILENAME);
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
		configuration = new ConfigurationPropertiesImpl(mockAwsKeyManagerClient, mockPropertyProvider, mockLog);

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
		configuration = new ConfigurationPropertiesImpl(mockAwsKeyManagerClient, mockPropertyProvider, mockLog);
		
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
