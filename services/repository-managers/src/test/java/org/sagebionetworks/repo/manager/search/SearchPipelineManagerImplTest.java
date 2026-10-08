package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.dbo.search.SearchPipelineDao;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.search.dsl.Combination;
import org.sagebionetworks.repo.model.search.dsl.CombinationParameters;
import org.sagebionetworks.repo.model.search.dsl.BoundMode;
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
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class SearchPipelineManagerImplTest {

	private static final String ORG_NAME = "test.org";
	private static final String ORG_ID = "42";
	private static final String WEIGHTS_FIELD = "settings.combination.parameters.weights";
	private static final String LOWER_BOUNDS_FIELD = "settings.normalization.parameters.lower_bounds";
	private static final String UPPER_BOUNDS_FIELD = "settings.normalization.parameters.upper_bounds";

	@Mock
	private SearchPipelineDao mockSearchPipelineDao;
	@Mock
	private AccessControlListDAO mockAclDao;
	@Mock
	private OrganizationDao mockOrganizationDao;

	private SearchPipelineManagerImpl manager;

	private UserInfo admin;
	private UserInfo sageEmployee;
	private UserInfo nonSageUser;
	private UserInfo anonymous;

	@BeforeEach
	void setUp() {
		manager = new SearchPipelineManagerImpl(mockSearchPipelineDao, mockAclDao, mockOrganizationDao);
		admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		sageEmployee = new UserInfo(false, 2L, AuthorizationConstants.DEFAULT_REALM_ID,
				Set.of(2L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		nonSageUser = new UserInfo(false, 3L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(3L));
		long anonymousId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		anonymous = new UserInfo(false, anonymousId, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(anonymousId));
	}

	// --- create ---

	@Test
	public void testCreateWithAdmin() {
		NamedSearchPipeline request = validPipeline();
		NamedSearchPipeline created = validPipeline().setId("1");
		when(mockSearchPipelineDao.create(1L, request)).thenReturn(created);

		// call under test
		NamedSearchPipeline result = manager.create(admin, request);

		assertEquals(created, result);
		verifyNoInteractions(mockAclDao, mockOrganizationDao);
	}

	@Test
	public void testCreateWithSageEmployeeWithOrgAcl() {
		NamedSearchPipeline request = validPipeline();
		NamedSearchPipeline created = validPipeline().setId("1");
		stubOrgAcl(sageEmployee, ACCESS_TYPE.CREATE, AuthorizationStatus.authorized());
		when(mockSearchPipelineDao.create(2L, request)).thenReturn(created);

		// call under test
		NamedSearchPipeline result = manager.create(sageEmployee, request);

		assertEquals(created, result);
	}

	@Test
	public void testCreateWithSageEmployeeWithoutOrgAcl() {
		stubOrgAcl(sageEmployee, ACCESS_TYPE.CREATE, AuthorizationStatus.accessDenied("no"));

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.create(sageEmployee, validPipeline());
		});

		verifyNoInteractions(mockSearchPipelineDao);
	}

	@Test
	public void testCreateWithNonSageUser() {
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.create(nonSageUser, validPipeline());
		});

		verifyNoInteractions(mockSearchPipelineDao, mockAclDao, mockOrganizationDao);
	}

	@Test
	public void testCreateWithAnonymousUser() {
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.create(anonymous, validPipeline());
		});

		verifyNoInteractions(mockSearchPipelineDao, mockAclDao, mockOrganizationDao);
	}

	@Test
	public void testCreateWithInvalidName() {
		NamedSearchPipeline request = validPipeline().setName("1bad-name");

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.create(admin, request);
		}).getMessage();

		assertEquals(SearchResourceConstants.RESOURCE_NAME_PATTERN_MSG, message);
		verifyNoInteractions(mockSearchPipelineDao);
	}

	@Test
	public void testCreateWithNullSettings() {
		NamedSearchPipeline request = validPipeline().setSettings(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.create(admin, request);
		}).getMessage();

		assertEquals("settings is required.", message);
		verifyNoInteractions(mockSearchPipelineDao);
	}

	// --- save-time settings validation ---

	@Test
	public void testCreateWithNullProcessors() {
		assertCreateRejected(new SearchPipeline(),
				"settings.phase_results_processors must contain exactly one entry with 'normalization-processor' set");
	}

	@Test
	public void testCreateWithEmptyProcessors() {
		assertCreateRejected(new SearchPipeline().setPhase_results_processors(List.of()),
				"settings.phase_results_processors must contain exactly one entry with 'normalization-processor' set");
	}

	@Test
	public void testCreateWithTwoProcessors() {
		PhaseResultsProcessor processor = new PhaseResultsProcessor().setNormalizationProcessor(new NormalizationProcessor());
		assertCreateRejected(new SearchPipeline().setPhase_results_processors(List.of(processor, processor)),
				"settings.phase_results_processors must contain exactly one entry with 'normalization-processor' set");
	}

	@Test
	public void testCreateWithProcessorMissingNormalizationProcessor() {
		assertCreateRejected(new SearchPipeline().setPhase_results_processors(List.of(new PhaseResultsProcessor())),
				"settings.phase_results_processors must contain exactly one entry with 'normalization-processor' set");
	}

	@Test
	public void testCreateWithDefaultProcessor() {
		NamedSearchPipeline request = validPipeline().setSettings(settings(new NormalizationProcessor()));
		when(mockSearchPipelineDao.create(1L, request)).thenReturn(request);

		// call under test
		manager.create(admin, request);

		verify(mockSearchPipelineDao).create(1L, request);
	}

	@Test
	public void testCreateWithOneWeight() {
		assertCreateRejected(weightedSettings(1.0),
				WEIGHTS_FIELD + " must contain 2 to 5 entries; found 1");
	}

	@Test
	public void testCreateWithTwoWeights() {
		NamedSearchPipeline request = validPipeline().setSettings(weightedSettings(0.7, 0.3));
		when(mockSearchPipelineDao.create(1L, request)).thenReturn(request);

		// call under test
		manager.create(admin, request);

		verify(mockSearchPipelineDao).create(1L, request);
	}

	@Test
	public void testCreateWithSixWeights() {
		assertCreateRejected(weightedSettings(0.1, 0.1, 0.1, 0.1, 0.1, 0.5),
				WEIGHTS_FIELD + " must contain 2 to 5 entries; found 6");
	}

	@Test
	public void testCreateWithNegativeWeight() {
		assertCreateRejected(weightedSettings(0.5, -0.1, 0.2, 0.2, 0.2),
				WEIGHTS_FIELD + " entries must be in the range [0.0, 1.0]; found -0.1");
	}

	@Test
	public void testCreateWithWeightAboveOne() {
		assertCreateRejected(weightedSettings(1.1, 0.0, 0.0, 0.0, 0.0),
				WEIGHTS_FIELD + " entries must be in the range [0.0, 1.0]; found 1.1");
	}

	@Test
	public void testCreateWithNullWeight() {
		assertCreateRejected(weightedSettings(0.5, null, 0.2, 0.2, 0.1),
				WEIGHTS_FIELD + " entries must not be null");
	}

	@Test
	public void testCreateWithAllZeroWeights() {
		assertCreateRejected(weightedSettings(0.0, 0.0, 0.0, 0.0, 0.0),
				WEIGHTS_FIELD + " must sum to 1.0; found 0.0");
	}

	@Test
	public void testCreateWithWeightsSummingBelowOne() {
		assertCreateRejected(weightedSettings(0.2, 0.2, 0.2, 0.2, 0.1),
				WEIGHTS_FIELD + " must sum to 1.0; found 0.9");
	}

	@Test
	public void testCreateWithWeightsSummingAboveOne() {
		assertCreateRejected(weightedSettings(0.5, 0.5, 0.5, 0.5, 0.5),
				WEIGHTS_FIELD + " must sum to 1.0; found 2.5");
	}

	@Test
	public void testCreateWithBoundaryWeights() {
		NamedSearchPipeline request = validPipeline().setSettings(weightedSettings(1.0, 0.0, 0.0, 0.0, 0.0));
		when(mockSearchPipelineDao.create(1L, request)).thenReturn(request);

		// call under test
		manager.create(admin, request);

		verify(mockSearchPipelineDao).create(1L, request);
	}

	@Test
	public void testCreateWithParametersWithoutWeights() {
		NamedSearchPipeline request = validPipeline().setSettings(settings(new NormalizationProcessor()
				.setCombination(new Combination().setParameters(new CombinationParameters()))));
		when(mockSearchPipelineDao.create(1L, request)).thenReturn(request);

		// call under test
		manager.create(admin, request);

		verify(mockSearchPipelineDao).create(1L, request);
	}

	@Test
	public void testCreateWithZScoreAndHarmonicMean() {
		assertCreateRejected(zScoreSettings(CombinationTechnique.harmonic_mean),
				"settings: the 'z_score' normalization technique only supports the 'arithmetic_mean' combination technique");
	}

	@Test
	public void testCreateWithZScoreAndGeometricMean() {
		assertCreateRejected(zScoreSettings(CombinationTechnique.geometric_mean),
				"settings: the 'z_score' normalization technique only supports the 'arithmetic_mean' combination technique");
	}

	@Test
	public void testCreateWithZScoreAndArithmeticMean() {
		NamedSearchPipeline request = validPipeline().setSettings(zScoreSettings(CombinationTechnique.arithmetic_mean));
		when(mockSearchPipelineDao.create(1L, request)).thenReturn(request);

		// call under test
		manager.create(admin, request);

		verify(mockSearchPipelineDao).create(1L, request);
	}

	@Test
	public void testCreateWithZScoreAndDefaultCombination() {
		NamedSearchPipeline request = validPipeline().setSettings(settings(new NormalizationProcessor()
				.setNormalization(new Normalization().setTechnique(NormalizationTechnique.z_score))));
		when(mockSearchPipelineDao.create(1L, request)).thenReturn(request);

		// call under test
		manager.create(admin, request);

		verify(mockSearchPipelineDao).create(1L, request);
	}

	@Test
	public void testCreateWithZScoreAndCombinationWithoutTechnique() {
		NamedSearchPipeline request = validPipeline().setSettings(zScoreSettings(null));
		when(mockSearchPipelineDao.create(1L, request)).thenReturn(request);

		// call under test
		manager.create(admin, request);

		verify(mockSearchPipelineDao).create(1L, request);
	}

	@Test
	public void testCreateWithMinMaxAndHarmonicMean() {
		NamedSearchPipeline request = validPipeline().setSettings(settings(new NormalizationProcessor()
				.setNormalization(new Normalization().setTechnique(NormalizationTechnique.min_max))
				.setCombination(new Combination().setTechnique(CombinationTechnique.harmonic_mean))));
		when(mockSearchPipelineDao.create(1L, request)).thenReturn(request);

		// call under test
		manager.create(admin, request);

		verify(mockSearchPipelineDao).create(1L, request);
	}

	@Test
	public void testCreateWithBounds() {
		NamedSearchPipeline request = validPipeline().setSettings(boundedSettings(null,
				List.of(lower(BoundMode.apply, 0.0), lower(BoundMode.clip, -10000.0), lower(BoundMode.ignore, null),
						lower(null, 2.5), new LowerBound()),
				List.of(upper(BoundMode.apply, 1.0), upper(BoundMode.clip, 10000.0), upper(BoundMode.ignore, null),
						upper(null, 25.0), new UpperBound())));
		when(mockSearchPipelineDao.create(1L, request)).thenReturn(request);

		// call under test
		manager.create(admin, request);

		verify(mockSearchPipelineDao).create(1L, request);
	}

	@Test
	public void testCreateWithLowerBoundsOnlyAndMinMax() {
		NamedSearchPipeline request = validPipeline().setSettings(boundedSettings(NormalizationTechnique.min_max,
				fiveLowerBounds(0.5), null));
		when(mockSearchPipelineDao.create(1L, request)).thenReturn(request);

		// call under test
		manager.create(admin, request);

		verify(mockSearchPipelineDao).create(1L, request);
	}

	@Test
	public void testCreateWithParametersWithoutBounds() {
		NamedSearchPipeline request = validPipeline().setSettings(boundedSettings(null, null, null));
		when(mockSearchPipelineDao.create(1L, request)).thenReturn(request);

		// call under test
		manager.create(admin, request);

		verify(mockSearchPipelineDao).create(1L, request);
	}

	@Test
	public void testCreateWithOneLowerBound() {
		assertCreateRejected(boundedSettings(null, fiveLowerBounds(0.0).subList(0, 1), null),
				LOWER_BOUNDS_FIELD + " must contain 2 to 5 entries; found 1");
	}

	@Test
	public void testCreateWithTwoWeightsAndTwoBounds() {
		NamedSearchPipeline request = validPipeline().setSettings(settings(new NormalizationProcessor()
				.setNormalization(new Normalization().setParameters(new NormalizationParameters()
						.setLower_bounds(fiveLowerBounds(0.0).subList(3, 5))
						.setUpper_bounds(fiveUpperBounds(1.0).subList(3, 5))))
				.setCombination(new Combination().setParameters(new CombinationParameters()
						.setWeights(List.of(0.7, 0.3))))));
		when(mockSearchPipelineDao.create(1L, request)).thenReturn(request);

		// call under test
		manager.create(admin, request);

		verify(mockSearchPipelineDao).create(1L, request);
	}

	@Test
	public void testCreateWithWeightAndBoundCountMismatch() {
		assertCreateRejected(settings(new NormalizationProcessor()
				.setNormalization(new Normalization().setParameters(new NormalizationParameters()
						.setLower_bounds(fiveLowerBounds(0.0).subList(0, 3))))
				.setCombination(new Combination().setParameters(new CombinationParameters()
						.setWeights(List.of(0.7, 0.3))))),
				"settings: combination.parameters.weights, normalization.parameters.lower_bounds and"
						+ " normalization.parameters.upper_bounds must contain the same number of entries");
	}

	@Test
	public void testCreateWithLowerAndUpperBoundCountMismatch() {
		assertCreateRejected(boundedSettings(null, fiveLowerBounds(0.0).subList(0, 2), fiveUpperBounds(1.0)),
				"settings: combination.parameters.weights, normalization.parameters.lower_bounds and"
						+ " normalization.parameters.upper_bounds must contain the same number of entries");
	}

	@Test
	public void testCreateWithSixUpperBounds() {
		List<UpperBound> bounds = new ArrayList<>(fiveUpperBounds(1.0));
		bounds.add(upper(BoundMode.apply, 1.0));
		assertCreateRejected(boundedSettings(null, null, bounds),
				UPPER_BOUNDS_FIELD + " must contain 2 to 5 entries; found 6");
	}

	@Test
	public void testCreateWithNullLowerBound() {
		List<LowerBound> bounds = new ArrayList<>(fiveLowerBounds(0.0));
		bounds.set(2, null);
		assertCreateRejected(boundedSettings(null, bounds, null),
				LOWER_BOUNDS_FIELD + " entries must not be null");
	}

	@Test
	public void testCreateWithLowerBoundBelowRange() {
		assertCreateRejected(boundedSettings(null, fiveLowerBounds(-10000.5), null),
				LOWER_BOUNDS_FIELD + " scores must be in the range [-10000.0, 10000.0]; found -10000.5");
	}

	@Test
	public void testCreateWithUpperBoundAboveRange() {
		assertCreateRejected(boundedSettings(null, null, fiveUpperBounds(10000.5)),
				UPPER_BOUNDS_FIELD + " scores must be in the range [-10000.0, 10000.0]; found 10000.5");
	}

	@Test
	public void testCreateWithBoundsAndL2() {
		assertCreateRejected(boundedSettings(NormalizationTechnique.l2, fiveLowerBounds(0.0), null),
				"settings.normalization.parameters bounds only apply to the 'min_max' normalization technique");
	}

	@Test
	public void testCreateWithBoundsAndZScore() {
		assertCreateRejected(boundedSettings(NormalizationTechnique.z_score, null, fiveUpperBounds(1.0)),
				"settings.normalization.parameters bounds only apply to the 'min_max' normalization technique");
	}

	// --- get ---

	@Test
	public void testGetWithNonSageUser() {
		NamedSearchPipeline existing = validPipeline().setId("1");
		when(mockSearchPipelineDao.get("1")).thenReturn(Optional.of(existing));

		// call under test
		NamedSearchPipeline result = manager.get(nonSageUser, "1");

		assertEquals(existing, result);
		verifyNoInteractions(mockAclDao);
	}

	@Test
	public void testGetWithNonExistentId() {
		when(mockSearchPipelineDao.get("999")).thenReturn(Optional.empty());

		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			manager.get(admin, "999");
		}).getMessage();

		assertEquals("A search pipeline with the given id does not exist.", message);
	}

	// --- update ---

	@Test
	public void testUpdateWithAdmin() {
		NamedSearchPipeline request = validPipeline().setId("1").setDescription("updated");
		NamedSearchPipeline updated = validPipeline().setId("1").setDescription("updated").setEtag("new");
		when(mockSearchPipelineDao.get("1")).thenReturn(Optional.of(validPipeline().setId("1")));
		when(mockSearchPipelineDao.update(1L, request)).thenReturn(updated);

		// call under test
		NamedSearchPipeline result = manager.update(admin, request);

		assertEquals(updated, result);
		verifyNoInteractions(mockAclDao, mockOrganizationDao);
	}

	@Test
	public void testUpdateWithSageEmployeeWithOrgAcl() {
		NamedSearchPipeline request = validPipeline().setId("1");
		when(mockSearchPipelineDao.get("1")).thenReturn(Optional.of(validPipeline().setId("1")));
		stubOrgAcl(sageEmployee, ACCESS_TYPE.UPDATE, AuthorizationStatus.authorized());
		when(mockSearchPipelineDao.update(2L, request)).thenReturn(request);

		// call under test
		NamedSearchPipeline result = manager.update(sageEmployee, request);

		assertEquals(request, result);
	}

	@Test
	public void testUpdateWithSageEmployeeWithoutOrgAcl() {
		NamedSearchPipeline request = validPipeline().setId("1");
		when(mockSearchPipelineDao.get("1")).thenReturn(Optional.of(validPipeline().setId("1")));
		stubOrgAcl(sageEmployee, ACCESS_TYPE.UPDATE, AuthorizationStatus.accessDenied("no"));

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.update(sageEmployee, request);
		});

		verify(mockSearchPipelineDao).get("1");
		verifyNoMoreInteractions(mockSearchPipelineDao);
	}

	@Test
	public void testUpdateWithNonSageUser() {
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.update(nonSageUser, validPipeline().setId("1"));
		});

		verifyNoInteractions(mockSearchPipelineDao, mockAclDao, mockOrganizationDao);
	}

	@Test
	public void testUpdateWithAnonymousUser() {
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.update(anonymous, validPipeline().setId("1"));
		});

		verifyNoInteractions(mockSearchPipelineDao, mockAclDao, mockOrganizationDao);
	}

	@Test
	public void testUpdateWithInvalidSettings() {
		NamedSearchPipeline request = validPipeline().setId("1").setSettings(weightedSettings(1.0));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.update(admin, request);
		}).getMessage();

		assertEquals(WEIGHTS_FIELD + " must contain 2 to 5 entries; found 1", message);
		verifyNoInteractions(mockSearchPipelineDao);
	}

	@Test
	public void testUpdateWithOrganizationNameChange() {
		NamedSearchPipeline request = validPipeline().setId("1").setOrganizationName("other.org");
		when(mockSearchPipelineDao.get("1")).thenReturn(Optional.of(validPipeline().setId("1")));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.update(admin, request);
		}).getMessage();

		assertEquals(SearchResourceConstants.ORG_NAME_IMMUTABLE_MSG, message);
		verify(mockSearchPipelineDao).get("1");
		verifyNoMoreInteractions(mockSearchPipelineDao);
	}

	@Test
	public void testUpdateWithNameChange() {
		NamedSearchPipeline request = validPipeline().setId("1").setName("renamed");
		when(mockSearchPipelineDao.get("1")).thenReturn(Optional.of(validPipeline().setId("1")));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.update(admin, request);
		}).getMessage();

		assertEquals(SearchResourceConstants.NAME_IMMUTABLE_MSG, message);
		verify(mockSearchPipelineDao).get("1");
		verifyNoMoreInteractions(mockSearchPipelineDao);
	}

	@Test
	public void testUpdateWithNonExistentId() {
		NamedSearchPipeline request = validPipeline().setId("999");
		when(mockSearchPipelineDao.get("999")).thenReturn(Optional.empty());

		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			manager.update(admin, request);
		}).getMessage();

		assertEquals("A search pipeline with the given id does not exist.", message);
		verify(mockSearchPipelineDao).get("999");
		verifyNoMoreInteractions(mockSearchPipelineDao);
	}

	// --- list ---

	@Test
	public void testListWithNoOrganizationName() {
		List<NamedSearchPipeline> page = List.of(validPipeline().setId("1"));
		when(mockSearchPipelineDao.listAll(51L, 0L)).thenReturn(page);

		// call under test
		ListNamedSearchPipelinesResponse response = manager.list(nonSageUser, new ListNamedSearchPipelinesRequest());

		assertEquals(new ListNamedSearchPipelinesResponse().setResults(page), response);
		verifyNoMoreInteractions(mockSearchPipelineDao);
	}

	@Test
	public void testListWithOrganizationName() {
		List<NamedSearchPipeline> page = List.of(validPipeline().setId("1"));
		when(mockSearchPipelineDao.list(ORG_NAME, 51L, 0L)).thenReturn(page);

		// call under test
		ListNamedSearchPipelinesResponse response = manager.list(nonSageUser,
				new ListNamedSearchPipelinesRequest().setOrganizationName(ORG_NAME));

		assertEquals(new ListNamedSearchPipelinesResponse().setResults(page), response);
		verifyNoMoreInteractions(mockSearchPipelineDao);
	}

	@Test
	public void testListWithMoreResults() {
		List<NamedSearchPipeline> page = new ArrayList<>();
		for (int i = 0; i < 51; i++) {
			page.add(validPipeline().setId(String.valueOf(i)));
		}
		when(mockSearchPipelineDao.listAll(51L, 0L)).thenReturn(page);

		// call under test
		ListNamedSearchPipelinesResponse response = manager.list(nonSageUser, new ListNamedSearchPipelinesRequest());

		assertEquals(50, response.getResults().size());
		assertEquals("50a50", response.getNextPageToken());
	}

	@Test
	public void testListWithNoMoreResults() {
		when(mockSearchPipelineDao.listAll(51L, 0L)).thenReturn(new ArrayList<>(List.of(validPipeline().setId("1"))));

		// call under test
		ListNamedSearchPipelinesResponse response = manager.list(nonSageUser, new ListNamedSearchPipelinesRequest());

		assertNull(response.getNextPageToken());
	}

	private void assertCreateRejected(SearchPipeline settings, String expectedMessage) {
		NamedSearchPipeline request = validPipeline().setSettings(settings);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.create(admin, request);
		}).getMessage();

		assertEquals(expectedMessage, message);
		verifyNoInteractions(mockSearchPipelineDao);
	}

	private void stubOrgAcl(UserInfo user, ACCESS_TYPE accessType, AuthorizationStatus status) {
		when(mockOrganizationDao.getOrganizationByName(ORG_NAME)).thenReturn(new Organization().setId(ORG_ID));
		when(mockAclDao.canAccess(eq(user), eq(ORG_ID), eq(ObjectType.ORGANIZATION), eq(accessType))).thenReturn(status);
	}

	private static NamedSearchPipeline validPipeline() {
		return new NamedSearchPipeline()
				.setOrganizationName(ORG_NAME)
				.setName("keyword_heavy")
				.setSettings(weightedSettings(0.4, 0.3, 0.1, 0.1, 0.1));
	}

	private static SearchPipeline settings(NormalizationProcessor processor) {
		return new SearchPipeline().setPhase_results_processors(
				List.of(new PhaseResultsProcessor().setNormalizationProcessor(processor)));
	}

	private static SearchPipeline weightedSettings(Double... weights) {
		return settings(new NormalizationProcessor()
				.setCombination(new Combination().setParameters(new CombinationParameters().setWeights(Arrays.asList(weights)))));
	}

	private static SearchPipeline boundedSettings(NormalizationTechnique technique, List<LowerBound> lowerBounds,
			List<UpperBound> upperBounds) {
		return settings(new NormalizationProcessor().setNormalization(new Normalization().setTechnique(technique)
				.setParameters(new NormalizationParameters().setLower_bounds(lowerBounds).setUpper_bounds(upperBounds))));
	}

	private static LowerBound lower(BoundMode mode, Double minScore) {
		return new LowerBound().setMode(mode).setMin_score(minScore);
	}

	private static UpperBound upper(BoundMode mode, Double maxScore) {
		return new UpperBound().setMode(mode).setMax_score(maxScore);
	}

	private static List<LowerBound> fiveLowerBounds(Double minScore) {
		return List.of(lower(BoundMode.apply, 0.0), lower(BoundMode.apply, 0.0), lower(BoundMode.apply, 0.0),
				lower(BoundMode.apply, 0.0), lower(BoundMode.clip, minScore));
	}

	private static List<UpperBound> fiveUpperBounds(Double maxScore) {
		return List.of(upper(BoundMode.apply, 1.0), upper(BoundMode.apply, 1.0), upper(BoundMode.apply, 1.0),
				upper(BoundMode.apply, 1.0), upper(BoundMode.clip, maxScore));
	}

	private static SearchPipeline zScoreSettings(CombinationTechnique combinationTechnique) {
		return settings(new NormalizationProcessor()
				.setNormalization(new Normalization().setTechnique(NormalizationTechnique.z_score))
				.setCombination(new Combination().setTechnique(combinationTechnique)));
	}
}
