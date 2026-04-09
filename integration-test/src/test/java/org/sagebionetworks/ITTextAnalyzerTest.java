package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersRequest;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersResponse;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.model.search.table.TextAnalyzerSettings;

@ExtendWith(ITTestExtension.class)
public class ITTextAnalyzerTest {

	private final SynapseAdminClient adminSynapse;
	private final List<String> analyzersToDelete = new ArrayList<>();

	public ITTextAnalyzerTest(SynapseAdminClient adminSynapse) {
		this.adminSynapse = adminSynapse;
	}

	@BeforeEach
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
	}

	@AfterEach
	public void after() {
		for (String id : analyzersToDelete) {
			try {
				adminSynapse.deleteTextAnalyzer(id);
			} catch (SynapseException e) {
				// ignore
			}
		}
	}

	@Test
	public void testTextAnalyzerCRUD() throws SynapseException {
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
		toCreate.setName("IT_TEST_ANALYZER");
		toCreate.setDescription("Integration test analyzer");
		toCreate.setOrganizationName(orgName);
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setFilterOrder(Arrays.asList("lowercase"));
		toCreate.setSettings(settings);

		TextAnalyzer created = adminSynapse.createTextAnalyzer(toCreate);
		assertNotNull(created.getId());
		assertNotNull(created.getEtag());
		assertEquals("IT_TEST_ANALYZER", created.getName());
		analyzersToDelete.add(created.getId());

		// GET
		TextAnalyzer fetched = adminSynapse.getTextAnalyzer(created.getId());
		assertEquals(created.getId(), fetched.getId());
		assertEquals(created.getEtag(), fetched.getEtag());
		assertEquals("IT_TEST_ANALYZER", fetched.getName());

		// UPDATE
		fetched.setDescription("Updated description");
		TextAnalyzer updated = adminSynapse.updateTextAnalyzer(fetched);
		assertEquals("Updated description", updated.getDescription());
		assertNotNull(updated.getEtag());

		// LIST by org
		ListTextAnalyzersRequest orgRequest = new ListTextAnalyzersRequest();
		orgRequest.setOrganizationName(orgName);
		ListTextAnalyzersResponse orgResponse = adminSynapse.listTextAnalyzers(orgRequest);
		assertNotNull(orgResponse.getResults());
		assertTrue(orgResponse.getResults().stream().anyMatch(a -> created.getId().equals(a.getId())));

		// DELETE
		adminSynapse.deleteTextAnalyzer(created.getId());
		analyzersToDelete.remove(created.getId());

		// Verify deleted
		assertThrows(SynapseNotFoundException.class, () -> adminSynapse.getTextAnalyzer(created.getId()));
	}

}
