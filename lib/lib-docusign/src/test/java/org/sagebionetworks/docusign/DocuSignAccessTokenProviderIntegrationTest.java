package org.sagebionetworks.docusign;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;

public class DocuSignAccessTokenProviderIntegrationTest {

	private static StackConfiguration stackConfiguration;
	private static DocuSignAccessTokenProvider provider;

	@BeforeAll
	public static void beforeAll() {
		stackConfiguration = StackConfigurationSingleton.singleton();
		assumeTrue(stackConfiguration.getDocuSignEnabled(),
				"DocuSign integration is disabled — skipping test.");

		StackDocuSignConfigProvider config = new StackDocuSignConfigProvider(stackConfiguration);
		provider = new DocuSignAccessTokenProvider(config);
	}

	@Test
	public void testGetAccessToken() {
		// call under test
		String token = provider.getAccessToken();

		assertNotNull(token);
	}

	@Test
	public void testGetAccessTokenReturnsCachedToken() {
		String first = provider.getAccessToken();

		// call under test
		String second = provider.getAccessToken();

		assertNotNull(first);
		assertNotNull(second);
	}

	@Test
	public void testInvalidateAndRefresh() {
		String first = provider.getAccessToken();
		provider.invalidateAccessToken();

		// call under test
		String second = provider.getAccessToken();

		assertNotNull(first);
		assertNotNull(second);
	}
}
