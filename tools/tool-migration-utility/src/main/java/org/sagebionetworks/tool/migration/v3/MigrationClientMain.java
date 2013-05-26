package org.sagebionetworks.tool.migration.v3;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.MigrationConfigurationImpl;

/**
 * The main entry point for the V3 data migration process.
 * @author jmhill
 *
 */
public class MigrationClientMain {

	static private Log log = LogFactory.getLog(MigrationClientMain.class);
	/**
	 * The main entry for for the V3 migration client.
	 * @param args
	 * @throws IOException 
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public static void main(String[] args) throws Exception {
		// First load the configuration
		MigrationConfigurationImpl configuration = new MigrationConfigurationImpl();
		loadConfigUsingArgs(configuration, args);		
		// Create the client factory
		SynapseClientFactory factory = new SynapseClientFactoryImpl(configuration);
		MigrationClient client = new MigrationClient(factory);
		try{
			client.migrateAllTypes(configuration.getMaximumBatchSize(), configuration.getWorkerTimeoutMs(), configuration.getRetryDenominator());
		}catch (Throwable e){
			log.error("Failed: "+e.getMessage(), e);
			System.exit(-1);
		}
		System.exit(0);
	}
	
	/**
	 * Load the configuration using the passed args.
	 * @param configuration 
	 * @param args
	 * @throws IOException
	 */
	public static void loadConfigUsingArgs(MigrationConfigurationImpl configuration, String[] args) throws IOException {
		if (args != null && args.length == 1) {
			// Load and validate from file
			String path = args[0];
			configuration.loadConfigurationFile(path);
		} else {
			// Validate System properties
			configuration.validateConfigurationProperties();
		}
	}
}
