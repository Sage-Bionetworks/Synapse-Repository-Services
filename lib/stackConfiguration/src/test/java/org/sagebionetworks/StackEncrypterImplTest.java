package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;
import com.amazonaws.services.kms.model.ReEncryptRequest;
import com.amazonaws.services.kms.model.ReEncryptResult;


@RunWith(MockitoJUnitRunner.class)
public class StackEncrypterImplTest {

	@InjectMocks
	private StackEncrypterImpl stackEncrypter;

	@Mock
	private ConfigurationProperties mockConfigurationProperties;
	@Mock
	private AWSKMS mockAWSKMS;
	@Mock
	private LoggerProvider mockLoggerProvider;
	@Mock
	private Logger mockLog;

	@Captor
	private ArgumentCaptor<EncryptRequest> encryptRequestCaptor;

	@Captor
	private ArgumentCaptor<DecryptRequest> decryptRequestCaptor;

	@Captor
	private ArgumentCaptor<ReEncryptRequest> reEncryptRequestCaptor;

	private String keyToBeDecrypted;
	private String encryptedValue;
	private String reEncryptedValue;
	private String base64EncodedCipher;
	private String base64EncodedReEncryptedCipher;
	private String decryptedValue;

	

	@Before
	public void setUp() {
		when(mockLoggerProvider.getLogger(anyString())).thenReturn(mockLog);

		stackEncrypter = new StackEncrypterImpl(mockConfigurationProperties, mockAWSKMS, mockLoggerProvider);

		keyToBeDecrypted = "keyToBeDecrypted";
		
		encryptedValue = "This is encrypted";
		base64EncodedCipher = base64Encode(encryptedValue);
		
		decryptedValue = "This is the value after decryption";
		
		reEncryptedValue = "This is re-encrypted";
		base64EncodedReEncryptedCipher = base64Encode(reEncryptedValue);
		
		when(mockConfigurationProperties.hasProperty(StackEncrypterImpl.PROPERTY_KEY_STACK_CMK_ALIAS)).thenReturn(true);
		when(mockConfigurationProperties.getProperty(keyToBeDecrypted)).thenReturn(base64EncodedCipher);
		
		EncryptResult encryptResult = new EncryptResult();
		encryptResult.setCiphertextBlob(ByteBuffer.wrap(encryptedValue.getBytes()));
		when(mockAWSKMS.encrypt(any(EncryptRequest.class))).thenReturn(encryptResult);
		
		DecryptResult decryptResult = new DecryptResult();
		decryptResult.setPlaintext(ByteBuffer.wrap(decryptedValue.getBytes()));
		when(mockAWSKMS.decrypt(any(DecryptRequest.class))).thenReturn(decryptResult);
		
		ReEncryptResult reEncryptResult = new ReEncryptResult();
		reEncryptResult.setCiphertextBlob(ByteBuffer.wrap(reEncryptedValue.getBytes()));
		when(mockAWSKMS.reEncrypt(any(ReEncryptRequest.class))).thenReturn(reEncryptResult);
	}

	@Test
	public void testEncryptionRoundTrip() throws Exception {
		{
			// method under test
			assertEquals(base64EncodedCipher, stackEncrypter.encryptAndBase64EncodeStringWithStackKey(encryptedValue));
			verify(mockAWSKMS).encrypt(encryptRequestCaptor.capture());
			EncryptRequest encryptRequest = encryptRequestCaptor.getValue();
			assertNotNull(encryptRequest);
		}

		{
			// method under test
			assertEquals(decryptedValue, stackEncrypter.decryptStackEncryptedAndBase64EncodedString(base64EncodedCipher));
			verify(mockAWSKMS).decrypt(decryptRequestCaptor.capture());
			DecryptRequest decryptRequest = decryptRequestCaptor.getValue();
			assertNotNull(decryptRequest);
		}

		{
			// method under test
			assertEquals(base64EncodedReEncryptedCipher, stackEncrypter.reEncryptStackEncryptedAndBase64EncodedString(base64EncodedCipher));
			verify(mockAWSKMS).reEncrypt(reEncryptRequestCaptor.capture());
			ReEncryptRequest reEncryptRequest = reEncryptRequestCaptor.getValue();
			assertNotNull(reEncryptRequest);
		}
	}
	
	@Test
	public void testEncryptionRoundTripNoAlias() throws Exception {
		when(mockConfigurationProperties.hasProperty(StackEncrypterImpl.PROPERTY_KEY_STACK_CMK_ALIAS)).thenReturn(false);
		
		String encryptionInput = "some random string";
		String base64EncodedInput = base64Encode(encryptionInput);
		// method under test
		assertEquals(base64EncodedInput, stackEncrypter.encryptAndBase64EncodeStringWithStackKey(encryptionInput));
		
		// method under test
		assertEquals(encryptionInput, stackEncrypter.decryptStackEncryptedAndBase64EncodedString(base64EncodedInput));
		
		// method under test
		assertEquals(base64EncodedInput, stackEncrypter.reEncryptStackEncryptedAndBase64EncodedString(base64EncodedInput));
	}
	
	
	@Test
	public void testGetDecryptedProperty() throws UnsupportedEncodingException {
		// call under test
		String results = stackEncrypter.getDecryptedProperty(keyToBeDecrypted);
		assertEquals(decryptedValue, results);
		verify(mockAWSKMS).decrypt(decryptRequestCaptor.capture());
		DecryptRequest request = decryptRequestCaptor.getValue();
		assertNotNull(request);
		assertEquals(encryptedValue, new String(request.getCiphertextBlob().array()));
		verify(mockLog).info("Decrypting property 'keyToBeDecrypted'...");
	}

	
	@Test
	public void testGetDecryptedPropertyNoAlias() throws UnsupportedEncodingException {
		// Remove the alias
		when(mockConfigurationProperties.hasProperty(StackEncrypterImpl.PROPERTY_KEY_STACK_CMK_ALIAS)).thenReturn(false);
		when(mockConfigurationProperties.getProperty(keyToBeDecrypted)).thenReturn(encryptedValue);
		
		// call under test
		String results = stackEncrypter.getDecryptedProperty(keyToBeDecrypted);
		// the value should not be modified in any way.
		assertEquals(encryptedValue, results);
		// the value should not be decrypted
		verify(mockAWSKMS, never()).decrypt(decryptRequestCaptor.capture());
		verify(mockLog).warn("Property: 'org.sagebionetworks.stack.cmk.alias' does not exist so the value of 'keyToBeDecrypted' will not be decrypted.");
	}
	
	
	@Test
	public void testGetDecryptedPropertyNullKey() throws UnsupportedEncodingException {
		try {
			String key = null;
			// call under test
			stackEncrypter.getDecryptedProperty(key);
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
			stackEncrypter.getDecryptedProperty(key);
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
			return Base64.encodeBase64URLSafeString(bytes);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

	}

}
