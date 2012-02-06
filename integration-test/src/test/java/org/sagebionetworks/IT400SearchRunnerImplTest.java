/**
 * 
 */
package org.sagebionetworks;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.dao.QueryRunner;
import org.sagebionetworks.tool.searchupdater.CloudSearchClient;
import org.sagebionetworks.tool.searchupdater.SearchUpdaterConfigurationImpl;
import org.sagebionetworks.tool.searchupdater.dao.SearchRunnerImpl;


/**
 * @author deflaux
 *
 */
public class IT400SearchRunnerImplTest {

	static CloudSearchClient csClient;
	
	/**
	 * @throws IOException
	 */
	@BeforeClass
	public static void setUp() throws IOException {
			SearchUpdaterConfigurationImpl configuration = new SearchUpdaterConfigurationImpl();
		csClient = configuration.createCloudSearchClient();
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testGetAllEntityData() throws Exception {
		QueryRunner searcher = new SearchRunnerImpl(csClient);
		List<EntityData> data = searcher.getAllEntityData(null);
		assertTrue(0 < data.size());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testGetTotalEntityCount() throws Exception {
		QueryRunner searcher = new SearchRunnerImpl(csClient);
		long total = searcher.getTotalEntityCount();
		assertTrue(0 < total);
	}
}
