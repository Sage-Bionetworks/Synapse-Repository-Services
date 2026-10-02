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
import org.sagebionetworks.repo.model.search.dsl.BoundMode;
import org.sagebionetworks.repo.model.search.dsl.Combination;
import org.sagebionetworks.repo.model.search.dsl.CombinationParameters;
import org.sagebionetworks.repo.model.search.dsl.CombinationTechnique;
import org.sagebionetworks.repo.model.search.dsl.LowerBound;
import org.sagebionetworks.repo.model.search.dsl.Normalization;
import org.sagebionetworks.repo.model.search.dsl.NormalizationParameters;
import org.sagebionetworks.repo.model.search.dsl.NormalizationProcessor;
import org.sagebionetworks.repo.model.search.dsl.NormalizationTechnique;
import org.sagebionetworks.repo.model.search.dsl.PhaseResultsProcessor;
import org.sagebionetworks.repo.model.search.dsl.SearchPipeline;
import org.sagebionetworks.repo.model.search.dsl.UpperBound;
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
		NormalizationParameters bounds = new NormalizationParameters()
				.setLower_bounds(List.of(lower(BoundMode.apply, 0.0), lower(BoundMode.clip, 2.5),
						lower(BoundMode.ignore, null), lower(BoundMode.apply, -1.0), lower(BoundMode.clip, 0.1)))
				.setUpper_bounds(List.of(upper(BoundMode.clip, 30.0), upper(BoundMode.apply, 1.0),
						upper(BoundMode.ignore, null), upper(BoundMode.apply, 10.0), upper(BoundMode.clip, 0.9)));
		NamedSearchPipeline toCreate = new NamedSearchPipeline()
				.setOrganizationName(ORG_NAME)
				.setName(name)
				.setDescription("Integration test search pipeline")
				.setSettings(settings(NormalizationTechnique.min_max, bounds, CombinationTechnique.arithmetic_mean,
						List.of(0.4, 0.3, 0.1, 0.1, 0.1)));

		// call under test
		NamedSearchPipeline created = adminSynapse.createSearchPipeline(toCreate);

		assertEquals(name, created.getName());
		assertEquals(toCreate.getSettings(), created.getSettings());

		// call under test
		NamedSearchPipeline fetched = adminSynapse.getSearchPipeline(created.getId());

		assertEquals(created, fetched);

		SearchPipeline updatedSettings = settings(NormalizationTechnique.l2, null, CombinationTechnique.harmonic_mean,
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

	private static SearchPipeline settings(NormalizationTechnique normalization, NormalizationParameters bounds,
			CombinationTechnique combination, List<Double> weights) {
		return new SearchPipeline().setPhase_results_processors(List.of(new PhaseResultsProcessor()
				.setNormalizationProcessor(new NormalizationProcessor()
						.setNormalization(new Normalization().setTechnique(normalization).setParameters(bounds))
						.setCombination(new Combination()
								.setTechnique(combination)
								.setParameters(new CombinationParameters().setWeights(weights))))));
	}

	private static LowerBound lower(BoundMode mode, Double minScore) {
		return new LowerBound().setMode(mode).setMin_score(minScore);
	}

	private static UpperBound upper(BoundMode mode, Double maxScore) {
		return new UpperBound().setMode(mode).setMax_score(maxScore);
	}
}
