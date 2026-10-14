package org.sagebionetworks;

import static org.sagebionetworks.ConfigurationPropertiesImpl.PROPERTY_KEY_CANNOT_BE_NULL;
import static org.sagebionetworks.ConfigurationPropertiesImpl.PROPERTY_WITH_KEY_S_DOES_NOT_EXIST;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.codec.binary.Base64;

import org.apache.logging.log4j.Logger;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;
import software.amazon.awssdk.services.kms.model.ReEncryptRequest;
import software.amazon.awssdk.services.kms.model.ReEncryptResponse;
import com.google.inject.Inject;

public class StackEncrypterImpl implements StackEncrypter {
	public static final String UTF_8 = "UTF-8";
	private KmsClient awsKeyManagerClient;
	private Logger log;
	private ConfigurationProperties configuration;
	private LoadingCache<String, String> decryptedPropertyCache;

	public static final String PROPERTY_KEY_STACK_CMK_ALIAS = "org.sagebionetworks.stack.cmk.alias";
	public static final String WILL_NOT_DECRYPT_MESSAGE = "Property: '%s' does not exist so the value of '%s' will not be decrypted.";
	public static final String DECRYPTING_PROPERTY = "Decrypting property '%s'...";
	private static final int CACHE_EXPIRATION_MINUTES = 5;

	@Inject
	public StackEncrypterImpl(ConfigurationProperties configuration, KmsClient awsKeyManagerClient, LoggerProvider logProvider) {
		this.awsKeyManagerClient=awsKeyManagerClient;
		this.configuration=configuration;
		this.log = logProvider.getLogger(StackEncrypterImpl.class.getName());
		this.decryptedPropertyCache = CacheBuilder.newBuilder()
				.expireAfterAccess(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
				.build( new CacheLoader<String, String>() {
					@Override
					public String load(String key) {
						return decryptProperty(key);
					}
				});
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
			byte[] plainTextBytes = plainText.getBytes(UTF_8);
			EncryptRequest encryptRequest = EncryptRequest.builder()
					.plaintext(SdkBytes.fromByteArray(plainTextBytes))
					.keyId(configuration.getProperty(PROPERTY_KEY_STACK_CMK_ALIAS))
					.build();
			EncryptResponse encryptResult = this.awsKeyManagerClient.encrypt(encryptRequest);
			return Base64.encodeBase64URLSafeString(encryptResult.ciphertextBlob().asByteArray());
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
			log.debug(String.format(WILL_NOT_DECRYPT_MESSAGE, PROPERTY_KEY_STACK_CMK_ALIAS, propertyKey));
			return propertyValue;
		}
		return decryptedPropertyCache.getUnchecked(propertyKey);
	}

	private String decryptProperty(String propertyKey) {
		log.debug(String.format(DECRYPTING_PROPERTY, propertyKey));
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
			DecryptResponse decryptResult = this.awsKeyManagerClient.decrypt(DecryptRequest.builder()
					.ciphertextBlob(SdkBytes.fromByteArray(rawEncrypted))
					.build());
			return new String(decryptResult.plaintext().asByteArray(), UTF_8);
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
		ReEncryptRequest reEncryptRequest = ReEncryptRequest.builder()
				.ciphertextBlob(SdkBytes.fromByteArray(rawEncrypted))
				.destinationKeyId(configuration.getProperty(PROPERTY_KEY_STACK_CMK_ALIAS))
				.build();
		ReEncryptResponse reEncryptResult = this.awsKeyManagerClient.reEncrypt(reEncryptRequest);
		return Base64.encodeBase64URLSafeString(reEncryptResult.ciphertextBlob().asByteArray());
	}
}
