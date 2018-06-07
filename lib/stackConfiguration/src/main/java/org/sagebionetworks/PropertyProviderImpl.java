package org.sagebionetworks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyProviderImpl implements PropertyProvider {

	@Override
	public Properties getSystemProperties() {
		return System.getProperties();
	}

	@Override
	public Properties getMavenSettingsProperties() {
		try {
			return SettingsLoader.loadSettingsFile();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Properties loadPropertiesFromClasspath(String fileName) {
		if (fileName == null) {
			throw new IllegalArgumentException("filename cannot be null");
		}
		try(InputStream input = ConfigurationPropertiesImpl.class.getResourceAsStream(fileName);){
			if(input == null) {
				throw new IllegalArgumentException("Cannot find file on classpath: "+fileName);
			}
			Properties properties = new Properties();
			properties.load(input);
			return properties;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
