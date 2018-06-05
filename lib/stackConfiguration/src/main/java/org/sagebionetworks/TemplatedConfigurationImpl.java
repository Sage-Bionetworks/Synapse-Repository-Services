package org.sagebionetworks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Properties;
import java.util.StringJoiner;

import org.apache.logging.log4j.Logger;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;

public class TemplatedConfigurationImpl implements TemplatedConfiguration {

	private static final String WILL_NOT_DECRYPT_MESSAGE = "Property: '%s' does not exist so value of '%s' will not be decrypted.";

	public static final String UTF_8 = "UTF-8";

	public static final String PROPERTY_KEY_STACK_CMK_ALIAS = "org.sagebionetworks.stack.cmk.alias";

	static final String DEFAULT_PROPERTIES_FILENAME = "/stack.properties";

	private Logger log;
	
	private Properties properties;

	private AWSKMS awsKeyManagerClient;

	/**
	 * The only constructor with AWSKMS and property provider.
	 * @param awsKeyManagerClient
	 * @param propertyProvider
	 */
	public TemplatedConfigurationImpl(AWSKMS awsKeyManagerClient, PropertyProvider propertyProvider, Logger log) {
		this.log = log;
		this.awsKeyManagerClient = awsKeyManagerClient;
		// Will contain the filal properties
		properties = new Properties();
		// Load the default properties.
		overrideProperties(propertyProvider.loadPropertiesFromClasspath(DEFAULT_PROPERTIES_FILENAME));
		// Maven settings override defaults.
		overrideProperties(propertyProvider.getMavenSettingsProperties());
		// System properties override all.
		overrideProperties(propertyProvider.getSystemProperties());
	}

	/**
	 * Override all non-null values from
	 * 
	 * @param originals
	 * @param overrides
	 */
	void overrideProperties(Properties overrides) {
		for (Object propertyName : overrides.keySet()) {
			String value = (String) overrides.get(propertyName);
			if (value != null && value.length() > 0) {
				properties.setProperty((String) propertyName, value);
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
			throw new IllegalArgumentException("Property key cannot be null");
		}
		String propertyValue = properties.getProperty(propertyKey);
		if (propertyValue == null) {
			throw new IllegalArgumentException("Property value is null for key: " + propertyKey);
		}
		return propertyValue;
	}

	@Override
	public String getDecryptedProperty(String propertyName) {
		if(propertyName == null) {
			throw new IllegalArgumentException("Property key cannot be null");
		}
		// Properties are only decrypted if a key alias is provider
		if(!this.properties.containsKey(PROPERTY_KEY_STACK_CMK_ALIAS)) {
			log.warn(String.format(WILL_NOT_DECRYPT_MESSAGE, PROPERTY_KEY_STACK_CMK_ALIAS, propertyName));
			return getProperty(propertyName);
		}
		try {
			// load the Base64 encoded encrypted string from the properties.
			String encryptedValueBase64 = getProperty(propertyName);
			byte[] rawEncrypted = Base64.getDecoder().decode(encryptedValueBase64.getBytes(UTF_8));
			DecryptResult back = this.awsKeyManagerClient.decrypt(new DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(rawEncrypted)));
			byte[] rawBytes = new byte[back.getPlaintext().remaining()];
			back.getPlaintext().get(rawBytes);
			return new String(rawBytes, UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Load
	 * 
	 * @param filename
	 * @param properties
	 * @return
	 * @throws IOException
	 */
	public static Properties loadPropertiesFromClasspath(String filename) throws IOException {
		if (filename == null) {
			throw new IllegalArgumentException("filename cannot be null");
		}
		try (InputStream input = TemplatedConfigurationImpl.class.getClassLoader().getResourceAsStream(filename);) {
			if (input == null) {
				throw new IllegalArgumentException("Cannot find file on classpath: " + filename);
			}
			Properties properties = new Properties();
			properties.load(input);
			return properties;
		}
	}


	/**
	 * Throws the same RuntimeException when a required property is missing.
	 * 
	 * @param propertyKey
	 * @param alternate
	 */
	private void throwRequiredPropertyException(String propertyKey, String alternate) {
		throw new RuntimeException(
				"The property: " + propertyKey + " or its alternate: " + alternate + " is required and cannot be null");
	}

	@Override
	public String getStack() {
		String stack = System.getProperty(StackConstants.PARAM3);
		if (stack == null)
			stack = System.getProperty(StackConstants.STACK_PROPERTY_NAME);
		if (stack == null)
			throwRequiredPropertyException(StackConstants.STACK_PROPERTY_NAME, StackConstants.PARAM3);
		return stack;
	}

	@Override
	public String getStackInstance() {
		String instance = System.getProperty(StackConstants.PARAM4);
		if (instance == null)
			instance = System.getProperty(StackConstants.STACK_INSTANCE_PROPERTY_NAME);
		if (instance == null)
			throwRequiredPropertyException(StackConstants.STACK_INSTANCE_PROPERTY_NAME, StackConstants.PARAM4);
		return instance;
	}

	@Override
	public String getAuthenticationServicePrivateEndpoint() {
		return getProperty("org.sagebionetworks.authenticationservice.privateendpoint");
	}

	@Override
	public String getAuthenticationServicePublicEndpoint() {
		return getProperty("org.sagebionetworks.authenticationservice.publicendpoint");
	}

	@Override
	public String getRepositoryServiceEndpoint() {
		return getProperty("org.sagebionetworks.repositoryservice.endpoint");
	}

	public String getFileServiceEndpoint() {
		return getProperty("org.sagebionetworks.fileservice.endpoint");
	}

	@Override
	public String getSearchServiceEndpoint() {
		return getProperty("org.sagebionetworks.searchservice.endpoint");
	}

	@Override
	public String getDockerServiceEndpoint() {
		return getProperty("org.sagebionetworks.docker.endpoint");
	}

	@Override
	public String getDockerRegistryListenerEndpoint() {
		return getProperty("org.sagebionetworks.docker.registry.listener.endpoint");
	}

	@Override
	public int getHttpClientMaxConnsPerRoute() {
		// We get connection timeouts from HttpClient if max conns is zero,
		// which is a confusing
		// error, so instead check more vigorously for that configuration
		// mistake
		String maxConnsPropertyName = "org.sagebionetworks.httpclient.connectionpool.maxconnsperroute";
		int maxConns = Integer.parseInt(getProperty(maxConnsPropertyName));
		if (1 > maxConns) {
			throw new IllegalArgumentException(maxConnsPropertyName + " must be greater than zero");
		}
		return maxConns;
	}
}
