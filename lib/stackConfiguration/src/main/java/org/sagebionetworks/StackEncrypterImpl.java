package org.sagebionetworks;

import static org.sagebionetworks.ConfigurationPropertiesImpl.PROPERTY_KEY_CANNOT_BE_NULL;
import static org.sagebionetworks.ConfigurationPropertiesImpl.PROPERTY_WITH_KEY_S_DOES_NOT_EXIST;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import org.apache.commons.codec.binary.Base64;

import org.apache.logging.log4j.Logger;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;
import com.amazonaws.services.kms.model.ReEncryptRequest;
import com.amazonaws.services.kms.model.ReEncryptResult;
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

	private boolean encryptionEnabled() {
		return configuration.hasProperty(PROPERTY_KEY_STACK_CMK_ALIAS);
	}

	@Override
	public String encryptAndBase64EncodeStringWithStackKey(String plainText) {
		try {
			if(!encryptionEnabled()) {
				return Base64.encodeBase64URLSafeString(plainText.getBytes(UTF_8));
			}
			byte[] plainTextBytes = plainText.getBytes( UTF_8 );
			EncryptRequest  encryptRequest = new EncryptRequest().
					withPlaintext(ByteBuffer.wrap(plainTextBytes)).
					withKeyId(configuration.getProperty(PROPERTY_KEY_STACK_CMK_ALIAS));
			EncryptResult encryptResult = this.awsKeyManagerClient.encrypt(encryptRequest);
			return Base64.encodeBase64URLSafeString(encryptResult.getCiphertextBlob().array());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String getDecryptedProperty(String propertyKey) {
		if(propertyKey == null) {
			throw new IllegalArgumentException(PROPERTY_KEY_CANNOT_BE_NULL);
		}
		String propertyValue = configuration.getProperty(propertyKey);
		if (propertyValue == null) {
			throw new IllegalArgumentException(String.format(PROPERTY_WITH_KEY_S_DOES_NOT_EXIST, propertyKey));
		}
		// Properties are only decrypted if a key alias is provided
		if(!encryptionEnabled()) {
			log.warn(String.format(WILL_NOT_DECRYPT_MESSAGE, PROPERTY_KEY_STACK_CMK_ALIAS, propertyKey));
			return propertyValue;
		}
		log.info(String.format(DECRYPTING_PROPERTY, propertyKey));
		// load the Base64 encoded encrypted string from the properties.
		String encryptedValueBase64 = configuration.getProperty(propertyKey);
		return decryptStackEncryptedAndBase64EncodedString(encryptedValueBase64);
	}
	
	@Override
	public String decryptStackEncryptedAndBase64EncodedString(String encryptedValueBase64) {
		try {
			byte[] rawEncrypted = Base64.decodeBase64(encryptedValueBase64);
			if(!encryptionEnabled()) {
				return new String(rawEncrypted, UTF_8);
			}
			// KMS can decrypt the value without providing the encryption key.
			DecryptResult decryptResult = this.awsKeyManagerClient.decrypt(new DecryptRequest().
					withCiphertextBlob(ByteBuffer.wrap(rawEncrypted)));
			return byteBufferToString(decryptResult.getPlaintext());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String reEncryptStackEncryptedAndBase64EncodedString(String encryptedValueBase64) {
		if(!encryptionEnabled()) {
			return encryptedValueBase64;
		}
		byte[] rawEncrypted = Base64.decodeBase64(encryptedValueBase64);
		// KMS can decrypt the value without providing the encryption key.
		ReEncryptRequest reEncryptRequest = new ReEncryptRequest().
				withCiphertextBlob(ByteBuffer.wrap(rawEncrypted)).
				withDestinationKeyId(configuration.getProperty(PROPERTY_KEY_STACK_CMK_ALIAS));
		ReEncryptResult reEncryptResult = this.awsKeyManagerClient.reEncrypt(reEncryptRequest);
		return Base64.encodeBase64URLSafeString(reEncryptResult.getCiphertextBlob().array());
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
