package org.sagebionetworks.tool.migration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class Migrator {
	
	private static int batchSize;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public static void main(String[] args) throws IOException, SynapseException, JSONObjectAdapterException {
		// First load the configuration
		MigrationConfigurationImpl configuration = new MigrationConfigurationImpl();
		loadConfigUsingArgs(configuration, args);
		// Get the source and destination info
		// Create the two connections.
		final SynapseConnectionInfo sourceInfo = configuration.getSourceConnectionInfo();
		final SynapseConnectionInfo destInfo = configuration.getDestinationConnectionInfo();
		batchSize = configuration.getMaximumBatchSize();

		// Create a source and destination
		ClientFactoryImpl factory = new ClientFactoryImpl();
		SynapseAdministration sourceClient = factory.createNewConnection(sourceInfo);
		//SynapseAdministration destClient = factory.createNewConnection(destInfo);
		SynapseAdministration destClient = null;
		
		// Get counts by type
		Map<MigrationType, Long> sourceCounts = new HashMap<MigrationType, Long>();
		for (MigrationTypeCount mtc: sourceClient.getTypeCounts().getList()) {
			sourceCounts.put(mtc.getType(), mtc.getCount());
		}
		
		// TODO: Verify that all source types exist at destination
		
		// Migrate each type
		for (MigrationType mt: sourceClient.getPrimaryTypes().getList()) {
			migrateType(sourceClient, destClient, mt, sourceCounts.get(mt));
		}
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
	
	/**
	 * Migrate count rows of data of type t from src to dest
	 * @param src
	 * @param dest 
	 * @param t
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public static void migrateType(SynapseAdministration src, SynapseAdministration dest, MigrationType t, Long count) throws SynapseException, JSONObjectAdapterException {
		int limit = batchSize;
		int offset = 0;
		RowMetadataResult pRes = src.getRowMetadata(t, limit, offset);
	}
}
