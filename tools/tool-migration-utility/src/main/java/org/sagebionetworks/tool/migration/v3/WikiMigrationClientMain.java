package org.sagebionetworks.tool.migration.v3;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.migration.WikiMigrationResult;
import org.sagebionetworks.repo.model.migration.WikiMigrationResultType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Main entry point for migrating V1 WikiPage tables to V2 tables.
 * @author hso
 *
 */
public class WikiMigrationClientMain {
	static private Log log = LogFactory.getLog(WikiMigrationClientMain.class);
	
	public static void main(String[] args) throws Exception {	
		// Create synapse java client and pass to migration
		SynapseAdminClientImpl synapse = new SynapseAdminClientImpl();
		if(args != null && args.length == 4) {
			synapse.setAuthEndpoint(args[0]);
			synapse.setRepositoryEndpoint(args[1]);
			synapse.setUserName(args[2]);
			synapse.setApiKey(args[3]);
		} else {
			throw new IllegalArgumentException("Invalid arguments. Please pass in: authEnd, repoEnd, userName, apiKey.");
		}
		SynapseAdminClient client = synapse;
		
		try {
			migrateWikisToV2(client);
		} catch(Exception e) {
			log.error("Migration of wikis to V2 failed. Error: " + e.getMessage(), e);
			System.exit(-1);
		}
		System.exit(0);
	}
	
	public static void migrateWikisToV2(SynapseAdminClient destination) throws SynapseException, JSONObjectAdapterException {
		log.info("Migration Wikis to V2");
		long limit = 10;
		long offset = 0;
		int failures = 0; // Number of failures
		log.info("Migrating group of wikis at offset: " + offset);
		PaginatedResults<WikiMigrationResult> results = destination.migrateWikisToV2(offset, limit);
		failures += processMigrationResults(results.getResults());
		long totalNumOfV1Wikis = results.getTotalNumberOfResults();
		offset += results.getResults().size();
		while(offset < totalNumOfV1Wikis) {
			// Migrate while we have not requested for the migration of all v1 wikis
			log.info("Migrating group of wikis at offset: " + offset);
			results = destination.migrateWikisToV2(offset, limit);
			failures += processMigrationResults(results.getResults());
			offset += results.getResults().size();
		}
		
		if(failures != 0) {
			throw new RuntimeException("There were " + failures + " failures during wiki migration.");
		} else {
			log.info("NO FAILURES!");
		}
		
	}
	
	private static int processMigrationResults(List<WikiMigrationResult> results) {
		int failures = 0;
		for(WikiMigrationResult result: results) {
			if(result.getResultType().equals(WikiMigrationResultType.FAILURE)) {
				failures++;
				log.info("[MIGRATION FAILURE] " + result.getMessage());
			}
		}
		// Return the number of failures in these results
		return failures;
	}
}
