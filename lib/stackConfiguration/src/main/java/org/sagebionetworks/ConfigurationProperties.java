package org.sagebionetworks;

/**
 * Provides configuration property values for the stack. Property values are
 * provided to the stack via three routes:
 * <ol>
 * <li>Default property file: stack.properties</li>
 * <li>Properties from the local maven .m2/setting.xml file.</li>
 * <li>Java System.properties.</li>
 * </ol>
 * System.properties will override properties from settings and defaults. The
 * settings properties will override the default properties.
 *
 */
public interface ConfigurationProperties {

	/**
	 * Get a property value for the given property key.
	 * 
	 * @param propertyKey
	 * @return
	 * @throws IllegalArgumentException
	 *             if the key or value is null.
	 */
	public String getProperty(String propertyKey);

	/**
	 * Get the decrypted (plaintext) value for a given property key.
	 * 
	 * @param propertyKey
	 * @return The property 'org.sagebionetworks.stack.cmk.alias' must be set in
	 *         order for the decrypted value to be returned. If the property is not
	 *         set, the unencrypted value of the property will be returned
	 */
	public String getDecryptedProperty(String propertyKey);

}