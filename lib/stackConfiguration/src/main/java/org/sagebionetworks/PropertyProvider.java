package org.sagebionetworks;

import java.io.IOException;
import java.util.Properties;

/**
 * Abstraction for loading static System.properties.
 *
 */
public interface PropertyProvider {

	/**
	 * Get the System.Properties.
	 * @return
	 */
	public Properties getSystemProperties();
	
	/**
	 * Get the Maven settings.xml file properties.
	 * @return
	 */
	public Properties getMavenSettingsProperties();
	
	/**
	 * Load the given property file from the classpath.
	 * 
	 * @param name
	 * @return
	 * @throws IOException 
	 */
	public Properties loadPropertiesFromClasspath(String name);
}
