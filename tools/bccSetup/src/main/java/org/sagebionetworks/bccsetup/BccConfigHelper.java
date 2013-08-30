package org.sagebionetworks.bccsetup;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.TemplatedConfiguration;
import org.sagebionetworks.TemplatedConfigurationImpl;

/**
 * Configuration Helper to used to manage configuration specific to this
 * workflow and access general workflow configuration and clients.
 * 
 * @author deflaux
 */
public class BccConfigHelper {

	private static final String DEFAULT_PROPERTIES_FILENAME = "/bccsetup.properties";
	private static final String TEMPLATE_PROPERTIES = "/bccsetupTemplate.properties";
	private static final String BCC_ABBREVIATION_PREFIX = "abbrev_";

	private static final Logger log = LogManager.getLogger(BccConfigHelper.class
			.getName());

	private static TemplatedConfiguration configuration = null;
	private static Map<String, String> ABBREV2NAME = null;

	static {
		configuration = new TemplatedConfigurationImpl(
				DEFAULT_PROPERTIES_FILENAME, TEMPLATE_PROPERTIES);
		// Load the stack configuration the first time this class is referenced
		try {
			configuration.reloadConfiguration();
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			throw new RuntimeException(t);
		}

	}
	
	public static String getBCCSpreadsheetTitle() {
		return configuration
				.getProperty("org.sagebionetworks.bcc.spreadsheet.title");
		
	}
	


	


}
