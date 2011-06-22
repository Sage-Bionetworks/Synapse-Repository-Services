package org.sagebionetworks;

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
public class IT550WikiGeneratorNoBamboo {

	/**
	 * @throws Exception
	 */
	@Test
	public void testCRUDWikiGenerator() throws Exception {
		String args[] = { "--repoEndpoint",
				Helpers.getRepositoryServiceBaseUrl(), "--authEndpoint",
				Helpers.getAuthServiceBaseUrl(), "--username",
				Helpers.getIntegrationTestUser(), "--password",
				Helpers.getIntegrationTestUser() };
		CRUDWikiGenerator.main(args);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testReadOnlyWikiGenerator() throws Exception {
		String args[] = { "--repoEndpoint",
				Helpers.getRepositoryServiceBaseUrl(), "--authEndpoint",
				Helpers.getAuthServiceBaseUrl(), "--username",
				Helpers.getIntegrationTestUser(), "--password",
				Helpers.getIntegrationTestUser() };
		ReadOnlyWikiGenerator.main(args);
	}
}
