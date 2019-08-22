package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Test;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;


@RunWith(MockitoJUnitRunner.class)
class StackEncrypterImplTest {

	private StackEncrypterImpl stackEncrypterImpl;

	@Mock
	private ConfigurationProperties mockConfigurationProperties;
	@Mock
	private AWSKMS mockAWSKMS;
	@Mock
	private LoggerProvider mockLoggerProvider;
	@Mock
	private Logger mockLog;

	@Captor
	private ArgumentCaptor<DecryptRequest> decryptRequestCaptor;

	private String cmkAlias;
	private String keyToBeDecrypted;
	private String encryptedValue;
	private String base64EncodedCipher;
	private String decryptedValue;

	

	@Before
	public void setUp() {
		stackEncrypterImpl = new StackEncrypterImpl(mockConfigurationProperties, mockAWSKMS, mockLoggerProvider);
		cmkAlias = "alias/test/foo";

		keyToBeDecrypted = "toBeDecrypted";
		encryptedValue = "This is encrypted";
		base64EncodedCipher = base64Encode(encryptedValue);
		
		when(mockLoggerProvider.getLogger(anyString())).thenReturn(mockLog);

		when(mockConfigurationProperties.getProperty(StackEncrypterImpl.PROPERTY_KEY_STACK_CMK_ALIAS)).thenReturn(cmkAlias);
	}
	
	
	@Test
	public void testGetDecryptedProperty() throws UnsupportedEncodingException {
		// call under test
		String results = stackEncrypterImpl.getDecryptedProperty(keyToBeDecrypted);
		assertEquals(decryptedValue, results);
		verify(mockAWSKMS).decrypt(decryptRequestCaptor.capture());
		DecryptRequest request = decryptRequestCaptor.getValue();
		assertNotNull(request);
		assertEquals(encryptedValue, new String(request.getCiphertextBlob().array()));
		verify(mockLog).info("Decrypting property 'toBeDecrypted'...");
	}

	
	@Test
	public void testGetDecryptedPropertyNoAlias() throws UnsupportedEncodingException {
		// Remove the alias
		when(mockConfigurationProperties.getProperty(StackEncrypterImpl.PROPERTY_KEY_STACK_CMK_ALIAS)).thenReturn(null);
		
		// call under test
		String results = stackEncrypterImpl.getDecryptedProperty(keyToBeDecrypted);
		// the value should not be modified in any way.
		assertEquals(base64EncodedCipher, results);
		// the value should not be decrypted
		verify(mockAWSKMS, never()).decrypt(decryptRequestCaptor.capture());
		verify(mockLog).warn("Property: 'org.sagebionetworks.stack.cmk.alias' does not exist so the value of 'toBeDecrypted' will not be decrypted.");
	}
	
	
	@Test
	public void testGetDecryptedPropertyNullKey() throws UnsupportedEncodingException {
		try {
			String key = null;
			// call under test
			stackEncrypterImpl.getDecryptedProperty(key);
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
			stackEncrypterImpl.getDecryptedProperty(key);
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
	private static String base64Encode(String input) {
		try {
			byte[] bytes = input.getBytes(ConfigurationPropertiesImpl.UTF_8);
			bytes = Base64.getEncoder().encode(bytes);
			return new String(bytes, ConfigurationPropertiesImpl.UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

	}
}
