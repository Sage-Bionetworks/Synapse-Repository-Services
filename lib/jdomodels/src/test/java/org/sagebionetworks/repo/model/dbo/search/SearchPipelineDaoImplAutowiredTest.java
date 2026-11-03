package org.sagebionetworks.repo.model.dbo.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.search.dsl.Combination;
import org.sagebionetworks.repo.model.search.dsl.CombinationParameters;
import org.sagebionetworks.repo.model.search.dsl.CombinationTechnique;
import org.sagebionetworks.repo.model.search.dsl.Normalization;
import org.sagebionetworks.repo.model.search.dsl.NormalizationProcessor;
import org.sagebionetworks.repo.model.search.dsl.NormalizationTechnique;
import org.sagebionetworks.repo.model.search.dsl.PhaseResultsProcessor;
import org.sagebionetworks.repo.model.search.dsl.SearchPipeline;
import org.sagebionetworks.repo.model.search.table.NamedSearchPipeline;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SearchPipelineDaoImplAutowiredTest {

	@Autowired
	private SearchPipelineDao searchPipelineDao;

	@Autowired
	private OrganizationDao organizationDao;

	private Long adminUserId;
	private String org1Id;
	private String org1Name;
	private String org2Id;
	private String org2Name;

	@BeforeEach
	public void before() {
		adminUserId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		searchPipelineDao.truncateAll();

		Organization org1 = organizationDao.createOrganization("test-org-" + UUID.randomUUID(), adminUserId);
		org1Id = org1.getId();
		org1Name = org1.getName();

		Organization org2 = organizationDao.createOrganization("test-org-" + UUID.randomUUID(), adminUserId);
		org2Id = org2.getId();
		org2Name = org2.getName();
	}

	@AfterEach
	public void after() {
		searchPipelineDao.truncateAll();
		if (org1Id != null) {
			organizationDao.deleteOrganization(org1Id);
		}
		if (org2Id != null) {
			organizationDao.deleteOrganization(org2Id);
		}
	}

	@Test
	public void testCreateAndGetWithSettings() {
		NamedSearchPipeline toCreate = newPipeline(org1Name, "keyword_heavy", "A test pipeline").setSettings(weightedSettings());

		// call under test
		NamedSearchPipeline created = searchPipelineDao.create(adminUserId, toCreate);

		assertNotNull(created.getId());
		assertNotNull(created.getEtag());
		assertNotNull(created.getCreatedOn());
		assertNotNull(created.getModifiedOn());
		NamedSearchPipeline expected = newPipeline(org1Name, "keyword_heavy", "A test pipeline")
				.setSettings(weightedSettings())
				.setId(created.getId())
				.setEtag(created.getEtag())
				.setCreatedBy(adminUserId.toString())
				.setCreatedOn(created.getCreatedOn())
				.setModifiedBy(adminUserId.toString())
				.setModifiedOn(created.getModifiedOn());
		assertEquals(expected, created);

		// call under test
		Optional<NamedSearchPipeline> fetched = searchPipelineDao.get(created.getId());

		assertEquals(Optional.of(created), fetched);
	}

	@Test
	public void testGetWithNonExistentId() {
		// call under test
		Optional<NamedSearchPipeline> result = searchPipelineDao.get("999999");

		assertEquals(Optional.empty(), result);
	}

	@Test
	public void testCreateWithDuplicateName() {
		searchPipelineDao.create(adminUserId, newPipeline(org1Name, "dup", null).setSettings(defaultSettings()));
		NamedSearchPipeline duplicate = newPipeline(org1Name, "dup", null).setSettings(weightedSettings());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			searchPipelineDao.create(adminUserId, duplicate);
		}).getMessage();

		assertEquals("A search pipeline with the same name already exists in this organization.", message);
	}

	@Test
	public void testUpdateWithModifiedSettingsAndDescription() {
		NamedSearchPipeline created = searchPipelineDao.create(adminUserId,
				newPipeline(org1Name, "test_update", "original").setSettings(defaultSettings()));
		String originalEtag = created.getEtag();

		created.setDescription("updated");
		created.setSettings(weightedSettings());

		// call under test
		NamedSearchPipeline updated = searchPipelineDao.update(adminUserId, created);

		assertEquals("updated", updated.getDescription());
		assertEquals(weightedSettings(), updated.getSettings());
		assertNotEquals(originalEtag, updated.getEtag());
		assertEquals(updated, searchPipelineDao.get(created.getId()).get());
	}

	@Test
	public void testUpdateWithChangedName() {
		NamedSearchPipeline created = searchPipelineDao.create(adminUserId,
				newPipeline(org1Name, "original_name", null).setSettings(defaultSettings()));

		created.setName("renamed");

		// call under test
		NamedSearchPipeline updated = searchPipelineDao.update(adminUserId, created);

		assertEquals("original_name", updated.getName());
	}

	@Test
	public void testUpdateWithStaleEtag() {
		NamedSearchPipeline created = searchPipelineDao.create(adminUserId,
				newPipeline(org1Name, "test_occ", null).setSettings(defaultSettings()));

		created.setDescription("first update");
		searchPipelineDao.update(adminUserId, created);

		created.setDescription("stale update");

		assertThrows(ConflictingUpdateException.class, () -> {
			// call under test
			searchPipelineDao.update(adminUserId, created);
		});
	}

	@Test
	public void testUpdateWithNonExistentId() {
		NamedSearchPipeline missing = newPipeline(org1Name, "missing", null)
				.setId("999999")
				.setEtag("etag")
				.setSettings(defaultSettings());

		assertThrows(NotFoundException.class, () -> {
			// call under test
			searchPipelineDao.update(adminUserId, missing);
		});
	}

	@Test
	public void testGetByOrganizationAndNameWithMatchingEntry() {
		NamedSearchPipeline created = searchPipelineDao.create(adminUserId,
				newPipeline(org1Name, "find_me", "target").setSettings(weightedSettings()));
		searchPipelineDao.create(adminUserId, newPipeline(org2Name, "find_me", "decoy").setSettings(defaultSettings()));

		// call under test
		Optional<NamedSearchPipeline> found = searchPipelineDao.getByOrganizationAndName(org1Name, "find_me");

		assertEquals(Optional.of(created), found);
	}

	@Test
	public void testGetByOrganizationAndNameWithNonExistentName() {
		// call under test
		Optional<NamedSearchPipeline> result = searchPipelineDao.getByOrganizationAndName(org1Name, "does_not_exist");

		assertEquals(Optional.empty(), result);
	}

	@Test
	public void testListWithMultipleOrganizations() {
		NamedSearchPipeline org1A = searchPipelineDao.create(adminUserId,
				newPipeline(org1Name, "org1_a", "first").setSettings(defaultSettings()));
		NamedSearchPipeline org1B = searchPipelineDao.create(adminUserId,
				newPipeline(org1Name, "org1_b", "second").setSettings(weightedSettings()));
		NamedSearchPipeline org2A = searchPipelineDao.create(adminUserId,
				newPipeline(org2Name, "org2_a", "third").setSettings(defaultSettings()));
		NamedSearchPipeline org2B = searchPipelineDao.create(adminUserId,
				newPipeline(org2Name, "org2_b", "fourth").setSettings(weightedSettings()));

		// call under test
		assertEquals(List.of(org1A, org1B), searchPipelineDao.list(org1Name, 10, 0));
		// call under test
		assertEquals(List.of(org2A, org2B), searchPipelineDao.list(org2Name, 10, 0));
		// call under test
		assertEquals(List.of(org1A, org1B, org2A, org2B), searchPipelineDao.listAll(10, 0));
	}

	@Test
	public void testListWithPaging() {
		NamedSearchPipeline first = searchPipelineDao.create(adminUserId,
				newPipeline(org1Name, "page_a", null).setSettings(defaultSettings()));
		NamedSearchPipeline second = searchPipelineDao.create(adminUserId,
				newPipeline(org1Name, "page_b", null).setSettings(weightedSettings()));
		NamedSearchPipeline third = searchPipelineDao.create(adminUserId,
				newPipeline(org1Name, "page_c", null).setSettings(defaultSettings()));

		// call under test
		assertEquals(List.of(first, second), searchPipelineDao.list(org1Name, 2, 0));
		// call under test
		assertEquals(List.of(third), searchPipelineDao.list(org1Name, 2, 2));
		// call under test
		assertEquals(List.of(second, third), searchPipelineDao.listAll(2, 1));
	}

	private static NamedSearchPipeline newPipeline(String organizationName, String name, String description) {
		return new NamedSearchPipeline()
				.setOrganizationName(organizationName)
				.setName(name)
				.setDescription(description);
	}

	private static SearchPipeline defaultSettings() {
		return new SearchPipeline().setPhase_results_processors(List.of(
				new PhaseResultsProcessor().setNormalizationProcessor(new NormalizationProcessor())));
	}

	private static SearchPipeline weightedSettings() {
		return new SearchPipeline().setPhase_results_processors(List.of(
				new PhaseResultsProcessor().setNormalizationProcessor(new NormalizationProcessor()
						.setNormalization(new Normalization().setTechnique(NormalizationTechnique.l2))
						.setCombination(new Combination()
								.setTechnique(CombinationTechnique.harmonic_mean)
								.setParameters(new CombinationParameters().setWeights(List.of(0.4, 0.3, 0.1, 0.1, 0.1)))))));
	}
}
