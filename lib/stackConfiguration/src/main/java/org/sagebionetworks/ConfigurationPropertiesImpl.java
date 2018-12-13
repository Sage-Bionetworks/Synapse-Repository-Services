package org.sagebionetworks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.Inject;

public class ConfigurationPropertiesImpl implements ConfigurationProperties {

	public static final String S3_OBJECT_DOES_NOT_EXIST = "S3 Object does not exist with bucket: '%s' and key: '%s'";
	public static final String ORG_SAGEBIONETWORKS_SECRETS_KEY = "org.sagebionetworks.secrets.key";
	public static final String ORG_SAGEBIONETWORKS_SECRETS_BUCKET = "org.sagebionetworks.secrets.bucket";
	public static final String SECRETS_WERE_NOT_LOADED_FROM_S3 = "Secrets were not loaded from S3.";
	public static final String LOADED_SECRECTS_S3 = "Loaded %s secrets from: %s/%s";
	public static final String DECRYPTING_PROPERTY = "Decrypting property '%s'...";
	public static final String PROPERTY_WITH_KEY_S_DOES_NOT_EXIST = "Property with key: '%s' does not exist.";
	public static final String PROPERTY_KEY_CANNOT_BE_NULL = "Property key cannot be null";
	public static final String WILL_NOT_DECRYPT_MESSAGE = "Property: '%s' does not exist so the value of '%s' will not be decrypted.";
	public static final String UTF_8 = "UTF-8";
	public static final String PROPERTY_KEY_STACK_CMK_ALIAS = "org.sagebionetworks.stack.cmk.alias";
	public static final String DEFAULT_PROPERTIES_FILENAME = "/stack.properties";

	private Logger log;
	
	private Properties properties;

	private AWSKMS awsKeyManagerClient;
	
	private AmazonS3 s3Client;
	
	private PropertyProvider propertyProvider;

	/**
	 * The only constructor with AWSKMS and property provider.
	 * @param awsKeyManagerClient
	 * @param propertyProvider
	 */
	@Inject
	public ConfigurationPropertiesImpl(AWSKMS awsKeyManagerClient, AmazonS3 s3Client, PropertyProvider propertyProvider, LoggerProvider logProvider) {
		this.log = logProvider.getLogger(ConfigurationPropertiesImpl.class.getName());
		this.awsKeyManagerClient = awsKeyManagerClient;
		this.s3Client = s3Client;
		this.propertyProvider = propertyProvider;
		initialize();
	}

	void initialize() {
		// Will contain the final properties
		properties = new Properties();
		// Load the default properties.
		overrideProperties(propertyProvider.loadPropertiesFromClasspath(DEFAULT_PROPERTIES_FILENAME));
		// Maven settings override defaults.
		overrideProperties(propertyProvider.getMavenSettingsProperties());
		// System properties override all.
		overrideProperties(propertyProvider.getSystemProperties());
		// load any secrets from S3
		String secretsBucket = properties.getProperty(ORG_SAGEBIONETWORKS_SECRETS_BUCKET);
		String secretsKey = properties.getProperty(ORG_SAGEBIONETWORKS_SECRETS_KEY);
		overrideProperties(loadSecrets(secretsBucket, secretsKey));
	}

	/**
	 * Override all non-null values from
	 * 
	 * @param originals
	 * @param overrides
	 */
	void overrideProperties(Properties overrides) {
		if(overrides != null) {
			for (Object propertyName : overrides.keySet()) {
				String value = (String) overrides.get(propertyName);
				if (value != null && value.length() > 0) {
					properties.setProperty((String) propertyName, value);
				}
			}
		}
	}

	/**
	 * Load the given property.
	 * 
	 * @throws IllegalArgumentException
	 *             if the key is null or the resulting value is null.
	 */
	public String getProperty(String propertyKey) {
		if (propertyKey == null) {
			throw new IllegalArgumentException(PROPERTY_KEY_CANNOT_BE_NULL);
		}
		String propertyValue = properties.getProperty(propertyKey);
		if (propertyValue == null) {
			throw new IllegalArgumentException(String.format(PROPERTY_WITH_KEY_S_DOES_NOT_EXIST, propertyKey));
		}
		return propertyValue;
	}

	@Override
	public String getDecryptedProperty(String propertyKey) {
		if(propertyKey == null) {
			throw new IllegalArgumentException(PROPERTY_KEY_CANNOT_BE_NULL);
		}
		// Properties are only decrypted if a key alias is provider
		if(!this.properties.containsKey(PROPERTY_KEY_STACK_CMK_ALIAS)) {
			log.warn(String.format(WILL_NOT_DECRYPT_MESSAGE, PROPERTY_KEY_STACK_CMK_ALIAS, propertyKey));
			return getProperty(propertyKey);
		}
		log.info(String.format(DECRYPTING_PROPERTY, propertyKey));
		try {
			// load the Base64 encoded encrypted string from the properties.
			String encryptedValueBase64 = getProperty(propertyKey);
			byte[] rawEncrypted = Base64.getDecoder().decode(encryptedValueBase64.getBytes(UTF_8));
			// KMS can decrypt the value without providing the encryption key.
			DecryptResult decryptResult = this.awsKeyManagerClient.decrypt(new DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(rawEncrypted)));
			return byteBuferToString(decryptResult.getPlaintext());
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
	static String byteBuferToString(ByteBuffer buffer) throws UnsupportedEncodingException {
		byte[] rawBytes = new byte[buffer.remaining()];
		buffer.get(rawBytes);
		return new String(rawBytes, UTF_8);
	}
	
	/**
	 * Load the secrets from the given S3 bucket and key.
	 * @param secretBucket
	 * @param secretKey
	 * @return Returns the properties from the given bucket and key. If the bucket or key are null then will return null.
	 * IF the object does not exist then will return null.
	 * @throws IOException 
	 * @throws SdkClientException 
	 * @throws AmazonServiceException 
	 */
	Properties loadSecrets(String secretBucket, String secretKey) {
		if(secretBucket != null && secretKey != null) {
			if(s3Client.doesObjectExist(secretBucket, secretKey)) {
				try {
					try(InputStream input = s3Client.getObject(secretBucket, secretKey).getObjectContent()){
						Properties props = new Properties();
						props.load(input);
						log.info(String.format(LOADED_SECRECTS_S3, props.keySet().size(), secretBucket, secretKey));
						return props;
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				} 
			}else {
				// the object does not exist
				log.warn(String.format(S3_OBJECT_DOES_NOT_EXIST, secretBucket, secretKey));
			}
		}
		log.warn(SECRETS_WERE_NOT_LOADED_FROM_S3);
		return null;
	}
}
