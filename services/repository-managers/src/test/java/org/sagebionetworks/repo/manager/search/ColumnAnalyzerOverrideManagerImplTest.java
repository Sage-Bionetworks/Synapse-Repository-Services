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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.sagebionetworks.repo.model.dbo.search.ColumnAnalyzerOverrideDao;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry;
import org.sagebionetworks.repo.model.search.table.ListColumnAnalyzerOverridesRequest;
import org.sagebionetworks.repo.model.search.table.ListColumnAnalyzerOverridesResponse;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class ColumnAnalyzerOverrideManagerImplTest {

	@Mock
	private ColumnAnalyzerOverrideDao columnAnalyzerOverrideDao;
	@Mock
	private TextAnalyzerDao textAnalyzerDao;
	@Mock
	private AccessControlListDAO aclDao;
	@Mock
	private OrganizationDao organizationDao;

	private ColumnAnalyzerOverrideManagerImpl manager;

	@BeforeEach
	void setUp() {
		manager = new ColumnAnalyzerOverrideManagerImpl(columnAnalyzerOverrideDao, textAnalyzerDao, aclDao, organizationDao);
	}

	// --- Sage employee / admin authorization ---

	@Test
	public void testNonSageUserCannotCreate() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L));

		assertThrows(UnauthorizedException.class, () ->
			manager.create(user, new ColumnAnalyzerOverride().setOrganizationName("test-org").setName("test")));
		verifyNoMoreInteractions(aclDao);
		verifyNoMoreInteractions(columnAnalyzerOverrideDao);
	}

	@Test
	public void testNonSageUserCannotUpdate() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L));

		assertThrows(UnauthorizedException.class, () ->
			manager.update(user, new ColumnAnalyzerOverride().setId("1").setOrganizationName("test-org").setName("test")));
		verifyNoMoreInteractions(aclDao);
		verifyNoMoreInteractions(columnAnalyzerOverrideDao);
	}

	@Test
	public void testNonSageUserCannotDelete() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L));

		assertThrows(UnauthorizedException.class, () ->
			manager.delete(user, "1"));
		verifyNoMoreInteractions(aclDao);
		verifyNoMoreInteractions(columnAnalyzerOverrideDao);
	}

	@Test
	public void testAnonymousUserCannotCreate() {
		UserInfo anon = new UserInfo(false, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID, Set.of(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId()));

		assertThrows(UnauthorizedException.class, () ->
			manager.create(anon, new ColumnAnalyzerOverride().setOrganizationName("test-org").setName("test")));
	}

	@Test
	public void testAnonymousUserCannotUpdate() {
		UserInfo anon = new UserInfo(false, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID, Set.of(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId()));

		assertThrows(UnauthorizedException.class, () ->
			manager.update(anon, new ColumnAnalyzerOverride().setId("1").setOrganizationName("test-org").setName("test")));
	}

	@Test
	public void testAnonymousUserCannotDelete() {
		UserInfo anon = new UserInfo(false, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID, Set.of(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId()));

		assertThrows(UnauthorizedException.class, () ->
			manager.delete(anon, "1"));
	}

	// --- ACL authorization (user is Sage employee) ---

	@Test
	public void testAuthenticatedUserWithoutOrgAclCannotCreate() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.CREATE)))
			.thenReturn(AuthorizationStatus.accessDenied("no"));

		assertThrows(UnauthorizedException.class, () ->
			manager.create(user, new ColumnAnalyzerOverride().setOrganizationName("test-org").setName("test")));
	}

	@Test
	public void testAuthenticatedUserWithOrgAclCanCreate() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.CREATE)))
			.thenReturn(AuthorizationStatus.authorized());
		ColumnAnalyzerOverride input = new ColumnAnalyzerOverride().setOrganizationName("test-org").setName("test");
		when(columnAnalyzerOverrideDao.create(eq(1L), any())).thenReturn(input.setId("1"));

		ColumnAnalyzerOverride result = manager.create(user, input);
		assertNotNull(result);
	}

	@Test
	public void testAdminBypassesOrgAclOnCreate() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		ColumnAnalyzerOverride input = new ColumnAnalyzerOverride().setOrganizationName("test-org").setName("test");
		when(columnAnalyzerOverrideDao.create(eq(1L), any())).thenReturn(input.setId("1"));

		ColumnAnalyzerOverride result = manager.create(admin, input);
		assertNotNull(result);
		verifyNoMoreInteractions(aclDao);
	}

	@Test
	public void testAdminBypassesOrgAclOnUpdate() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		ColumnAnalyzerOverride request = new ColumnAnalyzerOverride().setId("1").setOrganizationName("test-org").setName("updated");
		when(columnAnalyzerOverrideDao.get("1")).thenReturn(Optional.of(request));
		when(columnAnalyzerOverrideDao.update(eq(1L), any())).thenReturn(request);

		ColumnAnalyzerOverride result = manager.update(admin, request);
		assertNotNull(result);
		verifyNoMoreInteractions(aclDao);
	}

	@Test
	public void testAdminBypassesOrgAclOnDelete() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		ColumnAnalyzerOverride existing = new ColumnAnalyzerOverride().setId("1").setOrganizationName("test-org");
		when(columnAnalyzerOverrideDao.get("1")).thenReturn(Optional.of(existing));

		manager.delete(admin, "1");
		verify(columnAnalyzerOverrideDao).delete("1");
		verifyNoMoreInteractions(aclDao);
	}

	// --- Public read ---

	@Test
	public void testGetIsPublicNoAuthCheck() {
		when(columnAnalyzerOverrideDao.get("1")).thenReturn(Optional.of(new ColumnAnalyzerOverride()));
		manager.get(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), "1");
		verifyNoMoreInteractions(aclDao);
	}

	// --- Deletion ---

	@Test
	public void testDeleteSucceeds() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		ColumnAnalyzerOverride existing = new ColumnAnalyzerOverride().setId("1").setOrganizationName("test-org");
		when(columnAnalyzerOverrideDao.get("1")).thenReturn(Optional.of(existing));

		manager.delete(admin, "1");
		verify(columnAnalyzerOverrideDao).delete("1");
	}

	@Test
	public void testDeleteFailsWhenStillReferenced() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		ColumnAnalyzerOverride existing = new ColumnAnalyzerOverride().setId("1").setOrganizationName("test-org");
		when(columnAnalyzerOverrideDao.get("1")).thenReturn(Optional.of(existing));
		doThrow(new IllegalArgumentException("Cannot delete column analyzer override '1' because it is still referenced."))
			.when(columnAnalyzerOverrideDao).delete("1");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
			manager.delete(admin, "1"));
		assertTrue(ex.getMessage().contains("still referenced"));
	}

	// --- Not found ---

	@Test
	public void testGetNotFoundThrows() {
		when(columnAnalyzerOverrideDao.get("999")).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> manager.get(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), "999"));
	}

	@Test
	public void testDeleteNotFoundThrows() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		when(columnAnalyzerOverrideDao.get("1")).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> manager.delete(admin, "1"));
	}

	@Test
	public void testUpdateNotFoundThrows() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		ColumnAnalyzerOverride request = new ColumnAnalyzerOverride().setId("999").setOrganizationName("test-org").setName("updated");
		when(columnAnalyzerOverrideDao.get("999")).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> manager.update(admin, request));
	}

	// --- Update ACL ---

	@Test
	public void testUpdateRequiresOrgAcl() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		ColumnAnalyzerOverride request = new ColumnAnalyzerOverride().setId("1").setOrganizationName("test-org").setName("test_name");
		when(columnAnalyzerOverrideDao.get("1")).thenReturn(Optional.of(new ColumnAnalyzerOverride().setId("1").setOrganizationName("test-org").setName("test_name")));
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.UPDATE)))
			.thenReturn(AuthorizationStatus.accessDenied("no"));

		assertThrows(UnauthorizedException.class, () -> manager.update(user, request));
	}

	@Test
	public void testUpdateRejectsOrgNameMismatch() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		ColumnAnalyzerOverride request = new ColumnAnalyzerOverride().setId("1").setOrganizationName("other-org").setName("test_name");
		when(columnAnalyzerOverrideDao.get("1")).thenReturn(Optional.of(new ColumnAnalyzerOverride().setId("1").setOrganizationName("test-org").setName("test_name")));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> manager.update(admin, request));
		assertTrue(ex.getMessage().contains("organizationName cannot be changed"));
	}

	@Test
	public void testUpdateWithNameChangeThrows() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		ColumnAnalyzerOverride request = new ColumnAnalyzerOverride()
			.setId("1").setOrganizationName("test-org").setName("new_name");
		when(columnAnalyzerOverrideDao.get("1")).thenReturn(Optional.of(
			new ColumnAnalyzerOverride().setId("1").setOrganizationName("test-org").setName("original_name")));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> manager.update(admin, request));

		assertTrue(ex.getMessage().contains("name cannot be changed"));
		verify(columnAnalyzerOverrideDao, never()).update(any(), any());
	}

	// --- Delete ACL from stored entity ---

	@Test
	public void testDeleteChecksOrgAclFromStoredEntity() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		ColumnAnalyzerOverride existing = new ColumnAnalyzerOverride().setId("1").setOrganizationName("other-org");
		when(columnAnalyzerOverrideDao.get("1")).thenReturn(Optional.of(existing));
		when(organizationDao.getOrganizationByName("other-org")).thenReturn(new Organization().setId("99"));
		when(aclDao.canAccess(any(UserInfo.class), eq("99"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.DELETE)))
			.thenReturn(AuthorizationStatus.accessDenied("no"));

		assertThrows(UnauthorizedException.class, () -> manager.delete(user, "1"));
		verify(aclDao).canAccess(any(UserInfo.class), eq("99"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.DELETE));
	}

	// --- List / pagination ---

	@Test
	public void testListAllWhenNoOrgName() {
		ColumnAnalyzerOverride item = new ColumnAnalyzerOverride().setId("1");
		when(columnAnalyzerOverrideDao.listAll(anyLong(), anyLong()))
			.thenReturn(Arrays.asList(item));

		ListColumnAnalyzerOverridesRequest request = new ListColumnAnalyzerOverridesRequest();

		ListColumnAnalyzerOverridesResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);
		assertEquals(1, response.getResults().size());
		verify(columnAnalyzerOverrideDao).listAll(anyLong(), anyLong());
		verify(columnAnalyzerOverrideDao, never()).list(anyString(), anyLong(), anyLong());
	}

	@Test
	public void testListDelegatesToDaoWithPagination() {
		ColumnAnalyzerOverride item = new ColumnAnalyzerOverride().setId("1");
		when(columnAnalyzerOverrideDao.list(eq("test-org"), anyLong(), anyLong()))
			.thenReturn(Arrays.asList(item));

		ListColumnAnalyzerOverridesRequest request = new ListColumnAnalyzerOverridesRequest();
		request.setOrganizationName("test-org");

		ListColumnAnalyzerOverridesResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);
		assertEquals(1, response.getResults().size());
		assertEquals("1", response.getResults().get(0).getId());
	}

	@Test
	public void testListReturnsNextPageTokenWhenMoreResults() {
		List<ColumnAnalyzerOverride> page = new ArrayList<>();
		for (int i = 0; i < 51; i++) {
			page.add(new ColumnAnalyzerOverride().setId(String.valueOf(i)));
		}
		when(columnAnalyzerOverrideDao.listAll(eq(51L), eq(0L))).thenReturn(page);

		ListColumnAnalyzerOverridesRequest request = new ListColumnAnalyzerOverridesRequest();
		ListColumnAnalyzerOverridesResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);

		assertNotNull(response.getNextPageToken(), "Expected a next page token when DAO returns more than limit results");
		assertEquals(50, response.getResults().size());
	}

	@Test
	public void testListReturnsNullNextPageTokenWhenNoMoreResults() {
		List<ColumnAnalyzerOverride> page = Arrays.asList(new ColumnAnalyzerOverride().setId("1"));
		when(columnAnalyzerOverrideDao.listAll(eq(51L), eq(0L))).thenReturn(page);

		ListColumnAnalyzerOverridesRequest request = new ListColumnAnalyzerOverridesRequest();
		ListColumnAnalyzerOverridesResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);

		assertNull(response.getNextPageToken(), "Expected null next page token when all results fit in one page");
		assertEquals(1, response.getResults().size());
	}

	// --- Name pattern validation ---

	@Test
	public void testCreateWithInvalidNamePattern() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
			manager.create(admin, new ColumnAnalyzerOverride().setOrganizationName("test-org").setName("invalid-name")));
		assertTrue(ex.getMessage().contains("Resource name must start with a letter"));
		verifyNoMoreInteractions(columnAnalyzerOverrideDao);
	}

	// --- Entry analyzer name validation ---

	@Test
	public void testCreateWithInvalidQualifiedNameInEntry() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry()
			.setColumnName("myCol")
			.setAnalyzer(java.util.Map.of("$ref", "noHyphenHere"));
		ColumnAnalyzerOverride request = new ColumnAnalyzerOverride()
			.setOrganizationName("test-org").setName("MyOverride")
			.setOverrides(Arrays.asList(entry));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> manager.create(admin, request));
		assertTrue(ex.getMessage().contains("'{organizationName}-{resourceName}'"));
		verify(columnAnalyzerOverrideDao, never()).create(anyLong(), any());
	}

	@Test
	public void testCreateWithMissingAnalyzer() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry()
			.setColumnName("myCol")
			.setAnalyzer(java.util.Map.of("$ref", "org.sagebionetworks-MISSING"));
		ColumnAnalyzerOverride request = new ColumnAnalyzerOverride()
			.setOrganizationName("test-org").setName("MyOverride")
			.setOverrides(Arrays.asList(entry));
		when(textAnalyzerDao.findNonExistentNames(Arrays.asList("org.sagebionetworks-MISSING")))
			.thenReturn(Arrays.asList("org.sagebionetworks-MISSING"));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> manager.create(admin, request));
		assertTrue(ex.getMessage().contains("text analyzer names do not exist"));
		verify(columnAnalyzerOverrideDao, never()).create(anyLong(), any());
	}

	// --- validateEntryAnalyzerNames: inline-or-$ref edge branches ---

	@Test
	public void testCreateWithEmptyOverridesListSkipsValidation() {
		// Empty overrides[] short-circuits the validator entirely — no DAO call to verify
		// existence, no shape conversion. The DAO create still proceeds.
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		ColumnAnalyzerOverride request = new ColumnAnalyzerOverride()
				.setOrganizationName("test-org").setName("MyOverride")
				.setOverrides(java.util.Collections.emptyList());
		when(columnAnalyzerOverrideDao.create(eq(1L), any())).thenReturn(request.setId("1"));

		// call under test
		manager.create(admin, request);

		verifyNoMoreInteractions(textAnalyzerDao);
		verify(columnAnalyzerOverrideDao).create(eq(1L), any());
	}

	@Test
	public void testCreateWithNullEntryAnalyzerIsSkipped() {
		// An entry with no analyzer at all is a degenerate but not invalid shape — the validator
		// silently skips it rather than NPE on SearchOpaqueJsonUtil.readRef. The DAO create proceeds.
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry()
				.setColumnName("myCol").setAnalyzer(null);
		ColumnAnalyzerOverride request = new ColumnAnalyzerOverride()
				.setOrganizationName("test-org").setName("MyOverride")
				.setOverrides(Arrays.asList(entry));
		when(columnAnalyzerOverrideDao.create(eq(1L), any())).thenReturn(request.setId("1"));

		// call under test
		manager.create(admin, request);

		verifyNoMoreInteractions(textAnalyzerDao);
	}

	@Test
	public void testCreateWithInlineAnalyzerLiteralIsConvertedNotLookedUp() {
		// An inline analyzer literal must round-trip through the typed deserializer (shape
		// validation) but no DAO existence check fires — inline literals are ephemeral.
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		java.util.Map<String, Object> inlineAnalyzer = java.util.Map.of(
				"analyzer", java.util.Map.of("default",
						java.util.Map.of("type", "custom", "tokenizer", "standard")));
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry()
				.setColumnName("myCol").setAnalyzer(inlineAnalyzer);
		ColumnAnalyzerOverride request = new ColumnAnalyzerOverride()
				.setOrganizationName("test-org").setName("MyOverride")
				.setOverrides(Arrays.asList(entry));
		when(columnAnalyzerOverrideDao.create(eq(1L), any())).thenReturn(request.setId("1"));

		// call under test
		manager.create(admin, request);

		verifyNoMoreInteractions(textAnalyzerDao);
		verify(columnAnalyzerOverrideDao).create(eq(1L), any());
	}

	@Test
	public void testCreateWithMixOfRefAndInlineOnlyChecksRefs() {
		// One ref + one inline literal: the validator collects only the ref qname into
		// findNonExistentNames; the inline shape is converted but not looked up.
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		ColumnAnalyzerOverrideEntry refEntry = new ColumnAnalyzerOverrideEntry()
				.setColumnName("c1").setAnalyzer(java.util.Map.of("$ref", "biomed-FOO"));
		ColumnAnalyzerOverrideEntry inlineEntry = new ColumnAnalyzerOverrideEntry()
				.setColumnName("c2").setAnalyzer(java.util.Map.of(
						"analyzer", java.util.Map.of("default",
								java.util.Map.of("type", "custom", "tokenizer", "standard"))));
		ColumnAnalyzerOverride request = new ColumnAnalyzerOverride()
				.setOrganizationName("test-org").setName("MyOverride")
				.setOverrides(Arrays.asList(refEntry, inlineEntry));
		when(textAnalyzerDao.findNonExistentNames(Arrays.asList("biomed-FOO")))
				.thenReturn(java.util.Collections.emptyList());
		when(columnAnalyzerOverrideDao.create(eq(1L), any())).thenReturn(request.setId("1"));

		// call under test
		manager.create(admin, request);

		verify(textAnalyzerDao).findNonExistentNames(Arrays.asList("biomed-FOO"));
	}

	@Test
	public void testCreateWithAllInlineEntriesSkipsTextAnalyzerDao() {
		// When every entry is inline there are no qnames to look up, so the validator never
		// calls findNonExistentNames at all (covers the L175 empty-qualifiedNames branch).
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		java.util.Map<String, Object> inline = java.util.Map.of(
				"analyzer", java.util.Map.of("default",
						java.util.Map.of("type", "custom", "tokenizer", "standard")));
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry()
				.setColumnName("c1").setAnalyzer(inline);
		ColumnAnalyzerOverride request = new ColumnAnalyzerOverride()
				.setOrganizationName("test-org").setName("MyOverride")
				.setOverrides(Arrays.asList(entry));
		when(columnAnalyzerOverrideDao.create(eq(1L), any())).thenReturn(request.setId("1"));

		// call under test
		manager.create(admin, request);

		verifyNoMoreInteractions(textAnalyzerDao);
	}

	@Test
	public void testCreateWithMalformedInlineAnalyzerThrows() {
		// The inline analyzer literal must round-trip through the OpenSearch typed
		// deserializer; an unknown filter type is rejected at create time before
		// the DAO is touched.
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		java.util.Map<String, Object> malformed = java.util.Map.of(
				"filter", java.util.Map.of("bogus", java.util.Map.of("type", "this_filter_does_not_exist")),
				"analyzer", java.util.Map.of("default",
						java.util.Map.of("type", "custom", "tokenizer", "standard",
								"filter", java.util.List.of("bogus"))));
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry()
				.setColumnName("c1").setAnalyzer(malformed);
		ColumnAnalyzerOverride request = new ColumnAnalyzerOverride()
				.setOrganizationName("test-org").setName("MyOverride")
				.setOverrides(Arrays.asList(entry));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> manager.create(admin, request));
		assertTrue(ex.getMessage().contains("analyzer settings"),
				"expected typed-deserializer rejection, got: " + ex.getMessage());
		verifyNoMoreInteractions(textAnalyzerDao);
		verifyNoMoreInteractions(columnAnalyzerOverrideDao);
	}
}
