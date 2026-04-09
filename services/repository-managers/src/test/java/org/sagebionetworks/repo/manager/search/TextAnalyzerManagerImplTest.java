package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersRequest;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersResponse;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.model.search.table.TextAnalyzerSettings;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
public class TextAnalyzerManagerImplTest {

	@Mock
	private TextAnalyzerDao textAnalyzerDao;
	@Mock
	private AccessControlListDAO aclDao;
	@Mock
	private OrganizationDao organizationDao;

	@InjectMocks
	private TextAnalyzerManagerImpl manager;

	private UserInfo sageUser;
	private UserInfo nonSageUser;
	private UserInfo adminUser;

	@BeforeEach
	void setUp() {
		sageUser = new UserInfo(false);
		sageUser.setId(1L);
		sageUser.setGroups(Set.of(1L, BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId()));

		nonSageUser = new UserInfo(false);
		nonSageUser.setId(2L);
		nonSageUser.setGroups(Set.of(2L));

		adminUser = new UserInfo(true);
		adminUser.setId(3L);
	}

	// --- Create authorization ---

	@Test
	public void testCreateAsNonSageUserThrows() {
		TextAnalyzer input = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(new TextAnalyzerSettings());

		assertThrows(UnauthorizedException.class, () -> manager.create(nonSageUser, input));
		verifyZeroInteractions(textAnalyzerDao);
	}

	@Test
	public void testCreateAsAnonymousThrows() {
		UserInfo anon = new UserInfo(false);
		anon.setId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		anon.setGroups(Set.of(anon.getId()));

		TextAnalyzer input = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(new TextAnalyzerSettings());

		assertThrows(UnauthorizedException.class, () -> manager.create(anon, input));
	}

	@Test
	public void testCreateWithoutOrgAclThrows() {
		TextAnalyzer input = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(new TextAnalyzerSettings());
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.CREATE)))
			.thenReturn(AuthorizationStatus.accessDenied("no"));

		assertThrows(UnauthorizedException.class, () -> manager.create(sageUser, input));
	}

	@Test
	public void testCreateHappyPath() {
		TextAnalyzer input = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(new TextAnalyzerSettings());
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.CREATE)))
			.thenReturn(AuthorizationStatus.authorized());
		when(textAnalyzerDao.create(any(), eq(1L))).thenReturn(input.setId("1000"));

		TextAnalyzer result = manager.create(sageUser, input);
		assertNotNull(result);
		assertEquals("1000", result.getId());
	}

	@Test
	public void testCreateAsAdminBypassesAcl() {
		TextAnalyzer input = new TextAnalyzer()
			.setOrganizationName("test-org").setName("test").setSettings(new TextAnalyzerSettings());
		when(textAnalyzerDao.create(any(), eq(3L))).thenReturn(input.setId("1000"));

		TextAnalyzer result = manager.create(adminUser, input);
		assertNotNull(result);
		verifyZeroInteractions(aclDao);
	}

	// --- Get ---

	@Test
	public void testGetExisting() {
		TextAnalyzer analyzer = new TextAnalyzer().setId("1");
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(analyzer));

		assertEquals("1", manager.get(new UserInfo(false), 1L).getId());
		verifyZeroInteractions(aclDao);
	}

	@Test
	public void testGetNotFoundThrows() {
		when(textAnalyzerDao.get(999L)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> manager.get(new UserInfo(false), 999L));
	}

	// --- Update authorization ---

	@Test
	public void testUpdateAsNonSageUserThrows() {
		TextAnalyzer input = new TextAnalyzer()
			.setId("1").setOrganizationName("test-org").setName("updated");

		assertThrows(UnauthorizedException.class, () -> manager.update(nonSageUser, input));
		verifyZeroInteractions(textAnalyzerDao);
	}

	@Test
	public void testUpdateWithoutOrgAclThrows() {
		TextAnalyzer input = new TextAnalyzer()
			.setId("1").setOrganizationName("test-org").setName("updated");
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(new TextAnalyzer().setId("1").setOrganizationName("test-org")));
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.UPDATE)))
			.thenReturn(AuthorizationStatus.accessDenied("no"));

		assertThrows(UnauthorizedException.class, () -> manager.update(sageUser, input));
	}

	@Test
	public void testUpdateNotFoundThrows() {
		TextAnalyzer input = new TextAnalyzer()
			.setId("999").setOrganizationName("test-org").setName("updated");
		when(textAnalyzerDao.get(999L)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> manager.update(sageUser, input));
	}

	@Test
	public void testUpdateRejectsOrgNameMismatch() {
		TextAnalyzer input = new TextAnalyzer()
			.setId("1").setOrganizationName("other-org").setName("updated");
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(new TextAnalyzer().setId("1").setOrganizationName("test-org")));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> manager.update(sageUser, input));
		assertTrue(ex.getMessage().contains("organizationName cannot be changed"));
	}

	@Test
	public void testUpdateHappyPath() {
		TextAnalyzer input = new TextAnalyzer()
			.setId("1").setOrganizationName("test-org").setName("updated");
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(new TextAnalyzer().setId("1").setOrganizationName("test-org")));
		when(organizationDao.getOrganizationByName("test-org")).thenReturn(new Organization().setId("42"));
		when(aclDao.canAccess(any(UserInfo.class), eq("42"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.UPDATE)))
			.thenReturn(AuthorizationStatus.authorized());
		when(textAnalyzerDao.update(any(), eq(1L))).thenReturn(input);

		TextAnalyzer result = manager.update(sageUser, input);
		assertEquals("updated", result.getName());
	}

	// --- Delete authorization ---

	@Test
	public void testDeleteAsNonSageUserThrows() {
		assertThrows(UnauthorizedException.class, () -> manager.delete(nonSageUser, 1L));
		verifyZeroInteractions(textAnalyzerDao);
	}

	@Test
	public void testDeleteNotFoundThrows() {
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> manager.delete(adminUser, 1L));
	}

	@Test
	public void testDeleteChecksOrgAclFromStoredEntity() {
		TextAnalyzer existing = new TextAnalyzer().setId("1").setOrganizationName("other-org");
		when(textAnalyzerDao.get(1L)).thenReturn(Optional.of(existing));
		when(organizationDao.getOrganizationByName("other-org")).thenReturn(new Organization().setId("99"));
		when(aclDao.canAccess(any(UserInfo.class), eq("99"), eq(ObjectType.ORGANIZATION), eq(ACCESS_TYPE.DELETE)))
			.thenReturn(AuthorizationStatus.accessDenied("no"));

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

		ListTextAnalyzersResponse response = manager.list(new UserInfo(false), request);

		assertEquals(1, response.getResults().size());
	}

	@Test
	public void testListAllWhenNoOrgName() {
		when(textAnalyzerDao.listAll(anyLong(), anyLong()))
			.thenReturn(Arrays.asList(new TextAnalyzer()));

		ListTextAnalyzersRequest request = new ListTextAnalyzersRequest();

		ListTextAnalyzersResponse response = manager.list(new UserInfo(false), request);

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
		ListTextAnalyzersResponse response = manager.list(new UserInfo(false), request);

		assertNotNull(response.getNextPageToken(), "Expected a next page token when DAO returns more than limit results");
		assertEquals(50, response.getResults().size());
	}

	@Test
	public void testListReturnsNullNextPageTokenWhenNoMoreResults() {
		List<TextAnalyzer> page = Arrays.asList(new TextAnalyzer().setId("1"));
		when(textAnalyzerDao.listAll(eq(51L), eq(0L))).thenReturn(page);

		ListTextAnalyzersRequest request = new ListTextAnalyzersRequest();
		ListTextAnalyzersResponse response = manager.list(new UserInfo(false), request);

		assertNull(response.getNextPageToken(), "Expected null next page token when all results fit in one page");
		assertEquals(1, response.getResults().size());
	}
}
