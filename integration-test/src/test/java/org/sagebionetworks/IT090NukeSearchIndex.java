package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.tool.migration.job.AggregateResult;
import org.sagebionetworks.tool.searchupdater.SearchMigrationDriver;

/**
 * This test runs against a (nearly) empty repository service and therefore it
 * should delete most stuff in the search index (if anything was in there).
 * 
 * @author deflaux
 * 
 */
public class IT090NukeSearchIndex {

	static int NUM_RETRIES_ALLOWED = 3;

	/**
	 * @throws Exception
	 */
	@Test
	public void testNukeSearchIndex() throws Exception {

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
		// When we start the repository service a few entities are created so
		// this won't be zero but it should be a small number
		assertTrue(15 > destTotal);
	}

}
