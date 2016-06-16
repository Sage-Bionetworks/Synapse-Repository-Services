package org.sagebionetworks;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Properties;

import org.jdom.JDOMException;
import org.junit.Ignore;
import org.junit.Test;

public class SettingsLoaderTest {
	
	@Ignore
	@Test
	public void testLoadSettings() throws IOException, JDOMException{
		// Load the settings file
		Properties settingsProps = SettingsLoader.loadSettingsFile();
		assertNotNull(settingsProps);
//		assertNotNull(settingsProps.get("org.sagebionetworks.stack.configuration.url"));
//		assertNotNull(settingsProps.get("org.sagebionetworks.stackEncryptionKey"));
	}

}
