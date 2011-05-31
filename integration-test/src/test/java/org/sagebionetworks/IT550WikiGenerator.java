package org.sagebionetworks;

import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.CRUDWikiGenerator;
import org.sagebionetworks.repo.ReadOnlyWikiGenerator;

/**
 * This integration test confirms that our wiki generator still works
 * 
 * TODO capture the log4j output and check it for errors? Or run the
 * generateRepositoryServiceWiki.sh script directly.
 * 
 * @author deflaux
 * 
 */
public class IT550WikiGenerator {

	/**
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testCRUDWikiGenerator() throws Exception {
		String args[] = { Helpers.getRepositoryServiceBaseUrl() };
		CRUDWikiGenerator.main(args);
	}

	/**
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testReadOnlyWikiGenerator() throws Exception {
		String args[] = { Helpers.getRepositoryServiceBaseUrl() };
		ReadOnlyWikiGenerator.main(args);
	}
}
