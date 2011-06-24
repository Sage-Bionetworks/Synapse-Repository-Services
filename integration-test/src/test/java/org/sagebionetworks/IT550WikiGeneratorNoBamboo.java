package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.CRUDWikiGenerator;
import org.sagebionetworks.repo.ReadOnlyWikiGenerator;
import org.sagebionetworks.repo.WikiGenerator;

/**
 * This integration test confirms that our wiki generator still works
 * 
 * @author deflaux
 * 
 */
public class IT550WikiGeneratorNoBamboo {

	Logger log;
	StringWriter writer;
	WriterAppender appender;

	/**
	 * 
	 */
	@Before
	public void setUp() {
		writer = new StringWriter();
		appender = new WriterAppender(
				new PatternLayout("%d{ISO8601} %p - %m%n"), writer);
		appender.setThreshold(org.apache.log4j.Level.DEBUG);
		Logger.getRootLogger().addAppender(appender);
	}

	/**
	 * 
	 */
	@After
	public void tearDown() {
		Logger.getRootLogger().removeAppender(appender);
	}

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
		int numErrors = CRUDWikiGenerator.main(args);

		String output = writer.toString();

		// Make sure we are capturing log output
		assertTrue(-1 < output.indexOf("Delete a Dataset"));

		// Check for caught and handled errors
		assertFalse(-1 < output.indexOf(WikiGenerator.ERROR_PREFIX));
		assertFalse(-1 < output
				.indexOf("org.sagebionetworks.client.SynapseServiceException"));
		assertFalse(-1 < output
				.indexOf("org.sagebionetworks.client.SynapseUserException"));

		// Check the error count
		assertEquals(0, numErrors);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testReadOnlyWikiGenerator() throws Exception {

		String args[] = { "--repoEndpoint",
				StackConfiguration.getRepositoryServiceEndpoint(), "--authEndpoint",
				StackConfiguration.getAuthenticationServiceEndpoint(), "--username",
				Helpers.getIntegrationTestUser(), "--password",
				Helpers.getIntegrationTestUser() };
		int numErrors = ReadOnlyWikiGenerator.main(args);

		String output = writer.toString();

		// Make sure we are capturing log output
		assertTrue(-1 < output.indexOf("Access Control List Schema"));

		// Check for caught and handled errors
		assertFalse(-1 < output.indexOf(WikiGenerator.ERROR_PREFIX));
		assertFalse(-1 < output
				.indexOf("org.sagebionetworks.client.SynapseServiceException"));
		assertFalse(-1 < output
				.indexOf("org.sagebionetworks.client.SynapseUserException"));

		// Check the error count
		assertEquals(0, numErrors);
	}
}
