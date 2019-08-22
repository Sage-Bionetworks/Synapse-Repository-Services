package org.sagebionetworks;

import static org.sagebionetworks.ConfigurationPropertiesImpl.PROPERTY_KEY_CANNOT_BE_NULL;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Base64;

import org.apache.logging.log4j.Logger;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;
import com.google.inject.Inject;

public class StackEncrypterImpl implements StackEncrypter {
	public static final String UTF_8 = "UTF-8";
	private AWSKMS awsKeyManagerClient;
	private Logger log;
	private ConfigurationProperties configuration;
	
	public static final String PROPERTY_KEY_STACK_CMK_ALIAS = "org.sagebionetworks.stack.cmk.alias";
	public static final String WILL_NOT_DECRYPT_MESSAGE = "Property: '%s' does not exist so the value of '%s' will not be decrypted.";
	public static final String DECRYPTING_PROPERTY = "Decrypting property '%s'...";

	@Inject
	public StackEncrypterImpl(ConfigurationProperties configuration, AWSKMS awsKeyManagerClient, LoggerProvider logProvider) {
		this.awsKeyManagerClient=awsKeyManagerClient;
		this.configuration=configuration;
		this.log = logProvider.getLogger(StackEncrypterImpl.class.getName());
	}

	private boolean decryptionEnabled() {
		String cmkAlias = configuration.getProperty(PROPERTY_KEY_STACK_CMK_ALIAS);
		return cmkAlias!=null && cmkAlias.length()>0;
	}


	@Override
	public String encryptStringWithStackKey(String plainText) {
		if(!decryptionEnabled()) {
			return plainText;
		}
		try {
			byte[] plainTextBytes = plainText.getBytes( UTF_8 );
			EncryptResult encryptResult = this.awsKeyManagerClient.encrypt(
					new EncryptRequest().withPlaintext(ByteBuffer.wrap(plainTextBytes)));
			return new String(Base64.getEncoder().encode(encryptResult.getCiphertextBlob()).array(), UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String getDecryptedProperty(String propertyKey) {
		if(propertyKey == null) {
			throw new IllegalArgumentException(PROPERTY_KEY_CANNOT_BE_NULL);
		}
		// Properties are only decrypted if a key alias is provided
		if(!decryptionEnabled()) {
			log.warn(String.format(WILL_NOT_DECRYPT_MESSAGE, PROPERTY_KEY_STACK_CMK_ALIAS, propertyKey));
			return configuration.getProperty(propertyKey);
		}
		log.info(String.format(DECRYPTING_PROPERTY, propertyKey));
		// load the Base64 encoded encrypted string from the properties.
		String encryptedValueBase64 = configuration.getProperty(propertyKey);
		return decryptStackEncryptedString(encryptedValueBase64);
	}
	
	@Override
	public String decryptStackEncryptedString(String encryptedValueBase64) {
		if(!decryptionEnabled()) {
			return encryptedValueBase64;
		}
		try {
			byte[] rawEncrypted = Base64.getDecoder().decode(encryptedValueBase64.getBytes(UTF_8));
			// KMS can decrypt the value without providing the encryption key.
			DecryptResult decryptResult = this.awsKeyManagerClient.decrypt(new DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(rawEncrypted)));
			return byteBufferToString(decryptResult.getPlaintext());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Convert the given ByteBuffer to a UTF-8 string.
	 * @param buffer
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private static String byteBufferToString(ByteBuffer buffer) {
		try {
			byte[] rawBytes = new byte[buffer.remaining()];
			buffer.get(rawBytes);
			return new String(rawBytes, UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
