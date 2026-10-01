package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.search.dsl.Combination;
import org.sagebionetworks.repo.model.search.dsl.CombinationParameters;
import org.sagebionetworks.repo.model.search.dsl.CombinationTechnique;
import org.sagebionetworks.repo.model.search.dsl.Normalization;
import org.sagebionetworks.repo.model.search.dsl.NormalizationProcessor;
import org.sagebionetworks.repo.model.search.dsl.NormalizationTechnique;
import org.sagebionetworks.repo.model.search.dsl.PhaseResultsProcessor;
import org.sagebionetworks.repo.model.search.dsl.SearchPipeline;
import org.sagebionetworks.repo.model.search.table.ListNamedSearchPipelinesRequest;
import org.sagebionetworks.repo.model.search.table.ListNamedSearchPipelinesResponse;
import org.sagebionetworks.repo.model.search.table.NamedSearchPipeline;

@ExtendWith(ITTestExtension.class)
public class ITSearchPipelineTest {

	private static final String ORG_NAME = "org.sagebionetworks";

	private final SynapseAdminClient adminSynapse;

	public ITSearchPipelineTest(SynapseAdminClient adminSynapse) {
		this.adminSynapse = adminSynapse;
	}

	@BeforeEach
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
	}

	@Test
	public void testCRUDWithPipelineSettings() throws SynapseException {
		// Names are unique per organization with no delete endpoint, so use a UUID
		// suffix to avoid collisions across re-runs of the test.
		String name = "IT_TEST_PIPELINE_" + UUID.randomUUID().toString().replace("-", "");
		NamedSearchPipeline toCreate = new NamedSearchPipeline()
				.setOrganizationName(ORG_NAME)
				.setName(name)
				.setDescription("Integration test search pipeline")
				.setSettings(settings(NormalizationTechnique.min_max, CombinationTechnique.arithmetic_mean,
						List.of(0.4, 0.3, 0.1, 0.1, 0.1)));

		// call under test
		NamedSearchPipeline created = adminSynapse.createSearchPipeline(toCreate);

		assertEquals(name, created.getName());
		assertEquals(toCreate.getSettings(), created.getSettings());

		// call under test
		NamedSearchPipeline fetched = adminSynapse.getSearchPipeline(created.getId());

		assertEquals(created, fetched);

		SearchPipeline updatedSettings = settings(NormalizationTechnique.l2, CombinationTechnique.harmonic_mean,
				List.of(0.2, 0.2, 0.2, 0.2, 0.2));
		fetched.setDescription("Updated description").setSettings(updatedSettings);

		// call under test
		NamedSearchPipeline updated = adminSynapse.updateSearchPipeline(fetched);

		assertEquals("Updated description", updated.getDescription());
		assertEquals(updatedSettings, updated.getSettings());
		assertNotEquals(created.getEtag(), updated.getEtag());

		// Pipelines accumulate across re-runs and list in ID order, so page until the new one appears.
		ListNamedSearchPipelinesRequest listRequest = new ListNamedSearchPipelinesRequest().setOrganizationName(ORG_NAME);
		boolean found = false;
		do {
			// call under test
			ListNamedSearchPipelinesResponse page = adminSynapse.listSearchPipelines(listRequest);
			found = page.getResults().contains(updated);
			listRequest.setNextPageToken(page.getNextPageToken());
		} while (!found && listRequest.getNextPageToken() != null);

		assertTrue(found);
	}

	private static SearchPipeline settings(NormalizationTechnique normalization, CombinationTechnique combination,
			List<Double> weights) {
		return new SearchPipeline().setPhase_results_processors(List.of(new PhaseResultsProcessor()
				.setNormalizationProcessor(new NormalizationProcessor()
						.setNormalization(new Normalization().setTechnique(normalization))
						.setCombination(new Combination()
								.setTechnique(combination)
								.setParameters(new CombinationParameters().setWeights(weights))))));
	}
}
