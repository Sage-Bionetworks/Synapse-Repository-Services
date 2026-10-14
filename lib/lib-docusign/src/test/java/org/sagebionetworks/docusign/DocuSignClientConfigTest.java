package org.sagebionetworks.docusign;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;


/*
 * This test suite checks that all the Docusign parameters are getting passed through
 */
public class DocuSignClientConfigTest {

	private static StackConfiguration stackConfiguration;
	
	@BeforeAll
	public static void beforeAll() throws Exception {
		stackConfiguration = StackConfigurationSingleton.singleton();
	}
	
	@Test
	public void testGetDocuSignAccountId() throws Exception {
		assumeTrue(stackConfiguration.getDocuSignEnabled(),
				"DocuSign integration is disabled — skipping testGetDocuSignAccountId test.");
		
		// will throw exception if missing
		stackConfiguration.getDocuSignAccountId();
	}

	@Test
	public void testGetDocuSignIntegrationKey() throws Exception {
		assumeTrue(stackConfiguration.getDocuSignEnabled(),
				"DocuSign integration is disabled — skipping getDocuSignIntegrationKey test.");
		
		// will throw exception if missing
		stackConfiguration.getDocuSignIntegrationKey();
	}

	@Test
	public void testGetDocuSignPrivateKey() throws Exception {
		assumeTrue(stackConfiguration.getDocuSignEnabled(),
				"DocuSign integration is disabled — skipping getDocuSignPrivateKey test.");
				
		// will throw exception if missing
		stackConfiguration.getDocuSignPrivateKey();
	}

	@Test
	public void testGetDocuSignUserId() throws Exception {
		assumeTrue(stackConfiguration.getDocuSignEnabled(),
				"DocuSign integration is disabled — skipping getDocuSignUserId test.");
		
		// will throw exception if missing
		stackConfiguration.getDocuSignUserId();
	}

}
