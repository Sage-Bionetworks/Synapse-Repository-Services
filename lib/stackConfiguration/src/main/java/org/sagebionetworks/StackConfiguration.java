package org.sagebionetworks;

import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
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
public class StackConfiguration {

	private static final Logger log = Logger.getLogger(StackConfiguration.class
			.getName());

	private static final String PROPERTIES_FILENAME_PREFIX = "/stack";
	private static final String PROPERTIES_FILENAME_STAGE_SEPARATOR = "-";
	private static final String PROPERTIES_FILENAME_SUFFIX = ".properties";
	private static final String DEFAULT_PROPERTIES_FILENAME = PROPERTIES_FILENAME_PREFIX
			+ PROPERTIES_FILENAME_SUFFIX;

	private static final String STACK_SYSTEM_PROPERTY_KEY = "org.sagebionetworks.stack";

	private static Properties defaultStackProperties = null;
	private static Properties stackPropertyOverrides = null;

	static {
		// Load the stack configuration the first time this class is referenced
		reloadStackConfiguration();
	}

	/**
	 * Load stack configuration from properties files. Note that the System
	 * property org.sagebionetworks.stack is used to let the system know for
	 * which stack overrides should be loaded.
	 */
	public static void reloadStackConfiguration() {

		defaultStackProperties = new Properties();
		stackPropertyOverrides = new Properties();

		if (!loadProperties(DEFAULT_PROPERTIES_FILENAME, defaultStackProperties)) {
			throw new Error(
					"Unable to load default stack properties from classpath: "
							+ DEFAULT_PROPERTIES_FILENAME);
		}

		String stack = System.getProperty(STACK_SYSTEM_PROPERTY_KEY);
		if (null == stack) {
			log.info("System property " + STACK_SYSTEM_PROPERTY_KEY
					+ " not specified, using default stack properties");
		} else {
			String stackPropertiesFilename = PROPERTIES_FILENAME_PREFIX
					+ PROPERTIES_FILENAME_STAGE_SEPARATOR + stack
					+ PROPERTIES_FILENAME_SUFFIX;
			if (loadProperties(stackPropertiesFilename, stackPropertyOverrides)) {
				log.info("Loaded stack property overrides from "
						+ stackPropertiesFilename);
			} else {
				log.info("No stack properties file named "
						+ stackPropertiesFilename
						+ " found, using default stack properties");
			}
		}

	}

	private static String getProperty(String propertyName) {
		return (stackPropertyOverrides.containsKey(propertyName) ? stackPropertyOverrides
				.getProperty(propertyName)
				: defaultStackProperties.getProperty(propertyName));
	}

	private static boolean loadProperties(String filename, Properties properties) {
		URL propertiesLocation = StackConfiguration.class.getResource(filename);
		if (null == propertiesLocation) {
			return false;
		}

		try {
			properties.load(propertiesLocation.openStream());
		} catch (Exception e) {
			throw new Error(e);
		}
		return true;
	}

	public static String getCrowdEndpoint() {
		return getProperty("org.sagebionetworks.crowd.endpoint");
	}

	public static String getAuthenticationServiceEndpoint() {
		return getProperty("org.sagebionetworks.authenticationservice.endpoint");
	}

	public static String getRepositoryServiceEndpoint() {
		return getProperty("org.sagebionetworks.repositoryservice.endpoint");
	}

	public static String getPortalEndpoint() {
		return getProperty("org.sagebionetworks.portal.endpoint");
	}
	
	public static String getS3Bucket() {
		return getProperty("org.sagebionetworks.s3.bucket");
	}

	public static String getS3IamGroup() {
		return getProperty("org.sagebionetworks.s3.iam.group");
	}
}
