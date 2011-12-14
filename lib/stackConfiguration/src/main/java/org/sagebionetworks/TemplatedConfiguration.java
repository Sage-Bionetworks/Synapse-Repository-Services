package org.sagebionetworks;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * TemplatedConfiguration should serve as the base for any configuration we need
 * for any of our Java software. It encapsulates the core functionality of a
 * default properties file, a property override file, checking of that override
 * against a template, encrypted properties, and loading properties files from
 * S3.  It also exposes some properties common to all software stacks.
 * 
 * Here's the first stab at a configuration system for our various stacks. It
 * solves several problems we are currently having:
 * 
 * (1) a proliferation of system properties for non-password and non-credential
 * configuration values
 * 
 * (2) a way to build one artifact that can run on many stacks
 * 
 * (3) a place to encapsulate and limit the scope of property names, components
 * that depend upon this retrieve values by method instead of by property name
 * 
 * (4) standardization of property names because since they are close together,
 * we can all see the naming pattern
 */
public class TemplatedConfiguration {

	private static final Logger log = Logger
			.getLogger(TemplatedConfiguration.class.getName());

	private String defaultPropertiesFilename;
	private String templatePropertiesFilename;

	private Properties defaultStackProperties = null;
	private Properties stackPropertyOverrides = null;
	private Properties requiredProperties = null;
	private String propertyFileUrl = null;

	/**
	 * Pass in the default location for the properties file and also the
	 * template to use
	 * 
	 * @param defaultPropertiesFilename
	 * @param templatePropertiesFilename
	 */
	public TemplatedConfiguration(String defaultPropertiesFilename,
			String templatePropertiesFilename) {
		this.defaultPropertiesFilename = defaultPropertiesFilename;
		this.templatePropertiesFilename = templatePropertiesFilename;
	}

	/**
	 * Load stack configuration from properties files. Note that the System
	 * property org.sagebionetworks.stack is used to let the system know for
	 * which stack overrides should be loaded.
	 */
	public void reloadStackConfiguration() {
		defaultStackProperties = new Properties();
		stackPropertyOverrides = new Properties();
		requiredProperties = new Properties();

		// Load the default properties from the classpath.
		loadPropertiesFromClasspath(defaultPropertiesFilename,
				defaultStackProperties);
		// Load the required properties
		loadPropertiesFromClasspath(templatePropertiesFilename,
				requiredProperties);
		// If the system properties does not have the property file url,
		// then we need to try and load the maven settings file.
		if (System.getProperty(StackConstants.STACK_PROPERTY_FILE_URL) == null) {
			// Try loading the settings file
			addSettingsPropertiesToSystem();
		}
		// These four properties are required. If they are null, an exception
		// will be thrown
		String propertyFileUrl = getPropertyFileURL();

		String encryptionKey = getEncryptionKey();
		String stack = getStack();
		String stackInstance = getStackInstance();

		// Validate the property file
		StackUtils.validateStackProperty(stack + stackInstance,
				StackConstants.STACK_PROPERTY_FILE_URL, propertyFileUrl);

		// If we have IAM id and key the load the properties using the Amazon
		// client, else the URL should be public.
		String iamId = getIAMUserId();
		String iamKey = getIAMUserKey();
		if (propertyFileUrl
				.startsWith(StackConstants.S3_PROPERTY_FILENAME_PREFIX)
				&& iamId != null && iamKey != null) {
			try {
				S3PropertyFileLoader.loadPropertiesFromS3(propertyFileUrl,
						iamId, iamKey, stackPropertyOverrides);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			loadPropertiesFromURL(propertyFileUrl, stackPropertyOverrides);
		}
		// Validate the required properties
		StackUtils.validateRequiredProperties(requiredProperties,
				stackPropertyOverrides, stack, stackInstance);
	}

	public String getProperty(String propertyName) {
		String propertyValue = null;
		if (stackPropertyOverrides.containsKey(propertyName)) {
			propertyValue = stackPropertyOverrides.getProperty(propertyName);
			log.info(propertyName + "=" + propertyValue + " from "
					+ propertyFileUrl + "properties");
		} else {
			propertyValue = defaultStackProperties.getProperty(propertyName);
			log.info(propertyName + "=" + propertyValue
					+ " from default stack properties");
		}
		// NullPointerExceptions further downstream are not very helpful, throw
		// here
		// instead. In general folks calling methods here do not want null
		// values,
		// but if they do, they can try/catch.
		//
		// Also note that required properties should be checked for existence by
		// out template
		// so this should only happen for optional properties that code is
		// requesting
		if (null == propertyValue) {
			throw new NullPointerException(
					"no value found in StackConfiguration for property "
							+ propertyName + " propertyFileURL="
							+ propertyFileUrl);
		}
		return propertyValue;
	}

	public Set<String> getAllPropertyNames() {
		Set<String> allPropertyNames = new HashSet<String>();
		allPropertyNames.addAll(defaultStackProperties.stringPropertyNames());
		allPropertyNames.addAll(stackPropertyOverrides.stringPropertyNames());
		return allPropertyNames;
	}

	public String getDecryptedProperty(String propertyName) {
		String stackEncryptionKey = getEncryptionKey();
		if (stackEncryptionKey == null || stackEncryptionKey.length() == 0)
			throw new RuntimeException(
					"Expected system property org.sagebionetworks.stackEncryptionKey");
		String encryptedProperty = getProperty(propertyName);
		if (encryptedProperty == null || encryptedProperty.length() == 0)
			throw new RuntimeException("Expected property for " + propertyName);
		StringEncrypter se = new StringEncrypter(stackEncryptionKey);
		String clearTextPassword = se.decrypt(encryptedProperty);
		log.debug("clear text " + propertyName + " " + clearTextPassword);
		return clearTextPassword;
	}

	private void loadPropertiesFromClasspath(String filename,
			Properties properties) {
		if (filename == null)
			throw new IllegalArgumentException("filename cannot be null");
		if (properties == null)
			throw new IllegalArgumentException("properties cannot be null");
		URL propertiesLocation = TemplatedConfiguration.class
				.getResource(filename);
		if (null == propertiesLocation) {
			throw new IllegalArgumentException(
					"Could not load property file from classpath: " + filename);
		}
		try {
			properties.load(propertiesLocation.openStream());
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	/**
	 * Add the properties from the settings file to the system properties if
	 * they are there.
	 */
	private void addSettingsPropertiesToSystem() {
		Properties props;
		try {
			props = SettingsLoader.loadSettingsFile();
			if (props != null) {
				Iterator it = props.keySet().iterator();
				while (it.hasNext()) {
					String key = (String) it.next();
					String value = props.getProperty(key);
					System.setProperty(key, value);
				}
			}
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	/**
	 * Load a property file from a URL
	 * 
	 * @param url
	 * @param properties
	 * @return
	 */
	private void loadPropertiesFromURL(String url, Properties properties) {
		if (url == null)
			throw new IllegalArgumentException("url cannot be null");
		if (properties == null)
			throw new IllegalArgumentException("properties cannot be null");
		URL propertiesLocation;
		try {
			propertiesLocation = new URL(url);
		} catch (MalformedURLException e1) {
			throw new IllegalArgumentException(
					"Could not load property file from url: " + url, e1);
		}
		try {
			properties.load(propertiesLocation.openStream());
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	/**
	 * Throws the same RuntimeException when a required property is missing.
	 * 
	 * @param propertyKey
	 * @param alternate
	 */
	private void throwRequiredPropertyException(String propertyKey,
			String alternate) {
		throw new RuntimeException("The property: " + propertyKey
				+ " or its alternate: " + alternate
				+ " is required and cannot be null");
	}

	/**
	 * The location of the property file that overrides configuration
	 * properties.
	 * 
	 * @return
	 */
	public String getPropertyFileURL() {
		String url = System.getProperty(StackConstants.PARAM1);
		if (url == null)
			url = System.getProperty(StackConstants.STACK_PROPERTY_FILE_URL);
		if (url == null)
			throwRequiredPropertyException(
					StackConstants.STACK_PROPERTY_FILE_URL,
					StackConstants.PARAM1);
		return url;
	}

	/**
	 * The encryption key used to read passwords in the configuration property
	 * file.
	 * 
	 * @return
	 */
	public String getEncryptionKey() {
		String ek = System.getProperty(StackConstants.PARAM2);
		if (ek == null)
			ek = System.getProperty(StackConstants.STACK_ENCRYPTION_KEY);
		if (ek == null)
			throwRequiredPropertyException(StackConstants.STACK_ENCRYPTION_KEY,
					StackConstants.PARAM2);
		return ek;
	}

	/**
	 * The name of the stack.
	 * 
	 * @return
	 */
	public String getStack() {
		String stack = System.getProperty(StackConstants.PARAM3);
		if (stack == null)
			stack = System.getProperty(StackConstants.STACK_PROPERTY_NAME);
		if (stack == null)
			throwRequiredPropertyException(StackConstants.STACK_PROPERTY_NAME,
					StackConstants.PARAM3);
		return stack;
	}

	/**
	 * The stack instance (i.e 'A', or 'B')
	 * 
	 * @return
	 */
	public String getStackInstance() {
		String instance = System.getProperty(StackConstants.PARAM4);
		if (instance == null)
			instance = System
					.getProperty(StackConstants.STACK_INSTANCE_PROPERTY_NAME);
		if (instance == null)
			throwRequiredPropertyException(
					StackConstants.STACK_INSTANCE_PROPERTY_NAME,
					StackConstants.PARAM4);
		return instance;
	}

	/**
	 * Get the IAM user ID (Access Key ID)
	 * 
	 * @return
	 */
	public String getIAMUserId() {
		// There are a few places where we can find this
		String id = System.getProperty("AWS_ACCESS_KEY_ID");
		if (id != null)
			return id;
		id = System.getProperty(StackConstants.STACK_IAM_ID);
		if (id == null)
			return null;
		id = id.trim();
		if ("".equals(id))
			return null;
		return id;
	}

	/**
	 * Get the IAM user Key (Secret Access Key)
	 * 
	 * @return
	 */
	public String getIAMUserKey() {
		// There are a few places to look for this
		String key = System.getProperty("AWS_SECRET_KEY");
		if (key != null)
			return key;
		key = System.getProperty(StackConstants.STACK_IAM_KEY);
		if (key == null)
			return null;
		key = key.trim();
		if ("".equals(key))
			return null;
		return key;
	}

	public String getAuthenticationServicePrivateEndpoint() {
		return getProperty("org.sagebionetworks.authenticationservice.privateendpoint");
	}

	public String getAuthenticationServicePublicEndpoint() {
		return getProperty("org.sagebionetworks.authenticationservice.publicendpoint");
	}

	public String getRepositoryServiceEndpoint() {
		return getProperty("org.sagebionetworks.repositoryservice.endpoint");
	}

	public String getPortalEndpoint() {
		return getProperty("org.sagebionetworks.portal.endpoint");
	}

	public String getRScriptPath() {
		return getProperty("org.sagebionetworks.rScript.path");
	}
	
	public String getGEPipelineSourceProjectId() {
		return getProperty("org.sagebionetworks.gepipeline.sourceProjectId");
	}
	
	
	public String getGEPipelineTargetProjectId() {
		return getProperty("org.sagebionetworks.gepipeline.targetProjectId");
	}
	
	public String getGEPipelineCrawlerScript() {
		return getProperty("org.sagebionetoworks.gepipeline.crawlerscript");
	}
	
	public String getGEPipelineWorkflowScript() {
		return getProperty("org.sagebionetoworks.gepipeline.workflowscript");
	}
	
	public String getGEPipelineMaxDatasetSize() {
		return getProperty("org.sagebionetworks.gepipeline.maxdatasetsize");
	}

	public String getGEPipelineMaxWorkflowInstances() {
		return getProperty("org.sagebionetworks.gepipeline.maxworkflowinstances");
	}
	
	public String getGEPipelineNoop() {
		return getProperty("org.sagebionetworks.gepipeline.noop");
	}
	
	public int getGEPipelineSmallCapacityGB() {
		return Integer.parseInt(getProperty("org.sagebionetworks.gepipeline.smallGB"));
	}
	
	public int getGEPipelineMediumCapacityGB() {
		return Integer.parseInt(getProperty("org.sagebionetworks.gepipeline.mediumGB"));
	}
	
	public int getGEPipelineLargeCapacityGB() {
		return Integer.parseInt(getProperty("org.sagebionetworks.gepipeline.largeGB"));
	}
	
}
