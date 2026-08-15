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
import org.sagebionetworks.repo.model.dbo.search.SynonymSetDao;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsRequest;
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsResponse;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class SynonymSetManagerImplTest {

	@Mock
	private SynonymSetDao synonymSetDao;
	@Mock
	private AccessControlListDAO aclDao;
	@Mock
	private OrganizationDao organizationDao;

	private SynonymSetManagerImpl manager;

	@BeforeEach
	void setUp() {
		manager = new SynonymSetManagerImpl(synonymSetDao, aclDao, organizationDao);
	}

	// --- Sage employee / admin authorization ---

	@Test
	public void testCreateWithNonSageUser() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L));

		// call under test
		assertThrows(UnauthorizedException.class, () ->
			manager.create(user, validSynonymSet()));
		verifyNoMoreInteractions(aclDao);
		verifyNoMoreInteractions(synonymSetDao);
	}

	@Test
	public void testUpdateWithNonSageUser() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L));

		// call under test
		assertThrows(UnauthorizedException.class, () ->
			manager.update(user, validSynonymSet().setId("1")));
		verifyNoMoreInteractions(aclDao);
		verifyNoMoreInteractions(synonymSetDao);
	}

	@Test
	public void testDeleteWithNonSageUser() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L));

		// call under test
		assertThrows(UnauthorizedException.class, () ->
			manager.delete(user, "1"));
		verifyNoMoreInteractions(aclDao);
		verifyNoMoreInteractions(synonymSetDao);
	}

	@Test
	public void testCreateWithAnonymousUser() {
		UserInfo anon = new UserInfo(false, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID, Set.of(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId()));

		// call under test
		assertThrows(UnauthorizedException.class, () ->
			manager.create(anon, validSynonymSet()));
		verifyNoMoreInteractions(aclDao);
		verifyNoMoreInteractions(synonymSetDao);
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
			manager.create(user, validSynonymSet()));
		verifyNoMoreInteractions(synonymSetDao);
	}

	@Test
	public void testCreateWithUserWithOrgAcl() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.CREATE)))
			.thenReturn(AuthorizationStatus.authorized());
		SynonymSet input = new SynonymSet().setOrganizationName("test-org").setName("test")
				.setDefinition("{\"type\":\"synonym_graph\",\"synonyms\":[\"a, b\"]}");
		when(synonymSetDao.create(eq(1L), any())).thenReturn(input.setId("1"));

		// call under test
		SynonymSet result = manager.create(user, input);
		assertNotNull(result);
	}

	@Test
	public void testCreateWithAdminBypassesOrgAcl() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		SynonymSet input = new SynonymSet().setOrganizationName("test-org").setName("test")
				.setDefinition("{\"type\":\"synonym_graph\",\"synonyms\":[\"a, b\"]}");
		when(synonymSetDao.create(eq(1L), any())).thenReturn(input.setId("1"));

		// call under test
		SynonymSet result = manager.create(admin, input);
		assertNotNull(result);
		verifyNoMoreInteractions(aclDao);
	}

	// --- Public read ---

	@Test
	public void testGetWithPublicAccess() {
		when(synonymSetDao.get("1")).thenReturn(Optional.of(new SynonymSet()));

		// call under test
		manager.get(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), "1");
		verifyNoMoreInteractions(aclDao);
	}

	// --- Deletion ---

	@Test
	public void testDeleteWithAdmin() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		SynonymSet existing = new SynonymSet().setId("1").setOrganizationName("test-org");
		when(synonymSetDao.get("1")).thenReturn(Optional.of(existing));

		// call under test
		manager.delete(admin, "1");
		verify(synonymSetDao).delete("1");
	}

	@Test
	public void testDeleteWithFkConstraint() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		SynonymSet existing = new SynonymSet().setId("1").setOrganizationName("test-org");
		when(synonymSetDao.get("1")).thenReturn(Optional.of(existing));
		org.mockito.Mockito.doThrow(new IllegalArgumentException("Cannot delete synonym set '1' because it is still referenced."))
			.when(synonymSetDao).delete("1");

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
			manager.delete(admin, "1"));
		assertTrue(ex.getMessage().contains("still referenced"));
	}

	// --- Not found ---

	@Test
	public void testGetWithNonExistentId() {
		when(synonymSetDao.get("999")).thenReturn(Optional.empty());

		// call under test
		NotFoundException ex = assertThrows(NotFoundException.class, () -> manager.get(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), "999"));
		assertEquals("A synonym set with the given id does not exist.", ex.getMessage());
	}

	// --- List / pagination ---

	@Test
	public void testListWithNoOrgName() {
		SynonymSet item = new SynonymSet().setId("1");
		when(synonymSetDao.listAll(anyLong(), anyLong()))
			.thenReturn(Arrays.asList(item));

		ListSynonymSetsRequest request = new ListSynonymSetsRequest();

		// call under test
		ListSynonymSetsResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);
		assertEquals(1, response.getResults().size());
		verify(synonymSetDao).listAll(anyLong(), anyLong());
		verify(synonymSetDao, never()).list(anyString(), anyLong(), anyLong());
	}

	@Test
	public void testListWithOrgName() {
		SynonymSet item = new SynonymSet().setId("1");
		when(synonymSetDao.list(eq("test-org"), anyLong(), anyLong()))
			.thenReturn(Arrays.asList(item));

		ListSynonymSetsRequest request = new ListSynonymSetsRequest();
		request.setOrganizationName("test-org");

		// call under test
		ListSynonymSetsResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);
		assertEquals(1, response.getResults().size());
		assertEquals("1", response.getResults().get(0).getId());
	}

	// --- Update ACL ---

	@Test
	public void testUpdateWithUserWithoutOrgAcl() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		SynonymSet request = new SynonymSet().setId("1").setOrganizationName("test-org").setName("test_name")
				.setDefinition("{\"type\":\"synonym_graph\",\"synonyms\":[\"a, b\"]}");
		when(synonymSetDao.get("1")).thenReturn(Optional.of(new SynonymSet().setId("1").setOrganizationName("test-org").setName("test_name")));
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.UPDATE)))
			.thenReturn(AuthorizationStatus.accessDenied("no"));

		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.update(user, request));
		verify(synonymSetDao, never()).update(anyLong(), any());
	}

	@Test
	public void testUpdateWithOrgNameMismatch() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		SynonymSet request = new SynonymSet().setId("1").setOrganizationName("other-org").setName("test_name")
				.setDefinition("{\"type\":\"synonym_graph\",\"synonyms\":[\"a, b\"]}");
		when(synonymSetDao.get("1")).thenReturn(Optional.of(new SynonymSet().setId("1").setOrganizationName("test-org")));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> manager.update(admin, request));
		assertTrue(ex.getMessage().contains("organizationName cannot be changed"));
	}

	@Test
	public void testUpdateWithNameChangeThrows() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		SynonymSet request = new SynonymSet()
			.setId("1").setOrganizationName("test-org").setName("new_name")
			.setDefinition("{\"type\":\"synonym_graph\",\"synonyms\":[\"a, b\"]}");
		when(synonymSetDao.get("1")).thenReturn(Optional.of(
			new SynonymSet().setId("1").setOrganizationName("test-org").setName("original_name")));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> manager.update(admin, request));

		assertTrue(ex.getMessage().contains("name cannot be changed"));
		verify(synonymSetDao, never()).update(any(), any());
	}

	@Test
	public void testListWithMoreResults() {
		List<SynonymSet> page = new ArrayList<>();
		for (int i = 0; i < 51; i++) {
			page.add(new SynonymSet().setId(String.valueOf(i)));
		}
		when(synonymSetDao.listAll(eq(51L), eq(0L))).thenReturn(page);

		ListSynonymSetsRequest request = new ListSynonymSetsRequest();

		// call under test
		ListSynonymSetsResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);

		assertNotNull(response.getNextPageToken(), "Expected a next page token when DAO returns more than limit results");
		assertEquals(50, response.getResults().size());
	}

	@Test
	public void testDeleteWithNonExistentId() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		when(synonymSetDao.get("999")).thenReturn(Optional.empty());

		// call under test
		NotFoundException ex = assertThrows(NotFoundException.class, () -> manager.delete(admin, "999"));
		assertEquals("A synonym set with the given id does not exist.", ex.getMessage());
	}

	@Test
	public void testUpdateWithNonExistentId() {
		UserInfo admin = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		SynonymSet request = validSynonymSet().setId("999").setName("updated");
		when(synonymSetDao.get("999")).thenReturn(Optional.empty());

		// call under test
		NotFoundException ex = assertThrows(NotFoundException.class, () -> manager.update(admin, request));
		assertEquals("A synonym set with the given id does not exist.", ex.getMessage());
	}

	@Test
	public void testDeleteWithOrgAclCheck() {
		UserInfo user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));
		SynonymSet existing = new SynonymSet().setId("1").setOrganizationName("other-org");
		when(synonymSetDao.get("1")).thenReturn(Optional.of(existing));
		when(organizationDao.getOrganizationByName("other-org")).thenReturn(new Organization().setId("99"));
		when(aclDao.canAccess(any(UserInfo.class), eq("99"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.DELETE)))
			.thenReturn(AuthorizationStatus.accessDenied("no"));

		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.delete(user, "1"));
		verify(aclDao).canAccess(any(UserInfo.class), eq("99"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.DELETE));
		verify(synonymSetDao, never()).delete(anyString());
	}

	@Test
	public void testListWithNoMoreResults() {
		List<SynonymSet> page = Arrays.asList(new SynonymSet().setId("1"));
		when(synonymSetDao.listAll(eq(51L), eq(0L))).thenReturn(page);

		ListSynonymSetsRequest request = new ListSynonymSetsRequest();

		// call under test
		ListSynonymSetsResponse response = manager.list(new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID), request);

		assertNull(response.getNextPageToken(), "Expected null next page token when all results fit in one page");
		assertEquals(1, response.getResults().size());
	}

	/**
	 * Returns a structurally valid {@link SynonymSet} (organizationName, name, definition all
	 * set) so tests targeting the authorization layer reach the auth check rather than
	 * tripping on the structural validation that runs first in
	 * {@link SynonymSetManagerImpl#create}/{@code update}.
	 */
	private static SynonymSet validSynonymSet() {
		return new SynonymSet()
				.setOrganizationName("test-org")
				.setName("test")
				.setDefinition("{\"type\":\"synonym_graph\",\"synonyms\":[\"a, b\"]}");
	}

	@Test
	public void testAdminBypassesOrgAclOnUpdate() {
		// Admin users skip the per-org ACL check on update — covers the !user.isAdmin()=false
		// half of the L95 guard.
		UserInfo admin = new UserInfo(true, 2L, AuthorizationConstants.DEFAULT_REALM_ID);
		SynonymSet existing = validSynonymSet().setId("1");
		SynonymSet input = validSynonymSet().setId("1");
		when(synonymSetDao.get("1")).thenReturn(java.util.Optional.of(existing));
		when(synonymSetDao.update(2L, input)).thenReturn(input);

		// call under test
		manager.update(admin, input);

		verifyNoMoreInteractions(aclDao);
		verify(synonymSetDao).update(2L, input);
	}
}
