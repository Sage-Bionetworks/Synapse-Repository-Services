package org.sagebionetworks;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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


	private static Properties defaultStackProperties = null;
	private static Properties stackPropertyOverrides = null;
	private static Properties requiredProperties = null;
	private static String propertyFileUrl = null;
	
	static {
		// Load the stack configuration the first time this class is referenced
		try {
			reloadStackConfiguration();
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			throw new RuntimeException(t);
		}
	}

	/**
	 * Load stack configuration from properties files. Note that the System
	 * property org.sagebionetworks.stack is used to let the system know for
	 * which stack overrides should be loaded.
	 */
	public static void reloadStackConfiguration() {
		defaultStackProperties = new Properties();
		stackPropertyOverrides = new Properties();
		requiredProperties = new Properties();
				
		// Load the default properties from the classpath.
		loadPropertiesFromClasspath(StackConstants.DEFAULT_PROPERTIES_FILENAME, defaultStackProperties);
		// Load the required properties
		loadPropertiesFromClasspath(StackConstants.TEMPLATE_PROPERTIES, requiredProperties);
		// If the system properties does not have the property file url,
		// then we need to try and load the maven settings file.
		if(System.getProperty(StackConstants.STACK_PROPERTY_FILE_URL) == null){
			// Try loading the settings file
			addSettingsPropertiesToSystem();
		}
		// These four properties are required.  If they are null, an exception will be thrown
		String propertyFileUrl = getPropertyFileURL();

		String encryptionKey = getEncryptionKey();
		String stack = getStack();
		String stackInstance = getStackInstance();
		
		// Validate the property file
		StackUtils.validateStackProperty(stack+stackInstance, StackConstants.STACK_PROPERTY_FILE_URL, propertyFileUrl);
		
		// If we have IAM id and key the load the properties using the Amazon client, else the URL shoudl be public.
		String iamId = getIAMUserId();
		String iamKey = getIAMUserKey();
		if(propertyFileUrl.startsWith(StackConstants.S3_PROPERTY_FILENAME_PREFIX) && iamId != null && iamKey != null){
			try {
				S3PropertyFileLoader.loadPropertiesFromS3(propertyFileUrl, iamId, iamKey, stackPropertyOverrides);
			} catch (IOException e) {
				throw new RuntimeException(e); 
			}
		}else{
			loadPropertiesFromURL(propertyFileUrl, stackPropertyOverrides);
		}
		// Validate the required properties
		StackUtils.validateRequiredProperties(requiredProperties, stackPropertyOverrides, stack, stackInstance);
	}

	private static String getProperty(String propertyName) {
		String propertyValue = null;
		if (stackPropertyOverrides.containsKey(propertyName)) {
			propertyValue = stackPropertyOverrides.getProperty(propertyName);
			log.info(propertyName+"="+propertyValue+" from " + propertyFileUrl + "properties");
		} else {
			propertyValue = defaultStackProperties.getProperty(propertyName);
			log.info(propertyName+"="+propertyValue+" from default stack properties");
		}
		// NullPointerExceptions further downstream are not very helpful, throw here 
		// instead.  In general folks calling methods here do not want null values, 
		// but if they do, they can try/catch. 
		//
		// Also note that required properties should be checked for existence by out template 
		// so this should only happen for optional properties that code is requesting
		if(null == propertyValue) {
			throw new NullPointerException("no value found in StackConfiguration for property " + propertyName +
					" propertyFileURL="+propertyFileUrl);
		}
		return propertyValue;
	}
	
	private static String getDecryptedProperty(String propertyName) {
		String stackEncryptionKey = getEncryptionKey();
		if (stackEncryptionKey==null || stackEncryptionKey.length()==0)
			throw new RuntimeException("Expected system property org.sagebionetworks.stackEncryptionKey");
		String encryptedProperty = getProperty(propertyName);
		if (encryptedProperty==null || encryptedProperty.length()==0)
			throw new RuntimeException("Expected property for "+propertyName);
		StringEncrypter se = new StringEncrypter(stackEncryptionKey);
		String clearTextPassword = se.decrypt(encryptedProperty);
		log.debug("clear text " + propertyName + " " + clearTextPassword);
		return clearTextPassword; 
	}

	private static void loadPropertiesFromClasspath(String filename, Properties properties) {
		if(filename == null) throw new IllegalArgumentException("filename cannot be null");
		if(properties == null) throw new IllegalArgumentException("properties cannot be null");
		URL propertiesLocation = StackConfiguration.class.getResource(filename);
		if (null == propertiesLocation) {
			throw new IllegalArgumentException("Could not load property file from classpath: "+filename);
		}
		try {
			properties.load(propertiesLocation.openStream());
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	/**
	 * Add the properties from the settings file to the system properties if they are there.
	 */
	private static void addSettingsPropertiesToSystem(){
		Properties props;
		try {
			props = SettingsLoader.loadSettingsFile();
			if(props != null){
				Iterator it = props.keySet().iterator();
				while(it.hasNext()){
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
	 * @param url
	 * @param properties
	 * @return
	 */
	private static void loadPropertiesFromURL(String url, Properties properties) {
		if(url == null) throw new IllegalArgumentException("url cannot be null");
		if(properties == null) throw new IllegalArgumentException("properties cannot be null");
		URL propertiesLocation;
		try {
			propertiesLocation = new URL(url);
		} catch (MalformedURLException e1) {
			throw new IllegalArgumentException("Could not load property file from url: "+url, e1);
		}
		try {
			properties.load(propertiesLocation.openStream());
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	/**
	 * Throws the same RuntimeException when a required property is missing.
	 * @param propertyKey
	 * @param alternate
	 */
	private static void throwRequiredPropertyException(String propertyKey, String alternate){
		throw new RuntimeException("The property: "+propertyKey+" or its alternate: "+alternate+" is required and cannot be null");
	}
	
	/**
	 * The location of the property file that overrides configuration properties.
	 * @return
	 */
	public static String getPropertyFileURL() {
		String url = System.getProperty(StackConstants.PARAM1);
		if (url == null) url = System.getProperty(StackConstants.STACK_PROPERTY_FILE_URL);
		if(url == null) throwRequiredPropertyException(StackConstants.STACK_PROPERTY_FILE_URL, StackConstants.PARAM1);
		return url;
	}
	
	/**
	 * The encryption key used to read passwords in the configuration property file.
	 * @return
	 */
	public static String getEncryptionKey() {
		String ek = System.getProperty(StackConstants.PARAM2);
		if (ek == null) ek = System.getProperty(StackConstants.STACK_ENCRYPTION_KEY);
		if(ek == null) throwRequiredPropertyException(StackConstants.STACK_ENCRYPTION_KEY, StackConstants.PARAM2);
		return ek;
	}
	
	/**
	 * The name of the stack.
	 * @return
	 */
	public static String getStack() {
		String stack = System.getProperty(StackConstants.PARAM3);
		if(stack == null) stack = System.getProperty(StackConstants.STACK_PROPERTY_NAME);
		if(stack == null) throwRequiredPropertyException(StackConstants.STACK_PROPERTY_NAME, StackConstants.PARAM3);
		return stack;
	}
	/**
	 * The stack instance (i.e 'A', or 'B')
	 * @return
	 */
	public static String getStackInstance(){
		String instance = System.getProperty(StackConstants.PARAM4);
		if(instance == null) instance = System.getProperty(StackConstants.STACK_INSTANCE_PROPERTY_NAME);
		if(instance == null) throwRequiredPropertyException(StackConstants.STACK_INSTANCE_PROPERTY_NAME, StackConstants.PARAM4);
		return instance;
	}
	
	/**
	 * Get the IAM user ID (Access Key ID)
	 * @return
	 */
	public static String getIAMUserId(){
		// There are a few places where we can find this
		String id = System.getProperty("AWS_ACCESS_KEY_ID");
		if(id != null) return id;
		id = System.getProperty(StackConstants.STACK_IAM_ID);
		if (id == null) return null;
		id = id.trim();
		if ("".equals(id)) return null;
		return id;
	}
	
	/**
	 * Get the IAM user Key (Secret Access Key)
	 * @return
	 */
	public static String getIAMUserKey(){
		// There are a few places to look for this
		String key = System.getProperty("AWS_SECRET_KEY");
		if(key != null) return key;
		key = System.getProperty(StackConstants.STACK_IAM_KEY);
		if (key == null) return null;
		key = key.trim();
		if ("".equals(key)) return null;
		return key;
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

	public static Integer getS3ReadAccessExpiryMinutes() {
		return Integer.valueOf(getProperty("org.sagebionetworks.s3.readAccessExpiryMinutes"));
	}

	public static Integer getS3WriteAccessExpiryMinutes() {
		return Integer.valueOf(getProperty("org.sagebionetworks.s3.writeAccessExpiryMinutes"));
	}
	
	public static Integer getStsSessionDurationHours() {
		return Integer.valueOf(getProperty("org.sagebionetworks.sts.sessionDurationHours"));
	}
    
    public static String getTcgaWorkflowSnsTopic() {
		return getProperty("org.sagebionetworks.sns.topic.tcgaworkflow");
	}
	
	public static String getCrowdAPIApplicationKey() {
		return getDecryptedProperty("org.sagebionetworks.crowdApplicationKey");
	}	
	public static String getMailPassword() {
		return getDecryptedProperty("org.sagebionetworks.mailPW");
	}
	
	/**
	 * The database connection string used for the ID Generator.
	 * @return
	 */
	public String getIdGeneratorDatabaseConnectionUrl(){
		return getProperty("org.sagebionetworks.id.generator.database.connection.url");
	}
	
	/**
	 * The username used for the ID Generator.
	 * @return
	 */
	public String getIdGeneratorDatabaseUsername(){
		return getProperty("org.sagebionetworks.id.generator.database.username");
	}
	
	/**
	 * The password used for the ID Generator.
	 * @return
	 */
	public String getIdGeneratorDatabasePassword(){
		return getDecryptedProperty("org.sagebionetworks.id.generator.database.password");
	}
	
	public String getIdGeneratorDatabaseDriver(){
		return getProperty("org.sagebionetworks.id.generator.database.driver");
	}
	
	/**
	 * All of these keys are used to build up a map of JDO configurations
	 * passed to the JDOPersistenceManagerFactory
	 */
	private static String[] MAP_PROPERTY_NAME = new String[]{
		"javax.jdo.PersistenceManagerFactoryClass",
		"datanucleus.NontransactionalRead",
		"datanucleus.NontransactionalWrite",
		"javax.jdo.option.RetainValues",
		"datanucleus.autoCreateSchema",
		"datanucleus.validateConstraints",
		"datanucleus.validateTables",
		"datanucleus.transactionIsolation",
	};
		
	public Map<String, String> getRepositoryJDOConfigurationMap(){
		HashMap<String, String> map = new HashMap<String, String>();
		for(String name: MAP_PROPERTY_NAME){
			String value = getProperty(name);
			if(value == null) throw new IllegalArgumentException("Failed to find property: "+name);
			map.put(name, value);
		}
		map.put("javax.jdo.option.ConnectionURL", getRepositoryDatabaseConnectionUrl());
		map.put("javax.jdo.option.ConnectionDriverName", getRepositoryDatabaseDriver());
		map.put("javax.jdo.option.ConnectionUserName", getRepositoryDatabaseUsername());
		map.put("javax.jdo.option.ConnectionPassword", getRepositoryDatabasePassword());
		return map;
	}
	
	/**
	 * Driver for the repository service.
	 * @return
	 */
	public String getRepositoryDatabaseDriver(){
		return getProperty("org.sagebionetworks.id.generator.database.driver");
	}
	
	/**
	 * The repository database connection string.
	 * @return
	 */
	public String getRepositoryDatabaseConnectionUrl(){
		// First try to load the system property
		String jdbcConnection  = System.getProperty("JDBC_CONNECTION_STRING");
		if(jdbcConnection != null && !"".equals(jdbcConnection)) return jdbcConnection;
		// Now try the environment variable
		jdbcConnection = System.getenv("JDBC_CONNECTION_STRING");
		if(jdbcConnection != null && !"".equals(jdbcConnection)) return jdbcConnection;
		// Last try the stack configuration
		return getProperty("org.sagebionetworks.repository.database.connection.url");
	}
	
	/**
	 * The repository database username.
	 * @return
	 */
	public String getRepositoryDatabaseUsername(){
		return getProperty("org.sagebionetworks.repository.database.username");
	}
	
	/**
	 * The repository database password.
	 * @return
	 */
	public String getRepositoryDatabasePassword(){
		return getDecryptedProperty("org.sagebionetworks.repository.database.password");
	}
	
	
	/**
	 * Should the connection pool connections be validated?
	 * @return
	 */
	public String getDatabaseConnectionPoolShouldValidate(){
		return getProperty("org.sagebionetworks.pool.connection.validate");
	}
	
	/**
	 * The SQL used to validate pool connections
	 * @return
	 */
	public String getDatabaseConnectionPoolValidateSql(){
		return getProperty("org.sagebionetworks.pool.connection.validate.sql");
	}
	
	/**
	 * The minimum number of connections in the pool
	 * @return
	 */
	public String getDatabaseConnectionPoolMinNumberConnections(){
		return getProperty("org.sagebionetworks.pool.min.number.connections");
	}
	
	/**
	 * The maximum number of connections in the pool
	 * @return
	 */
	public String getDatabaseConnectionPoolMaxNumberConnections(){
		return getProperty("org.sagebionetworks.pool.max.number.connections");
	}

	/**
	 * @return The name of a user for integration tests
 	 */
	public static String getIntegrationTestUserOneName() {
		return getProperty("org.sagebionetworks.integration.test.username.one");
	}
	
	/**
	 * @return The password of a user for integration tests
 	 */
	public static String getIntegrationTestUserOnePassword() {
		return getProperty("org.sagebionetworks.integration.test.password.one");
	}
	
	/**
	 * @return The name of a second user for integration tests
 	 */
	public static String getIntegrationTestUserTwoName() {
		return getProperty("org.sagebionetworks.integration.test.username.two");
	}
	
	/**
	 * @return The password of a second user for integration tests
 	 */
	public static String getIntegrationTestUserTwoPassword() {
		return getProperty("org.sagebionetworks.integration.test.password.two");
	}
	
	public static Boolean getUseFederatedIamUsersLaunchFlag() {
		return Boolean.valueOf(getProperty("org.sagebionetowrks.launchFlags.useFederatedIamUsers"));
	}
}
