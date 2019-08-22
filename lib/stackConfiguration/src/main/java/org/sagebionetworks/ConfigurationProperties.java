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
	 * returns true iff the properties has the given key
	 */
	public boolean hasProperty(String propertyKey);

	/**
	 * Get a property value for the given property key.
	 * 
	 * @param propertyKey
	 * @return
	 * @throws IllegalArgumentException
	 *             if the key or value is null.
	 */
	public String getProperty(String propertyKey);

}