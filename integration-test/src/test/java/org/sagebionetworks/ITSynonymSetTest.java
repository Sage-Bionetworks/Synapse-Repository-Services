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
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsRequest;
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsResponse;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersRequest;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersResponse;
import org.sagebionetworks.repo.model.search.table.SynonymRule;
import org.sagebionetworks.repo.model.search.table.SynonymRuleType;
import org.sagebionetworks.repo.model.search.table.SynonymSet;

@ExtendWith(ITTestExtension.class)
public class ITSynonymSetTest {

	private final SynapseAdminClient adminSynapse;
	private final List<String> toDelete = new ArrayList<>();

	public ITSynonymSetTest(SynapseAdminClient adminSynapse) {
		this.adminSynapse = adminSynapse;
	}

	@BeforeEach
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
	}

	@AfterEach
	public void after() {
		for (String id : toDelete) {
			try {
				adminSynapse.deleteSynonymSet(id);
			} catch (SynapseException e) {
				// ignore
			}
		}
	}

	@Test
	public void testCRUDWithSynonymRules() throws SynapseException {
		// Get org ID from bootstrapped analyzers
		ListTextAnalyzersResponse analyzers = adminSynapse.listTextAnalyzers(new ListTextAnalyzersRequest());
		String orgName = analyzers.getResults().get(0).getOrganizationName();

		// CREATE
		SynonymRule rule = new SynonymRule();
		rule.setRuleType(SynonymRuleType.EQUIVALENT);
		rule.setTerms(Arrays.asList("cancer", "tumor", "neoplasm"));

		SynonymSet toCreate = new SynonymSet();
		toCreate.setName("IT_TEST_SYNONYMS");
		toCreate.setDescription("Integration test synonym set");
		toCreate.setOrganizationName(orgName);
		toCreate.setRules(Arrays.asList(rule));

		// call under test
		SynonymSet created = adminSynapse.createSynonymSet(toCreate);
		assertNotNull(created.getId());
		assertNotNull(created.getEtag());
		assertEquals("IT_TEST_SYNONYMS", created.getName());
		assertEquals(1, created.getRules().size());
		toDelete.add(created.getId());

		// call under test
		SynonymSet fetched = adminSynapse.getSynonymSet(created.getId());
		assertEquals(created.getId(), fetched.getId());
		assertEquals(created.getEtag(), fetched.getEtag());
		assertEquals("IT_TEST_SYNONYMS", fetched.getName());
		assertEquals(1, fetched.getRules().size());
		assertEquals(SynonymRuleType.EQUIVALENT, fetched.getRules().get(0).getRuleType());

		// UPDATE
		fetched.setDescription("Updated description");
		SynonymRule additionalRule = new SynonymRule();
		additionalRule.setRuleType(SynonymRuleType.EXPLICIT);
		additionalRule.setTerms(Arrays.asList("AD", "Alzheimer's disease"));
		fetched.setRules(Arrays.asList(rule, additionalRule));

		// call under test
		SynonymSet updated = adminSynapse.updateSynonymSet(fetched);
		assertEquals("Updated description", updated.getDescription());
		assertEquals(2, updated.getRules().size());
		assertNotNull(updated.getEtag());

		// call under test
		ListSynonymSetsRequest listRequest = new ListSynonymSetsRequest();
		listRequest.setOrganizationName(orgName);
		ListSynonymSetsResponse listResponse = adminSynapse.listSynonymSets(listRequest);
		assertNotNull(listResponse.getResults());
		assertTrue(listResponse.getResults().stream().anyMatch(s -> created.getId().equals(s.getId())));

		// call under test
		adminSynapse.deleteSynonymSet(created.getId());
		toDelete.remove(created.getId());

		// call under test
		assertThrows(SynapseNotFoundException.class, () -> adminSynapse.getSynonymSet(created.getId()));
	}
}
