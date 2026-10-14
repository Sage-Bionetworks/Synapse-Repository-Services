package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import org.sagebionetworks.repo.model.dbo.search.SynonymSetDao;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersRequest;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersResponse;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
public class TextAnalyzerManagerImplTest {

	private static final String VALID_SETTINGS =
			"{\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"}}}";
	private static final String SETTINGS_WITH_REF =
			"{\"filter\":{\"med\":{\"$ref\":\"biomed-medical_terms\"}},"
			+ "\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\","
			+ "\"filter\":[\"med\"]}}}";

	@Mock
	private TextAnalyzerDao textAnalyzerDao;
	@Mock
	private AccessControlListDAO aclDao;
	@Mock
	private OrganizationDao organizationDao;
	@Mock
	private SynonymSetDao synonymSetDao;
	@Mock
	private OpenSearchManager openSearchManager;

	@InjectMocks
	private TextAnalyzerManagerImpl manager;

	private UserInfo sageUser;
	private UserInfo nonSageUser;
	private UserInfo adminUser;

	@BeforeEach
	void setUp() {
		sageUser = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));

		nonSageUser = new UserInfo(false, 2L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(2L));

		adminUser = new UserInfo(true, 3L, AuthorizationConstants.DEFAULT_REALM_ID);
	}

	// --- Create authorization ---

	@Test
	public void testCreateAsNonSageUserThrows() {
		TextAnalyzer input = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(VALID_SETTINGS);

		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.create(nonSageUser, input));
		verifyNoMoreInteractions(textAnalyzerDao);
	}

	@Test
	public void testCreateAsAnonymousThrows() {
		UserInfo anon = new UserInfo(false, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID, Set.of(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId()));

		TextAnalyzer input = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(VALID_SETTINGS);

		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.create(anon, input));
	}

	@Test
	public void testCreateWithoutOrgAclThrows() {
		TextAnalyzer input = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(VALID_SETTINGS);
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.CREATE)))
			.thenReturn(AuthorizationStatus.accessDenied("no"));

		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.create(sageUser, input));
	}

	@Test
	public void testCreateHappyPath() {
		TextAnalyzer input = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(VALID_SETTINGS);
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.CREATE)))
			.thenReturn(AuthorizationStatus.authorized());
		when(textAnalyzerDao.create(any(), eq(1L))).thenReturn(input.setId("1000"));

		// call under test
		TextAnalyzer result = manager.create(sageUser, input);

		assertNotNull(result);
		assertEquals("1000", result.getId());
	}

	@Test
	public void testCreateAsAdminBypassesAcl() {
		TextAnalyzer input = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(VALID_SETTINGS);
		when(textAnalyzerDao.create(any(), eq(3L))).thenReturn(input.setId("1000"));

		// call under test
		TextAnalyzer result = manager.create(adminUser, input);

		assertNotNull(result);
		verifyNoMoreInteractions(aclDao);
	}

	// --- Get ---

	@Test
	public void testGetExisting() {
		TextAnalyzer analyzer = new TextAnalyzer().setId("1");
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(analyzer));

		// call under test
		assertEquals("1", manager.get(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), 1L).getId());
		verifyNoMoreInteractions(aclDao);
	}

	@Test
	public void testGetNotFoundThrows() {
		when(textAnalyzerDao.get(999L)).thenReturn(Optional.empty());

		// call under test
		assertThrows(NotFoundException.class, () -> manager.get(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), 999L));
	}

	// --- Update authorization ---

	@Test
	public void testUpdateAsNonSageUserThrows() {
		TextAnalyzer input = new TextAnalyzer()
			.setId("1").setOrganizationName("test-org").setName("updated").setSettings(VALID_SETTINGS);

		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.update(nonSageUser, input));
		verifyNoMoreInteractions(textAnalyzerDao);
	}

	@Test
	public void testUpdateWithoutOrgAclThrows() {
		TextAnalyzer input = new TextAnalyzer()
			.setId("1").setOrganizationName("test-org").setName("test_name").setSettings(VALID_SETTINGS);
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(new TextAnalyzer().setId("1").setOrganizationName("test-org").setName("test_name")));
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.UPDATE)))
			.thenReturn(AuthorizationStatus.accessDenied("no"));

		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.update(sageUser, input));
	}

	@Test
	public void testUpdateNotFoundThrows() {
		TextAnalyzer input = new TextAnalyzer()
			.setId("999").setOrganizationName("test-org").setName("updated").setSettings(VALID_SETTINGS);
		when(textAnalyzerDao.get(999L)).thenReturn(Optional.empty());

		// call under test
		assertThrows(NotFoundException.class, () -> manager.update(sageUser, input));
	}

	@Test
	public void testUpdateRejectsOrgNameMismatch() {
		TextAnalyzer input = new TextAnalyzer()
			.setId("1").setOrganizationName("other-org").setName("updated").setSettings(VALID_SETTINGS);
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(new TextAnalyzer().setId("1").setOrganizationName("test-org")));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> manager.update(sageUser, input));
		assertEquals(SearchResourceConstants.ORG_NAME_IMMUTABLE_MSG, ex.getMessage());
	}

	@Test
	public void testUpdateWithNameChangeThrows() {
		TextAnalyzer input = new TextAnalyzer()
			.setId("1").setOrganizationName("test-org").setName("new_name").setSettings(VALID_SETTINGS);
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(
			new TextAnalyzer().setId("1").setOrganizationName("test-org").setName("original_name")));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> manager.update(sageUser, input));

		assertEquals(SearchResourceConstants.NAME_IMMUTABLE_MSG, ex.getMessage());
		verify(textAnalyzerDao, never()).update(any(), any());
	}

	@Test
	public void testUpdateHappyPath() {
		TextAnalyzer input = new TextAnalyzer()
			.setId("1").setOrganizationName("test-org").setName("test_name").setDescription("updated description").setSettings(VALID_SETTINGS);
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(new TextAnalyzer().setId("1").setOrganizationName("test-org").setName("test_name")));
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.UPDATE)))
			.thenReturn(AuthorizationStatus.authorized());
		when(textAnalyzerDao.update(any(), eq(1L))).thenReturn(input);

		// call under test
		TextAnalyzer result = manager.update(sageUser, input);

		assertEquals("test_name", result.getName());
	}

	// --- Delete authorization ---

	@Test
	public void testDeleteAsNonSageUserThrows() {
		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.delete(nonSageUser, 1L));
		verifyNoMoreInteractions(textAnalyzerDao);
	}

	@Test
	public void testDeleteNotFoundThrows() {
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.empty());

		// call under test
		assertThrows(NotFoundException.class, () -> manager.delete(adminUser, 1L));
	}

	@Test
	public void testDeleteChecksOrgAclFromStoredEntity() {
		TextAnalyzer existing = new TextAnalyzer().setId("1").setOrganizationName("other-org");
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(existing));
		when(organizationDao.getOrganizationByName("other-org")).thenReturn(new Organization().setId("99"));
		when(aclDao.canAccess(any(UserInfo.class), eq("99"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.DELETE)))
			.thenReturn(AuthorizationStatus.accessDenied("no"));

		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.delete(sageUser, 1L));
		verify(aclDao).canAccess(any(UserInfo.class), eq("99"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.DELETE));
	}

	@Test
	public void testDeleteBlockedWhenReferencedByForeignKey() {
		TextAnalyzer existing = new TextAnalyzer().setId("1").setOrganizationName("test-org");
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(existing));
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.DELETE)))
			.thenReturn(AuthorizationStatus.authorized());
		doThrow(new DataIntegrityViolationException("FK constraint"))
			.when(textAnalyzerDao).delete(1L);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> manager.delete(sageUser, 1L));
		assertTrue(ex.getMessage().contains("still referenced"));
	}

	@Test
	public void testDeleteHappyPath() {
		TextAnalyzer existing = new TextAnalyzer().setId("1").setOrganizationName("test-org");
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(existing));
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.DELETE)))
			.thenReturn(AuthorizationStatus.authorized());

		// call under test
		manager.delete(sageUser, 1L);

		verify(textAnalyzerDao).delete(1L);
	}

	// --- List / pagination ---

	@Test
	public void testListByOrganizationDelegatesToDao() {
		when(textAnalyzerDao.listByOrganization(eq("test-org"), anyLong(), anyLong()))
			.thenReturn(Arrays.asList(new TextAnalyzer()));

		ListTextAnalyzersRequest request = new ListTextAnalyzersRequest();
		request.setOrganizationName("test-org");

		// call under test
		ListTextAnalyzersResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);

		assertEquals(1, response.getResults().size());
	}

	@Test
	public void testListAllWhenNoOrgName() {
		when(textAnalyzerDao.listAll(anyLong(), anyLong()))
			.thenReturn(Arrays.asList(new TextAnalyzer()));

		ListTextAnalyzersRequest request = new ListTextAnalyzersRequest();

		// call under test
		ListTextAnalyzersResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);

		assertEquals(1, response.getResults().size());
		verify(textAnalyzerDao).listAll(anyLong(), anyLong());
		verify(textAnalyzerDao, never()).listByOrganization(anyString(), anyLong(), anyLong());
	}

	@Test
	public void testListReturnsNextPageTokenWhenMoreResults() {
		// NextPageToken default limit is 50, so limitForQuery is 51
		List<TextAnalyzer> page = new ArrayList<>();
		for (int i = 0; i < 51; i++) {
			page.add(new TextAnalyzer().setId(String.valueOf(i)));
		}
		when(textAnalyzerDao.listAll(eq(51L), eq(0L))).thenReturn(page);

		ListTextAnalyzersRequest request = new ListTextAnalyzersRequest();

		// call under test
		ListTextAnalyzersResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);

		assertNotNull(response.getNextPageToken(), "Expected a next page token when DAO returns more than limit results");
		assertEquals(50, response.getResults().size());
	}

	@Test
	public void testListReturnsNullNextPageTokenWhenNoMoreResults() {
		List<TextAnalyzer> page = Arrays.asList(new TextAnalyzer().setId("1"));
		when(textAnalyzerDao.listAll(eq(51L), eq(0L))).thenReturn(page);

		ListTextAnalyzersRequest request = new ListTextAnalyzersRequest();

		// call under test
		ListTextAnalyzersResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);

		assertNull(response.getNextPageToken(), "Expected null next page token when all results fit in one page");
		assertEquals(1, response.getResults().size());
	}

	// --- Settings validation ---

	@Test
	public void testCreateWithMissingSettingsThrows() {
		TextAnalyzer bad = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(null);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.create(adminUser, bad));
		verifyNoMoreInteractions(textAnalyzerDao);
	}

	@Test
	public void testCreateWithMalformedSettingsJsonThrows() {
		TextAnalyzer bad = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings("{not_valid");
		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> manager.create(adminUser, bad));
		assertTrue(e.getMessage().startsWith("Invalid JSON"));
		verify(textAnalyzerDao, never()).create(any(), any());
	}

	@Test
	public void testCreateWithInvalidResourceNameThrows() {
		TextAnalyzer bad = new TextAnalyzer()
			.setOrganizationName("test-org").setName("9invalid").setSettings(VALID_SETTINGS);
		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> manager.create(adminUser, bad));
		assertEquals(SearchResourceConstants.RESOURCE_NAME_PATTERN_MSG, e.getMessage());
	}

	@Test
	public void testCreateWithRefAndAllExistResolves() {
		when(synonymSetDao.findNonExistentNames(eq(Collections.singletonList("biomed-medical_terms"))))
			.thenReturn(Collections.emptyList());
		// resolveRefs walks the parsed settings and calls getByQualifiedNames once per ref;
		// stub it so the AOSS-side validate hand-off has a real definition to substitute.
		when(synonymSetDao.getByQualifiedNames(eq(Collections.singletonList("biomed-medical_terms"))))
			.thenReturn(Collections.singletonMap("biomed-medical_terms",
					new SynonymSet().setDefinition("{\"type\":\"synonym_graph\",\"synonyms\":[\"a, b\"]}")));
		when(textAnalyzerDao.create(argThat(a -> a != null && SETTINGS_WITH_REF.equals(a.getSettings())), eq(3L)))
			.thenReturn(new TextAnalyzer().setId("1"));
		TextAnalyzer request = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(SETTINGS_WITH_REF);

		// call under test
		manager.create(adminUser, request);

		verify(synonymSetDao).findNonExistentNames(Collections.singletonList("biomed-medical_terms"));
		// The resolved settings handed to AOSS must have the synonym definition substituted
		// in place of the {"$ref":"biomed-medical_terms"} marker — in the typed model, the
		// med filter resolves to a SynonymGraph variant.
		verify(openSearchManager).validateAnalyzerSettings(argThat(settings -> settings != null
			&& settings.filter().get("med") != null
			&& settings.filter().get("med").definition() != null
			&& settings.filter().get("med").definition().isSynonymGraph()));
		verify(textAnalyzerDao).create(argThat(a -> a != null && SETTINGS_WITH_REF.equals(a.getSettings())), eq(3L));
	}

	@Test
	public void testCreateWithoutRefsStillCallsAossValidate() {
		// No $refs in settings — manager must still hand the parsed tree to the AOSS-side
		// validate probe so component shape / chain ordering get checked.
		when(textAnalyzerDao.create(argThat(a -> a != null && VALID_SETTINGS.equals(a.getSettings())), eq(3L)))
			.thenReturn(new TextAnalyzer().setId("1"));
		TextAnalyzer request = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(VALID_SETTINGS);

		// call under test
		manager.create(adminUser, request);

		verify(openSearchManager).validateAnalyzerSettings(argThat(settings -> settings != null
			&& settings.analyzer().get("default") != null
			&& settings.analyzer().get("default").isCustom()
			&& "standard".equals(settings.analyzer().get("default").custom().tokenizer())));
		verify(textAnalyzerDao).create(argThat(a -> a != null && VALID_SETTINGS.equals(a.getSettings())), eq(3L));
	}

	@Test
	public void testCreateWhenAossValidateRejectsThrows() {
		// AOSS rejects the analyzer at the _analyze probe — manager must propagate the
		// IllegalArgumentException without persisting the row.
		doThrow(new IllegalArgumentException("Invalid analyzer configuration: bogus tokenizer"))
			.when(openSearchManager).validateAnalyzerSettings(
					argThat(settings -> settings != null
							&& settings.analyzer().get("default") != null
							&& settings.analyzer().get("default").isCustom()
							&& "standard".equals(settings.analyzer().get("default").custom().tokenizer())));
		TextAnalyzer request = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(VALID_SETTINGS);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
			() -> manager.create(adminUser, request));
		assertTrue(ex.getMessage().contains("Invalid analyzer configuration"));
		verify(textAnalyzerDao, never()).create(any(), any());
	}

	@Test
	public void testCreateWithUnresolvedRefThrows() {
		when(synonymSetDao.findNonExistentNames(any()))
			.thenReturn(Collections.singletonList("biomed-medical_terms"));
		TextAnalyzer request = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(SETTINGS_WITH_REF);

		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> manager.create(adminUser, request));
		assertTrue(e.getMessage().contains("biomed-medical_terms"));
		verify(textAnalyzerDao, never()).create(any(), any());
	}

	@Test
	public void testCreateWithMalformedRefQnameThrows() {
		TextAnalyzer request = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(
				"{\"filter\":{\"x\":{\"$ref\":\"not-a-valid-qname-9starts\"}},"
				+ "\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\","
				+ "\"filter\":[\"x\"]}}}");

		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> manager.create(adminUser, request));
		assertTrue(e.getMessage().contains("Invalid qualified name format"),
			"Format error must mention qualified-name: " + e.getMessage());
		verify(textAnalyzerDao, never()).create(any(), any());
		verifyNoMoreInteractions(synonymSetDao);
	}

	@Test
	public void testCreateWithoutDefaultAnalyzerThrows() {
		// SearchConfiguration binds to a TextAnalyzer by its bare qualified name; the index-
		// build code resolves that to the analyzer entry named "default". A settings blob
		// that declares only registries (or names its analyzer something else) would never
		// be reachable, so OpenSearchManager.validateAnalyzerSettings rejects it and the
		// manager propagates without persisting.
		doThrow(new IllegalArgumentException(
				"settings must declare an analyzer named 'default' under analyzer.default."))
			.when(openSearchManager).validateAnalyzerSettings(any());
		TextAnalyzer bad = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(
				"{\"filter\":{\"english_stop\":{\"type\":\"stop\"}}}");

		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> manager.create(adminUser, bad));
		assertTrue(e.getMessage().contains("analyzer.default"),
			"Error must name analyzer.default: " + e.getMessage());
		verify(textAnalyzerDao, never()).create(any(), any());
	}

	@Test
	public void testCreateWithDefaultSearchOnlyThrows() {
		// `default_search` alone (without `default`) is rejected by the downstream check:
		// bindings always resolve to the `default` entry first, so a record without it can
		// never be addressed.
		doThrow(new IllegalArgumentException(
				"settings must declare an analyzer named 'default' under analyzer.default."))
			.when(openSearchManager).validateAnalyzerSettings(any());
		TextAnalyzer bad = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(
				"{\"analyzer\":{\"default_search\":{\"type\":\"custom\",\"tokenizer\":\"standard\"}}}");

		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> manager.create(adminUser, bad));
		assertTrue(e.getMessage().contains("analyzer.default"));
		verify(textAnalyzerDao, never()).create(any(), any());
	}

	@Test
	public void testCreateWithDefaultAndDefaultSearchSucceeds() {
		// Asymmetric index/search analysis (the edge_ngram autocomplete case) is the one
		// supported reason to declare multiple entries inside the inner `analyzer` map.
		String settings = "{\"analyzer\":{"
				+ "\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"},"
				+ "\"default_search\":{\"type\":\"custom\",\"tokenizer\":\"keyword\"}"
				+ "}}";
		when(textAnalyzerDao.create(any(TextAnalyzer.class), eq(3L)))
			.thenReturn(new TextAnalyzer().setId("1"));
		TextAnalyzer request = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(settings);

		// call under test
		manager.create(adminUser, request);

		verify(textAnalyzerDao).create(any(TextAnalyzer.class), eq(3L));
	}

	@Test
	public void testCreateWithExtraNamedAnalyzerThrows() {
		// One TextAnalyzer record exposes one externally-addressable analyzer. Curators
		// who want a separate `headline` analyzer create a separate TextAnalyzer record;
		// the inner analyzer map cannot carry sibling entries beyond `default` /
		// `default_search`.
		String settings = "{\"analyzer\":{"
				+ "\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"},"
				+ "\"headline\":{\"type\":\"custom\",\"tokenizer\":\"standard\"}"
				+ "}}";
		TextAnalyzer bad = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(settings);

		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> manager.create(adminUser, bad));
		assertTrue(e.getMessage().contains("rejected: [headline]"),
			"Error must list the rejected sibling key(s): " + e.getMessage());
		assertTrue(e.getMessage().contains("default")
			&& e.getMessage().contains("default_search"));
		verifyNoMoreInteractions(textAnalyzerDao);
		verifyNoMoreInteractions(synonymSetDao);
	}

	@Test
	public void testUpdateWithNullSettingsThrows() {
		TextAnalyzer input = new TextAnalyzer()
			.setId("1").setOrganizationName("test-org").setName("test_name");

		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.update(sageUser, input));

		verifyNoMoreInteractions(textAnalyzerDao);
		verifyNoMoreInteractions(openSearchManager);
	}

	// --- validateSettings (package-private): direct branch coverage ---

	@Test
	public void testValidateSettingsWithMapInputAcceptsParsedTree() {
		// settings field is now schema-typed as Object; the manager must accept already-parsed
		// Map values exactly as it accepts JSON-string values.
		java.util.Map<String, Object> analyzer = java.util.Map.of(
				"default", java.util.Map.of("type", "custom", "tokenizer", "standard"));
		java.util.Map<String, Object> settings = java.util.Map.of("analyzer", analyzer);

		// call under test
		manager.validateSettings(settings);

		verify(openSearchManager).validateAnalyzerSettings(any());
	}

	@Test
	public void testValidateSettingsWithoutAnalyzerKeyDelegatesToOpenSearch() {
		// The manager only enforces sibling-key restrictions when the analyzer map is present;
		// absence of analyzer is the OpenSearch validator's domain.
		manager.validateSettings("{\"filter\":{}}");

		verify(openSearchManager).validateAnalyzerSettings(any());
	}

	@Test
	public void testValidateSettingsCollectsRefsAndChecksSynonymSetExistence() {
		// $ref entries inside settings.filter must format-validate and resolve to existing
		// SynonymSets. A present target is OK; the manager forwards to OpenSearchManager.
		when(synonymSetDao.findNonExistentNames(Arrays.asList("biomed-medical_terms")))
				.thenReturn(Collections.emptyList());
		when(synonymSetDao.getByQualifiedNames(Collections.singletonList("biomed-medical_terms")))
				.thenReturn(java.util.Map.of("biomed-medical_terms",
						new SynonymSet().setDefinition("{\"type\":\"synonym_graph\",\"synonyms\":[]}")));

		// call under test
		manager.validateSettings(SETTINGS_WITH_REF);

		verify(openSearchManager).validateAnalyzerSettings(any());
	}

	@Test
	public void testValidateSettingsWithMissingSynonymSetRejects() {
		when(synonymSetDao.findNonExistentNames(Arrays.asList("biomed-medical_terms")))
				.thenReturn(Arrays.asList("biomed-medical_terms"));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> manager.validateSettings(SETTINGS_WITH_REF));

		assertTrue(ex.getMessage().contains("biomed-medical_terms"), ex.getMessage());
		// OpenSearch shouldn't be hit when a $ref already failed to resolve.
		verify(openSearchManager, never()).validateAnalyzerSettings(any());
	}

	@Test
	public void testValidateSettingsWithoutAnalyzerMapDelegatesToOpenSearch() {
		// settings without an analyzer map at all (only filters) skips the sibling-key check
		// entirely and goes straight to the typed deserializer + OpenSearch validation.
		manager.validateSettings("{\"filter\":{\"x\":{\"type\":\"stop\",\"stopwords\":\"_english_\"}}}");

		verify(openSearchManager).validateAnalyzerSettings(any());
	}

	@Test
	public void testValidateSettingsWithNonObjectAnalyzerNodeSkipsSiblingCheck() {
		// If `analyzer` is present but not an object (curator typo: a string instead), the
		// sibling-key guard short-circuits and the typed deserializer surfaces the shape
		// error rather than this manager's reject-extra-keys path. The point of this test is
		// to exercise the `analyzerMap != null && !analyzerMap.isObject()` half of L196.
		assertThrows(IllegalArgumentException.class,
				() -> manager.validateSettings("{\"analyzer\":\"not_an_object\"}"));
		verify(openSearchManager, never()).validateAnalyzerSettings(any());
	}

	// --- Admin bypass of org-ACL check (parameterized): update/delete/create ---

	private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> adminMutationActions() {
		return java.util.stream.Stream.of(
				org.junit.jupiter.params.provider.Arguments.of("create"),
				org.junit.jupiter.params.provider.Arguments.of("update"),
				org.junit.jupiter.params.provider.Arguments.of("delete"));
	}

	@org.junit.jupiter.params.ParameterizedTest(name = "{0} as admin skips ACL")
	@org.junit.jupiter.params.provider.MethodSource("adminMutationActions")
	public void testAdminBypassesOrgAclOnMutation(String action) {
		// Admin users bypass the per-org ACL check (the !user.isAdmin() guard at L114/L138 in
		// update/delete and the parallel guard in create). One parameterized test covers every
		// mutation path rather than three near-identical case bodies.
		UserInfo admin = new UserInfo(true, 2L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(2L));

		switch (action) {
			case "create": {
				TextAnalyzer input = new TextAnalyzer()
						.setOrganizationName("test-org").setName("test").setSettings(VALID_SETTINGS);
				when(textAnalyzerDao.create(any(), eq(2L))).thenReturn(input.setId("1"));
				manager.create(admin, input);
				verifyNoMoreInteractions(aclDao);
				verify(textAnalyzerDao).create(any(), eq(2L));
				break;
			}
			case "update": {
				TextAnalyzer existing = new TextAnalyzer().setId("1")
						.setOrganizationName("test-org").setName("test").setSettings(VALID_SETTINGS);
				TextAnalyzer input = new TextAnalyzer().setId("1")
						.setOrganizationName("test-org").setName("test").setSettings(VALID_SETTINGS);
				when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(existing));
				when(textAnalyzerDao.update(any(), eq(2L))).thenReturn(input);
				manager.update(admin, input);
				verifyNoMoreInteractions(aclDao);
				verify(textAnalyzerDao).update(any(), eq(2L));
				break;
			}
			case "delete": {
				TextAnalyzer existing = new TextAnalyzer().setId("1")
						.setOrganizationName("test-org").setName("test").setSettings(VALID_SETTINGS);
				when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(existing));
				manager.delete(admin, 1L);
				verifyNoMoreInteractions(aclDao);
				verify(textAnalyzerDao).delete(1L);
				break;
			}
			default: throw new AssertionError("unhandled action: " + action);
		}
	}
}
