package org.sagebionetworks;

import org.junit.Test;
import org.sagebionetworks.repo.CRUDWikiGenerator;

/**
 * This integration test does something lame to ensure the database schema is in
 * place.
 * 
 * TODO think of a better way to do this for the in-memory db
 * 
 * @author deflaux
 * 
 */
public class IT005BootstrapDatabaseSchema {

	/**
	 * @throws Exception
	 */
	@Test
	public void testCRUDWikiGenerator() throws Exception {
		String args[] = { "--repoEndpoint",
				StackConfiguration.getRepositoryServiceEndpoint(), "--authEndpoint",
				StackConfiguration.getAuthenticationServiceEndpoint(), "--username",
				Helpers.getIntegrationTestUser(), "--password",
				Helpers.getIntegrationTestUser() };
		CRUDWikiGenerator.main(args);
	}
}
