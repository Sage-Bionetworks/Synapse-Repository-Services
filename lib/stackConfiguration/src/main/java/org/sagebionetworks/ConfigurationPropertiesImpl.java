package org.sagebionetworks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.aws.SynapseS3Client;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.google.inject.Inject;

public class ConfigurationPropertiesImpl implements ConfigurationProperties {

	public static final String S3_OBJECT_DOES_NOT_EXIST = "S3 Object does not exist with bucket: '%s' and key: '%s'";
	public static final String ORG_SAGEBIONETWORKS_SECRETS_KEY = "org.sagebionetworks.secrets.key";
	public static final String ORG_SAGEBIONETWORKS_SECRETS_BUCKET = "org.sagebionetworks.secrets.bucket";
	public static final String SECRETS_WERE_NOT_LOADED_FROM_S3 = "Secrets were not loaded from S3.";
	public static final String LOADED_SECRECTS_S3 = "Loaded %s secrets from: %s/%s";
	public static final String PROPERTY_WITH_KEY_S_DOES_NOT_EXIST = "Property with key: '%s' does not exist.";
	public static final String PROPERTY_KEY_CANNOT_BE_NULL = "Property key cannot be null";
	public static final String UTF_8 = "UTF-8";
	public static final String DEFAULT_PROPERTIES_FILENAME = "/stack.properties";

	private Logger log;
	
	private Properties properties;
	
	private SynapseS3Client s3Client;
	
	private PropertyProvider propertyProvider;

	/**
	 * 
	 * @param encryptionUtils
	 * @param s3Client
	 * @param propertyProvider
	 * @param logProvider
	 */
	@Inject
	public ConfigurationPropertiesImpl(SynapseS3Client s3Client, PropertyProvider propertyProvider, LoggerProvider logProvider) {
		this.log = logProvider.getLogger(ConfigurationPropertiesImpl.class.getName());
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
	
	@Override
	public boolean hasProperty(String propertyKey) {
		if (propertyKey == null) {
			throw new IllegalArgumentException(PROPERTY_KEY_CANNOT_BE_NULL);
		}
		return properties.containsKey(propertyKey);
	}


	/**
	 * Load the given property.
	 * 
	 * @throws IllegalArgumentException
	 *             if the key is null or the resulting value is null.
	 */
	@Override
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
