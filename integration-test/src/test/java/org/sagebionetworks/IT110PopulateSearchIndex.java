package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.tool.migration.job.AggregateResult;
import org.sagebionetworks.tool.searchupdater.SearchMigrationDriver;

/**
 * This test runs after migration of a backup and should send many entities to
 * search.
 * 
 * @author deflaux
 * 
 */
public class IT110PopulateSearchIndex {

	static int NUM_RETRIES_ALLOWED = 3;

	/**
	 * @throws Exception
	 */
	@Test
	public void testPopulateSearchIndex() throws Exception {

		// Only run these tests on bamboo for now, later each developer might
		// configure his own search stack
		if (!StackConfiguration.getStack().equals("bamboo")) {
			return;
		}

		long sourceTotal = -1;
		long destTotal = -1;

		SearchMigrationDriver driver = new SearchMigrationDriver();
		int numTries = 0;

		while (NUM_RETRIES_ALLOWED > numTries) {
			numTries++;
			try {
				AggregateResult result = driver.migrateEntities();
				assertNotNull(result);
				sourceTotal = driver.getSourceEntityCount();
				destTotal = driver.getDestinationEntityCount();
				if (sourceTotal == destTotal)
					break;
			} catch (Exception e) {
				// allow a few exceptions
				if (NUM_RETRIES_ALLOWED == numTries) {
					throw e;
				}
			}
		}
		assertEquals(sourceTotal, destTotal);
		// There may be more entities than this, but there should always be at
		// least this many if we have successfully restored the backup
		// containing the SageBioCuration project
		assertTrue(500 < destTotal);
	}

}
