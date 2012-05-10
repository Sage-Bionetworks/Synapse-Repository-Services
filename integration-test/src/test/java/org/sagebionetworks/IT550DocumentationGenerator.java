package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.client.EntityDocumentation;
import org.sagebionetworks.client.ManualProvenanceDocumentation;

/**
 * This integration test confirms that our wiki generator still works
 * 
 * @author deflaux
 * 
 */
public class IT550DocumentationGenerator {

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
	public void testEntityDocumentationGenerator() throws Exception {
		String args[] = { "--repoEndpoint",
				StackConfiguration.getRepositoryServiceEndpoint(),
				"--authEndpoint",
				StackConfiguration.getAuthenticationServicePrivateEndpoint(),
				"--username",
				StackConfiguration.getIntegrationTestUserOneName(),
				"--password",
				StackConfiguration.getIntegrationTestUserOnePassword() };
		EntityDocumentation.main(args);

		String output = writer.toString();

		// Make sure we are capturing log output
		assertTrue(-1 < output.indexOf("Search for entities"));

		// Check for exceptions
		assertEquals(-1, output.indexOf("Exception"));
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testManualProvenanceDocumentationGenerator() throws Exception {
		String args[] = { "--repoEndpoint",
				StackConfiguration.getRepositoryServiceEndpoint(),
				"--authEndpoint",
				StackConfiguration.getAuthenticationServicePrivateEndpoint(),
				"--username",
				StackConfiguration.getIntegrationTestUserOneName(),
				"--password",
				StackConfiguration.getIntegrationTestUserOnePassword() };
		ManualProvenanceDocumentation.main(args);

		String output = writer.toString();

		// Make sure we are capturing log output
		assertTrue(-1 < output.indexOf("Provenance"));

		// Check for exceptions
		assertEquals(-1, output.indexOf("Exception"));
	}
}
