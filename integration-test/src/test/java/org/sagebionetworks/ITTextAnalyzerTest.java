package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersRequest;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersResponse;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.model.search.table.TextAnalyzerSettings;

@ExtendWith(ITTestExtension.class)
public class ITTextAnalyzerTest {

	private final SynapseAdminClient adminSynapse;

	public ITTextAnalyzerTest(SynapseAdminClient adminSynapse) {
		this.adminSynapse = adminSynapse;
	}

	@BeforeEach
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
	}

	@Test
	public void testCRUDWithTextAnalyzerSettings() throws SynapseException {
		// The org.sagebionetworks organization is bootstrapped on startup
		// List system analyzers to get the organization ID
		ListTextAnalyzersRequest listRequest = new ListTextAnalyzersRequest();
		ListTextAnalyzersResponse listResponse = adminSynapse.listTextAnalyzers(listRequest);
		assertNotNull(listResponse.getResults());
		// System analyzers are bootstrapped, so there should be at least 6
		assertTrue(listResponse.getResults().size() >= 6);

		String orgName = listResponse.getResults().get(0).getOrganizationName();

		// CREATE
		TextAnalyzer toCreate = new TextAnalyzer();
		toCreate.setName("IT_TEST_ANALYZER_" + UUID.randomUUID().toString().replace("-", ""));
		toCreate.setDescription("Integration test analyzer");
		toCreate.setOrganizationName(orgName);
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setFilterOrder(Arrays.asList("lowercase"));
		toCreate.setSettings(settings);

		// call under test
		TextAnalyzer created = adminSynapse.createTextAnalyzer(toCreate);
		assertNotNull(created.getId());
		assertNotNull(created.getEtag());
		assertEquals(toCreate.getName(), created.getName());

		// call under test
		TextAnalyzer fetched = adminSynapse.getTextAnalyzer(created.getId());
		assertEquals(created.getId(), fetched.getId());
		assertEquals(created.getEtag(), fetched.getEtag());
		assertEquals(toCreate.getName(), fetched.getName());

		// call under test
		fetched.setDescription("Updated description");
		TextAnalyzer updated = adminSynapse.updateTextAnalyzer(fetched);
		assertEquals("Updated description", updated.getDescription());
		assertNotNull(updated.getEtag());

		// call under test
		ListTextAnalyzersRequest orgRequest = new ListTextAnalyzersRequest();
		orgRequest.setOrganizationName(orgName);
		ListTextAnalyzersResponse orgResponse = adminSynapse.listTextAnalyzers(orgRequest);
		assertNotNull(orgResponse.getResults());
		assertTrue(orgResponse.getResults().stream().anyMatch(a -> created.getId().equals(a.getId())));

	}
}
