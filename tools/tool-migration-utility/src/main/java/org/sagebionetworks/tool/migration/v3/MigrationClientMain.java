package org.sagebionetworks.tool.migration.v3;

import java.io.IOException;

import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.MigrationConfigurationImpl;

/**
 * The main entry point for the V3 data migration process.
 * @author jmhill
 *
 */
public class MigrationClientMain {

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
		client.migrateFromSourceToDestination();
	}
	
	/**
	 * Load the configuration using the passed args.
	 * @param configuration 
	 * @param args
	 * @throws IOException
	 */
	public static void loadConfigUsingArgs(MigrationConfigurationImpl configuration, String[] args) throws IOException {
		// Load the location of the configuration property file
		if (args == null) {
			throw new IllegalArgumentException(	"The first argument must be the configuration property file path.");
		}
		if (args.length != 1) {
			throw new IllegalArgumentException(	"The first argument must be the configuration property file path. args.length: "+args.length);
		}
		String path = args[0];
		// Load all of the configuration information.
		configuration.loadConfigurationFile(path);
	}
}
