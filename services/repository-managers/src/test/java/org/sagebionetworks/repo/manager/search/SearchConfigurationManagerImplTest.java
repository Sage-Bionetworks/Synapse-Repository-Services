package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.dbo.search.ColumnAnalyzerOverrideDao;
import org.sagebionetworks.repo.model.dbo.search.SearchConfigurationDao;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.search.table.BindSearchConfigToEntityRequest;
import org.sagebionetworks.repo.model.search.table.ListSearchConfigurationsRequest;
import org.sagebionetworks.repo.model.search.table.ListSearchConfigurationsResponse;
import org.sagebionetworks.repo.model.search.table.SearchConfigBinding;
import org.sagebionetworks.repo.model.search.table.SearchConfiguration;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class SearchConfigurationManagerImplTest {

	@Mock
	private SearchConfigurationDao searchConfigurationDao;
	@Mock
	private AccessControlListDAO aclDao;
	@Mock
	private OrganizationDao organizationDao;
	@Mock
	private ColumnAnalyzerOverrideDao columnAnalyzerOverrideDao;
	@Mock
	private TextAnalyzerDao textAnalyzerDao;
	@Mock
	private NodeDAO nodeDAO;
	@Mock
	private EntityAuthorizationManager entityAuthorizationManager;

	private SearchConfigurationManagerImpl manager;

	@BeforeEach
	void setUp() {
		manager = new SearchConfigurationManagerImpl(searchConfigurationDao, aclDao, organizationDao,
				columnAnalyzerOverrideDao, textAnalyzerDao, nodeDAO, entityAuthorizationManager);
	}

	// --- Sage employee / admin authorization ---

	@Test
	public void testCreateWithNonSageUser() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L));

		// call under test
		assertThrows(UnauthorizedException.class, () ->
			manager.create(user, new SearchConfiguration().setOrganizationName("test-org").setName("test")));
		verifyNoMoreInteractions(aclDao);
		verifyNoMoreInteractions(searchConfigurationDao);
	}

	@Test
	public void testUpdateWithNonSageUser() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L));

		// call under test
		assertThrows(UnauthorizedException.class, () ->
			manager.update(user, new SearchConfiguration().setId("1").setOrganizationName("test-org").setName("test")));
		verifyNoMoreInteractions(aclDao);
		verifyNoMoreInteractions(searchConfigurationDao);
	}

	@Test
	public void testCreateWithAnonymousUser() {
		UserInfo anon = new UserInfo(false, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID, Set.of(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId()));

		// call under test
		assertThrows(UnauthorizedException.class, () ->
			manager.create(anon, new SearchConfiguration().setOrganizationName("test-org").setName("test")));
		verifyNoMoreInteractions(aclDao);
		verifyNoMoreInteractions(searchConfigurationDao);
		verifyNoMoreInteractions(organizationDao);
	}

	// --- ACL authorization (user is Sage employee) ---

	@Test
	public void testCreateWithUserWithoutOrgAcl() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.CREATE)))
			.thenReturn(AuthorizationStatus.accessDenied("no"));

		// call under test
		assertThrows(UnauthorizedException.class, () ->
			manager.create(user, new SearchConfiguration().setOrganizationName("test-org").setName("test")));
		verifyNoMoreInteractions(searchConfigurationDao);
	}

	@Test
	public void testCreateWithUserWithOrgAcl() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.CREATE)))
			.thenReturn(AuthorizationStatus.authorized());
		SearchConfiguration input = new SearchConfiguration().setOrganizationName("test-org").setName("test");
		when(searchConfigurationDao.create(eq(1L), any())).thenReturn(input.setId("1"));

		// call under test
		SearchConfiguration result = manager.create(user, input);
		assertNotNull(result);
	}

	@Test
	public void testCreateWithAdminBypassesOrgAcl() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		SearchConfiguration input = new SearchConfiguration().setOrganizationName("test-org").setName("test");
		when(searchConfigurationDao.create(eq(1L), any())).thenReturn(input.setId("1"));

		// call under test
		SearchConfiguration result = manager.create(admin, input);
		assertNotNull(result);
		verifyNoMoreInteractions(aclDao);
	}

	// --- Public read ---

	@Test
	public void testGetWithPublicAccess() {
		when(searchConfigurationDao.get("1")).thenReturn(Optional.of(new SearchConfiguration()));

		// call under test
		manager.get(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), "1");
		verifyNoMoreInteractions(aclDao);
	}

	// --- Not found ---

	@Test
	public void testGetWithNonExistentId() {
		when(searchConfigurationDao.get("999")).thenReturn(Optional.empty());

		// call under test
		NotFoundException ex = assertThrows(NotFoundException.class, () -> manager.get(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), "999"));
		assertEquals("A search configuration with the given id does not exist.", ex.getMessage());
	}

	@Test
	public void testUpdateWithNonExistentId() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		SearchConfiguration request = new SearchConfiguration().setId("999").setOrganizationName("test-org").setName("updated");
		when(searchConfigurationDao.get("999")).thenReturn(Optional.empty());

		// call under test
		NotFoundException ex = assertThrows(NotFoundException.class, () -> manager.update(admin, request));
		assertEquals("A search configuration with the given id does not exist.", ex.getMessage());
	}

	// --- Update ACL ---

	@Test
	public void testUpdateWithUserWithoutOrgAcl() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		SearchConfiguration request = new SearchConfiguration().setId("1").setOrganizationName("test-org").setName("test_name");
		when(searchConfigurationDao.get("1")).thenReturn(Optional.of(new SearchConfiguration().setId("1").setOrganizationName("test-org").setName("test_name")));
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.UPDATE)))
			.thenReturn(AuthorizationStatus.accessDenied("no"));

		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.update(user, request));
		verify(searchConfigurationDao, never()).update(anyLong(), any());
	}

	@Test
	public void testUpdateWithOrgNameMismatch() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		SearchConfiguration request = new SearchConfiguration().setId("1").setOrganizationName("other-org").setName("test_name");
		when(searchConfigurationDao.get("1")).thenReturn(Optional.of(new SearchConfiguration().setId("1").setOrganizationName("test-org")));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> manager.update(admin, request));
		assertEquals(SearchResourceConstants.ORG_NAME_IMMUTABLE_MSG, ex.getMessage());
	}

	@Test
	public void testUpdateWithNameChangeThrows() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		SearchConfiguration request = new SearchConfiguration()
			.setId("1").setOrganizationName("test-org").setName("new_name");
		when(searchConfigurationDao.get("1")).thenReturn(Optional.of(
			new SearchConfiguration().setId("1").setOrganizationName("test-org").setName("original_name")));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> manager.update(admin, request));

		assertEquals(SearchResourceConstants.NAME_IMMUTABLE_MSG, ex.getMessage());
		verify(searchConfigurationDao, never()).update(any(), any());
	}

	@Test
	public void testUpdateClearsColumnAnalyzerOverrides() {
		// Verify that nulling columnAnalyzerOverrides on the request reaches the DAO
		// with the same null rather than the manager defaulting back to the existing values.
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		SearchConfiguration request = new SearchConfiguration().setId("999")
			.setOrganizationName("test-org").setName("my_config").setEtag("etag-1");
		request.setColumnAnalyzerOverrides(null);
		SearchConfiguration existing = new SearchConfiguration().setId("999")
			.setOrganizationName("test-org").setName("my_config").setEtag("etag-1")
			.setColumnAnalyzerOverrides(Arrays.asList("biomed-overrides"));
		when(searchConfigurationDao.get("999")).thenReturn(Optional.of(existing));
		SearchConfiguration cleared = new SearchConfiguration().setId("999")
			.setOrganizationName("test-org").setName("my_config").setEtag("etag-2");
		when(searchConfigurationDao.update(eq(1L), eq(request))).thenReturn(cleared);

		// call under test
		SearchConfiguration result = manager.update(admin, request);

		assertEquals(cleared, result);
		verify(searchConfigurationDao).update(eq(1L), eq(request));
	}

	// --- List / pagination ---

	@Test
	public void testListWithNoOrgName() {
		SearchConfiguration item = new SearchConfiguration().setId("1");
		when(searchConfigurationDao.listAll(anyLong(), anyLong()))
			.thenReturn(Arrays.asList(item));

		ListSearchConfigurationsRequest request = new ListSearchConfigurationsRequest();

		// call under test
		ListSearchConfigurationsResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);
		assertEquals(1, response.getResults().size());
		verify(searchConfigurationDao).listAll(anyLong(), anyLong());
		verify(searchConfigurationDao, never()).list(anyString(), anyLong(), anyLong());
	}

	@Test
	public void testListWithOrgName() {
		SearchConfiguration item = new SearchConfiguration().setId("1");
		when(searchConfigurationDao.list(eq("test-org"), anyLong(), anyLong()))
			.thenReturn(Arrays.asList(item));

		ListSearchConfigurationsRequest request = new ListSearchConfigurationsRequest();
		request.setOrganizationName("test-org");

		// call under test
		ListSearchConfigurationsResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);
		assertEquals(1, response.getResults().size());
		assertEquals("1", response.getResults().get(0).getId());
	}

	@Test
	public void testListWithMoreResults() {
		List<SearchConfiguration> page = new ArrayList<>();
		for (int i = 0; i < 51; i++) {
			page.add(new SearchConfiguration().setId(String.valueOf(i)));
		}
		when(searchConfigurationDao.listAll(eq(51L), eq(0L))).thenReturn(page);

		ListSearchConfigurationsRequest request = new ListSearchConfigurationsRequest();

		// call under test
		ListSearchConfigurationsResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);

		assertNotNull(response.getNextPageToken(), "Expected a next page token when DAO returns more than limit results");
		assertEquals(50, response.getResults().size());
	}

	@Test
	public void testListWithNoMoreResults() {
		List<SearchConfiguration> page = Arrays.asList(new SearchConfiguration().setId("1"));
		when(searchConfigurationDao.listAll(eq(51L), eq(0L))).thenReturn(page);

		ListSearchConfigurationsRequest request = new ListSearchConfigurationsRequest();

		// call under test
		ListSearchConfigurationsResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);

		assertNull(response.getNextPageToken(), "Expected null next page token when all results fit in one page");
		assertEquals(1, response.getResults().size());
	}

	// --- Bind / Unbind ---

	@Test
	public void testBindSearchConfigToEntityWithValidRequest() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));

		BindSearchConfigToEntityRequest request = new BindSearchConfigToEntityRequest();
		request.setEntityId("syn123");
		request.setSearchConfigurationId("456");

		when(entityAuthorizationManager.hasAccess(user, "syn123", ACCESS_TYPE.UPDATE))
			.thenReturn(AuthorizationStatus.authorized());
		when(nodeDAO.getNodeTypeById("syn123")).thenReturn(EntityType.project);
		when(searchConfigurationDao.get("456")).thenReturn(Optional.of(new SearchConfiguration()));
		SearchConfigBinding expectedBinding = new SearchConfigBinding();
		expectedBinding.setBindId("1");
		when(searchConfigurationDao.getSearchConfigBindingForObject(123L, "entity"))
			.thenReturn(Optional.of(expectedBinding));

		// call under test
		SearchConfigBinding result = manager.bindSearchConfigToEntity(user, request);

		assertEquals(expectedBinding, result);
		verify(searchConfigurationDao).bindSearchConfigToObject(456L, 123L, "entity", 1L);
	}

	@Test
	public void testBindSearchConfigToEntityWithAnonymousUser() {
		UserInfo anon = new UserInfo(false, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID);
		anon.setRealmAnonymousUserId(anon.getId());

		BindSearchConfigToEntityRequest request = new BindSearchConfigToEntityRequest();
		request.setEntityId("syn123");
		request.setSearchConfigurationId("456");

		// call under test
		String message = assertThrows(UnauthorizedException.class, () -> manager.bindSearchConfigToEntity(anon, request)).getMessage();
		assertEquals("Must login to perform this action", message);
		verifyNoMoreInteractions(aclDao);
		verifyNoMoreInteractions(searchConfigurationDao);
	}

	@Test
	public void testBindSearchConfigToEntityWithMissingEntityId() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L));

		BindSearchConfigToEntityRequest request = new BindSearchConfigToEntityRequest();
		request.setSearchConfigurationId("456");

		// call under test
		String message = assertThrows(IllegalArgumentException.class, () -> manager.bindSearchConfigToEntity(user, request)).getMessage();
		assertEquals("entityId is required and must not be the empty string.", message);
		verifyNoMoreInteractions(searchConfigurationDao);
	}

	@Test
	public void testBindSearchConfigToEntityWithMissingConfigId() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L));

		BindSearchConfigToEntityRequest request = new BindSearchConfigToEntityRequest();
		request.setEntityId("syn123");

		// call under test
		String message = assertThrows(IllegalArgumentException.class, () -> manager.bindSearchConfigToEntity(user, request)).getMessage();
		assertEquals("searchConfigurationId is required and must not be the empty string.", message);
		verifyNoMoreInteractions(searchConfigurationDao);
	}

	@Test
	public void testBindSearchConfigToEntityWithNonExistentConfig() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));

		BindSearchConfigToEntityRequest request = new BindSearchConfigToEntityRequest();
		request.setEntityId("syn123");
		request.setSearchConfigurationId("999");

		when(entityAuthorizationManager.hasAccess(user, "syn123", ACCESS_TYPE.UPDATE))
			.thenReturn(AuthorizationStatus.authorized());
		when(nodeDAO.getNodeTypeById("syn123")).thenReturn(EntityType.folder);
		when(searchConfigurationDao.get("999")).thenReturn(Optional.empty());

		// call under test
		String message = assertThrows(NotFoundException.class, () -> manager.bindSearchConfigToEntity(user, request)).getMessage();
		assertEquals("A search configuration with the given id does not exist.", message);
		verify(searchConfigurationDao, never()).bindSearchConfigToObject(anyLong(), anyLong(), anyString(), anyLong());
	}

	@Test
	public void testBindSearchConfigToEntityWithNonProjectOrFolderEntity() {
		// A search configuration can only be bound to a Project or Folder.
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));

		BindSearchConfigToEntityRequest request = new BindSearchConfigToEntityRequest();
		request.setEntityId("syn123");
		request.setSearchConfigurationId("456");

		when(entityAuthorizationManager.hasAccess(user, "syn123", ACCESS_TYPE.UPDATE))
			.thenReturn(AuthorizationStatus.authorized());
		when(nodeDAO.getNodeTypeById("syn123")).thenReturn(EntityType.searchindex);

		// call under test
		String message = assertThrows(IllegalArgumentException.class, () -> manager.bindSearchConfigToEntity(user, request)).getMessage();
		assertEquals("A search configuration can only be bound to a Project or Folder.", message);
		verify(searchConfigurationDao, never()).bindSearchConfigToObject(anyLong(), anyLong(), anyString(), anyLong());
	}

	@Test
	public void testClearSearchConfigBindingWithValidEntity() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));

		when(entityAuthorizationManager.hasAccess(user, "syn123", ACCESS_TYPE.UPDATE))
			.thenReturn(AuthorizationStatus.authorized());

		// call under test
		manager.clearSearchConfigBinding(user, "syn123");

		verify(searchConfigurationDao).clearSearchConfigBinding(123L, "entity");
	}

	@Test
	public void testClearSearchConfigBindingWithAnonymousUser() {
		UserInfo anon = new UserInfo(false, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID);
		anon.setRealmAnonymousUserId(anon.getId());

		// call under test
		String message = assertThrows(UnauthorizedException.class, () -> manager.clearSearchConfigBinding(anon, "syn123")).getMessage();
		assertEquals("Must login to perform this action", message);
		verifyNoMoreInteractions(aclDao);
		verifyNoMoreInteractions(searchConfigurationDao);
	}

	// --- Hierarchy walk for getSearchConfigBinding ---

	@Test
	public void testGetSearchConfigBindingWalksHierarchyAndThrowsWhenAbsent() {
		when(nodeDAO.getEntityIdOfFirstBoundSearchConfig(1L)).thenReturn(Optional.empty());

		// call under test
		NotFoundException e = assertThrows(NotFoundException.class,
				() -> manager.getSearchConfigBinding(new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID), "syn1"));

		assertTrue(e.getMessage().contains("any of its ancestors"));
	}

	@Test
	public void testGetSearchConfigBindingFollowsHierarchyToAncestor() {
		// nodeDAO returns ancestor 999 as the first bound node; fetch the binding under that.
		when(nodeDAO.getEntityIdOfFirstBoundSearchConfig(1L)).thenReturn(Optional.of(999L));
		SearchConfigBinding binding = new SearchConfigBinding().setBindId("7").setObjectId("999");
		when(searchConfigurationDao.getSearchConfigBindingForObject(999L, "entity"))
				.thenReturn(Optional.of(binding));

		// call under test
		assertEquals(binding, manager.getSearchConfigBinding(new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID), "syn1"));
	}

	// --- Name pattern validation ---

	@Test
	public void testCreateWithInvalidNamePattern() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
			manager.create(admin, new SearchConfiguration().setOrganizationName("test-org").setName("9invalid")));
		assertEquals(SearchResourceConstants.RESOURCE_NAME_PATTERN_MSG, ex.getMessage());
		verifyNoMoreInteractions(searchConfigurationDao);
	}

	// --- Reference name validation ---

	@Test
	public void testCreateWithInvalidQualifiedNameFormat() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);

		// call under test — defaultAnalyzer is a $ref to a malformed qname
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
			manager.create(admin, new SearchConfiguration()
				.setOrganizationName("test-org").setName("MyConfig")
				.setDefaultAnalyzer(Map.of("$ref", "noHyphenHere"))));
		assertTrue(ex.getMessage().contains("Invalid qualified name format"));
		verifyNoMoreInteractions(searchConfigurationDao);
	}

	@Test
	public void testCreateWithMissingDefaultAnalyzer() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		when(textAnalyzerDao.findNonExistentNames(Arrays.asList("org.sagebionetworks-MISSING")))
			.thenReturn(Arrays.asList("org.sagebionetworks-MISSING"));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
			manager.create(admin, new SearchConfiguration()
				.setOrganizationName("test-org").setName("MyConfig")
				.setDefaultAnalyzer(Map.of("$ref", "org.sagebionetworks-MISSING"))));
		assertTrue(ex.getMessage().contains("text analyzer name(s) do not exist"));
		verify(searchConfigurationDao, never()).create(anyLong(), any());
	}

	@Test
	public void testCreateWithMissingColumnAnalyzerOverride() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		when(columnAnalyzerOverrideDao.findNonExistentNames(Arrays.asList("org.sagebionetworks-MISSING_OVERRIDE")))
			.thenReturn(Arrays.asList("org.sagebionetworks-MISSING_OVERRIDE"));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
			manager.create(admin, new SearchConfiguration()
				.setOrganizationName("test-org").setName("MyConfig")
				.setColumnAnalyzerOverrides(Arrays.asList(Map.of("$ref", "org.sagebionetworks-MISSING_OVERRIDE")))));
		assertTrue(ex.getMessage().contains("column analyzer override name(s) do not exist"));
		verify(searchConfigurationDao, never()).create(anyLong(), any());
	}

	// --- Inline column-analyzer-override path through validateReferencedNames ---

	@Test
	public void testCreateWithInlineOverrideValidatesNestedAnalyzerRef() {
		// An inline ColumnAnalyzerOverride lives only inside the SearchConfiguration's JSON;
		// each entry's analyzer is itself inline-or-$ref. Refs nested inside an inline override
		// must still be format-validated and existence-checked against TextAnalyzer.
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		Map<String, Object> inlineOverride = Map.of("overrides", Arrays.asList(
				Map.of("columnName", "diagnosis",
						"analyzer", Map.of("$ref", "org.sagebionetworks-DEEP_REF"))));
		when(textAnalyzerDao.findNonExistentNames(Arrays.asList("org.sagebionetworks-DEEP_REF")))
				.thenReturn(Collections.emptyList());
		SearchConfiguration created = new SearchConfiguration()
				.setOrganizationName("test-org").setName("MyConfig")
				.setColumnAnalyzerOverrides(Arrays.asList(inlineOverride));
		when(searchConfigurationDao.create(eq(1L), any())).thenReturn(created.setId("1"));

		// call under test
		manager.create(admin, created);

		verify(textAnalyzerDao).findNonExistentNames(Arrays.asList("org.sagebionetworks-DEEP_REF"));
	}

	@Test
	public void testCreateWithInlineOverrideMissingNestedAnalyzerThrows() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		Map<String, Object> inlineOverride = Map.of("overrides", Arrays.asList(
				Map.of("columnName", "diagnosis",
						"analyzer", Map.of("$ref", "biomed-MISSING"))));
		when(textAnalyzerDao.findNonExistentNames(Arrays.asList("biomed-MISSING")))
				.thenReturn(Arrays.asList("biomed-MISSING"));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
				manager.create(admin, new SearchConfiguration()
						.setOrganizationName("test-org").setName("MyConfig")
						.setColumnAnalyzerOverrides(Arrays.asList(inlineOverride))));
		assertTrue(ex.getMessage().contains("biomed-MISSING"), ex.getMessage());
		verify(searchConfigurationDao, never()).create(anyLong(), any());
	}

	@Test
	public void testCreateWithInlineOverrideHavingNullOverridesListIsTolerated() {
		// A degenerate inline override with no overrides[] list at all must not NPE in
		// validateReferencedNames; the recursive walk has nothing to validate.
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		Map<String, Object> degenerate = Map.of("organizationName", "biomed", "name", "noop");
		SearchConfiguration created = new SearchConfiguration()
				.setOrganizationName("test-org").setName("MyConfig")
				.setColumnAnalyzerOverrides(Arrays.asList(degenerate));
		when(searchConfigurationDao.create(eq(1L), any())).thenReturn(created.setId("1"));

		// call under test
		manager.create(admin, created);

		verifyNoMoreInteractions(textAnalyzerDao);
		verify(searchConfigurationDao).create(eq(1L), any());
	}

	@Test
	public void testCreateWithInlineDefaultAnalyzerSkipsRefExistenceCheck() {
		// An inline analyzer literal at defaultAnalyzer must pass the shape-conversion check
		// but is never registered as a saved row, so no findNonExistentNames call is made.
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		Map<String, Object> inlineAnalyzer = Map.of(
				"analyzer", Map.of("default",
						Map.of("type", "custom", "tokenizer", "standard")));
		SearchConfiguration created = new SearchConfiguration()
				.setOrganizationName("test-org").setName("MyConfig")
				.setDefaultAnalyzer(inlineAnalyzer);
		when(searchConfigurationDao.create(eq(1L), any())).thenReturn(created.setId("1"));

		// call under test
		manager.create(admin, created);

		verifyNoMoreInteractions(textAnalyzerDao);
		verify(searchConfigurationDao).create(eq(1L), any());
	}

	@Test
	public void testCreateWithMalformedInlineDefaultAnalyzerThrows() {
		// The inline analyzer literal must round-trip through the OpenSearch typed
		// deserializer; an unknown filter type is rejected at create time.
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		Map<String, Object> malformedAnalyzer = Map.of(
				"filter", Map.of("bogus", Map.of("type", "this_filter_does_not_exist")),
				"analyzer", Map.of("default",
						Map.of("type", "custom", "tokenizer", "standard", "filter", List.of("bogus"))));
		SearchConfiguration toCreate = new SearchConfiguration()
				.setOrganizationName("test-org").setName("MyConfig")
				.setDefaultAnalyzer(malformedAnalyzer);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> manager.create(admin, toCreate));
		assertTrue(ex.getMessage().contains("analyzer settings"),
				"expected typed-deserializer rejection, got: " + ex.getMessage());
		verifyNoMoreInteractions(searchConfigurationDao);
		verifyNoMoreInteractions(textAnalyzerDao);
	}

	// --- bindSearchConfigToEntity authorization & user.isAdmin shortcuts ---

	@Test
	public void testBindSearchConfigToEntityWithNonSageUserThrows() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L));
		BindSearchConfigToEntityRequest req = new BindSearchConfigToEntityRequest()
				.setEntityId("syn123").setSearchConfigurationId("42");

		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.bindSearchConfigToEntity(user, req));
		verifyNoMoreInteractions(searchConfigurationDao);
		verifyNoMoreInteractions(aclDao);
	}

	@Test
	public void testBindSearchConfigToEntityWithSageNonAdminChecksEntityAuthorization() {
		// Sage employee but not admin: UPDATE check must resolve the benefactor via EntityAuthorizationManager.
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		BindSearchConfigToEntityRequest req = new BindSearchConfigToEntityRequest()
				.setEntityId("syn123").setSearchConfigurationId("42");
		when(entityAuthorizationManager.hasAccess(user, "syn123", ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(nodeDAO.getNodeTypeById("syn123")).thenReturn(EntityType.project);
		when(searchConfigurationDao.get("42")).thenReturn(Optional.of(new SearchConfiguration().setId("42")));
		when(searchConfigurationDao.getSearchConfigBindingForObject(123L, "entity"))
				.thenReturn(Optional.of(new SearchConfigBinding()));

		// call under test
		manager.bindSearchConfigToEntity(user, req);

		verify(entityAuthorizationManager).hasAccess(user, "syn123", ACCESS_TYPE.UPDATE);
		verify(searchConfigurationDao).bindSearchConfigToObject(42L, 123L, "entity", 1L);
	}

	@Test
	public void testBindSearchConfigToEntityWithBenefactorInheritedAccess() {
		// PLFM-9754: a caller authorized only via the benefactor (no local ACL on the entity) must be able
		// to bind. The UPDATE check must go through EntityAuthorizationManager (which resolves the benefactor)
		// and never read the entity's own ACL via aclDao.
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		BindSearchConfigToEntityRequest req = new BindSearchConfigToEntityRequest()
				.setEntityId("syn123").setSearchConfigurationId("42");
		when(entityAuthorizationManager.hasAccess(user, "syn123", ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(nodeDAO.getNodeTypeById("syn123")).thenReturn(EntityType.folder);
		when(searchConfigurationDao.get("42")).thenReturn(Optional.of(new SearchConfiguration().setId("42")));
		when(searchConfigurationDao.getSearchConfigBindingForObject(123L, "entity"))
				.thenReturn(Optional.of(new SearchConfigBinding()));

		// call under test
		manager.bindSearchConfigToEntity(user, req);

		verify(searchConfigurationDao).bindSearchConfigToObject(42L, 123L, "entity", 1L);
		// The entity's own ACL must not be consulted directly.
		verify(aclDao, never()).canAccess(any(UserInfo.class), anyString(), eq(ObjectType.ENTITY), any(ACCESS_TYPE.class));
	}

	@Test
	public void testBindSearchConfigToEntityWithUnauthorized() {
		// Sage employee but lacks UPDATE on the entity (or its benefactor) → denied, no write.
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		BindSearchConfigToEntityRequest req = new BindSearchConfigToEntityRequest()
				.setEntityId("syn123").setSearchConfigurationId("42");
		when(entityAuthorizationManager.hasAccess(user, "syn123", ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.accessDenied("no"));

		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.bindSearchConfigToEntity(user, req));

		verify(searchConfigurationDao, never()).bindSearchConfigToObject(anyLong(), anyLong(), anyString(), anyLong());
	}

	@Test
	public void testClearSearchConfigBindingWithNonSageUserThrows() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L));

		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.clearSearchConfigBinding(user, "syn123"));
		verifyNoMoreInteractions(searchConfigurationDao);
		verifyNoMoreInteractions(aclDao);
	}

	@Test
	public void testClearSearchConfigBindingWithSageNonAdminChecksEntityAuthorization() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		when(entityAuthorizationManager.hasAccess(user, "syn123", ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());

		// call under test
		manager.clearSearchConfigBinding(user, "syn123");

		verify(entityAuthorizationManager).hasAccess(user, "syn123", ACCESS_TYPE.UPDATE);
		verify(searchConfigurationDao).clearSearchConfigBinding(123L, "entity");
	}

	@Test
	public void testClearSearchConfigBindingWithUnauthorized() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		when(entityAuthorizationManager.hasAccess(user, "syn123", ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.accessDenied("no"));

		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.clearSearchConfigBinding(user, "syn123"));

		verify(searchConfigurationDao, never()).clearSearchConfigBinding(anyLong(), anyString());
	}

	@Test
	public void testBindSearchConfigToEntityAsAdmin() {
		// admin → EntityAuthorizationManager grants UPDATE; ORGANIZATION aclDao is untouched here.
		UserInfo admin = new UserInfo(true, 2L, AuthorizationConstants.DEFAULT_REALM_ID);
		BindSearchConfigToEntityRequest req = new BindSearchConfigToEntityRequest()
				.setEntityId("syn123").setSearchConfigurationId("42");
		when(entityAuthorizationManager.hasAccess(admin, "syn123", ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(nodeDAO.getNodeTypeById("syn123")).thenReturn(EntityType.project);
		when(searchConfigurationDao.get("42")).thenReturn(Optional.of(new SearchConfiguration().setId("42")));
		when(searchConfigurationDao.getSearchConfigBindingForObject(123L, "entity"))
				.thenReturn(Optional.of(new SearchConfigBinding()));

		// call under test
		manager.bindSearchConfigToEntity(admin, req);

		verifyNoMoreInteractions(aclDao);
		verify(searchConfigurationDao).bindSearchConfigToObject(42L, 123L, "entity", 2L);
	}

	@Test
	public void testClearSearchConfigBindingAsAdmin() {
		// admin → EntityAuthorizationManager grants UPDATE; ORGANIZATION aclDao is untouched here.
		UserInfo admin = new UserInfo(true, 2L, AuthorizationConstants.DEFAULT_REALM_ID);
		when(entityAuthorizationManager.hasAccess(admin, "syn123", ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());

		// call under test
		manager.clearSearchConfigBinding(admin, "syn123");

		verifyNoMoreInteractions(aclDao);
		verify(searchConfigurationDao).clearSearchConfigBinding(123L, "entity");
	}
}
