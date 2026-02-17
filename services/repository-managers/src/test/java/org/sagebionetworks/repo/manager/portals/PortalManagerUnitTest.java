package org.sagebionetworks.repo.manager.portals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AccessControlListManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.UserPortalPermissions;
import org.sagebionetworks.repo.model.dbo.portals.DBOPortal;
import org.sagebionetworks.repo.model.dbo.portals.PortalDao;
import org.sagebionetworks.repo.model.portals.CreateOrUpdatePortalRequest;
import org.sagebionetworks.repo.model.portals.ListPortalsRequest;
import org.sagebionetworks.repo.model.portals.ListPortalsResponse;
import org.sagebionetworks.repo.model.portals.Portal;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class PortalManagerUnitTest {

	@Mock
	private PortalDao mockPortalDao;

	@Mock
	private AccessControlListManager mockAclManager;

	@InjectMocks
	private PortalManagerImpl manager;

	private UserInfo user;

	private CreateOrUpdatePortalRequest request;
	
	private Portal portal;
	
	private AccessControlList acl;

	@BeforeEach
	public void before() {
		user = new UserInfo(false, 1234L, AuthorizationConstants.DEFAULT_REALM_ID);
		user.setGroups(Set.of(user.getId(), BOOTSTRAP_PRINCIPAL.PORTAL_MANAGERS.getPrincipalId()));
		
		request = new CreateOrUpdatePortalRequest().setName("My Portal").setUrl("https://myportal.synapse.org");
		portal = new Portal().setId("123").setCreatedOn(new Date()).setCreatedBy(user.getId().toString());
		acl = AccessControlListUtil.createACL(portal.getId().toString(), user, PortalManager.DEFAULT_PERMISSIONS, portal.getCreatedOn());
	}

	@Test
	public void testCreatePortalWithAdmin() {

		when(mockPortalDao.createPortal(user.getId(), request.getName(), request.getUrl())).thenReturn(portal);

		// Call under test
		assertEquals(portal, manager.createPortal(user, request));

		verify(mockAclManager).create(user, acl, ObjectType.PORTAL, Long.parseLong(portal.getCreatedBy()));
	}

	@Test
	public void testCreatePortalWithNotAdmin() {

		user = new UserInfo(false);

		assertEquals("You are not authorized to perform this operation.", assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.createPortal(user, request);
		}).getMessage());

		verifyZeroInteractions(mockAclManager, mockPortalDao);
	}

	@Test
	public void testCreatePortalWithNoUser() {

		user = null;

		assertEquals("The user is required.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.createPortal(user, request);
		}).getMessage());

		verifyZeroInteractions(mockAclManager, mockPortalDao);
	}

	@Test
	public void testCreatePortalWithNoRequest() {

		request = null;

		assertEquals("The request is required.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.createPortal(user, request);
		}).getMessage());

		verifyZeroInteractions(mockAclManager, mockPortalDao);
	}

	@Test
	public void testCreatePortalWithEmptyName() {

		request.setName("");

		assertEquals("The name is required and must not be the empty string.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.createPortal(user, request);
		}).getMessage());

		verifyZeroInteractions(mockAclManager, mockPortalDao);
	}

	@Test
	public void testCreatePortalWithEmptyOrInvalidUrl() {

		request.setUrl("");

		assertEquals("The url is not a valid url: ", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.createPortal(user, request);
		}).getMessage());

		request.setUrl("http://malformed");
		
		assertEquals("The url is not a valid url: http://malformed", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.createPortal(user, request);
		}).getMessage());

		verifyZeroInteractions(mockAclManager, mockPortalDao);
	}

	@Test
	public void testUpdatePortalWithAdmin() {

		when(mockPortalDao.getPortal(portal.getId())).thenReturn(Optional.of(portal));
		when(mockPortalDao.updatePortal(user.getId(), portal.getId(), request.getName(), request.getUrl())).thenReturn(portal);

		// Call under test
		assertEquals(portal, manager.updatePortal(user, portal.getId(), request));

		verifyZeroInteractions(mockAclManager);
	}

	@Test
	public void testUpdatePortalWithNotAdminAndAuthorized() {

		user = new UserInfo(false);
		
		when(mockAclManager.canAccess(user, portal.getId(), ObjectType.PORTAL, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		when(mockPortalDao.getPortal(portal.getId())).thenReturn(Optional.of(portal));
		when(mockPortalDao.updatePortal(user.getId(), portal.getId(), request.getName(), request.getUrl())).thenReturn(portal);

		// Call under test
		assertEquals(portal, manager.updatePortal(user, portal.getId(), request));
		
		verifyNoMoreInteractions(mockPortalDao, mockAclManager);

	}

	@Test
	public void testUpdatePortalWithNotAdminAndNotAuthorized() {

		user = new UserInfo(false);

		when(mockAclManager.canAccess(user, "123", ObjectType.PORTAL, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.accessDenied("Nope"));

		assertEquals("Nope", assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.updatePortal(user, "123", request);
		}).getMessage());

		verifyNoMoreInteractions(mockPortalDao, mockAclManager);
	}

	@Test
	public void testUpdatePortalWithNonExisting() {

		when(mockPortalDao.getPortal(portal.getId())).thenReturn(Optional.empty());

		assertEquals("A portal with the given id does not exist.", assertThrows(NotFoundException.class, () -> {
			// Call under test
			manager.updatePortal(user, portal.getId(), request);			
		}).getMessage());

		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testUpdatePortalWithNoUser() {

		user = null;

		assertEquals("The user is required.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.updatePortal(user, "123", request);
		}).getMessage());

		verifyZeroInteractions(mockAclManager, mockPortalDao);
	}

	@Test
	public void testUpdatePortalWithNoPortalId() {

		assertEquals("The portalId is required.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.updatePortal(user, null, request);
		}).getMessage());

		verifyZeroInteractions(mockAclManager, mockPortalDao);
	}

	@Test
	public void testUpdatePortalWithNoRequest() {

		request = null;

		assertEquals("The request is required.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.updatePortal(user, "123", request);
		}).getMessage());

		verifyZeroInteractions(mockAclManager, mockPortalDao);
	}

	@Test
	public void testUpdatePortalWithEmptyName() {

		request.setName("");

		assertEquals("The name is required and must not be the empty string.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.updatePortal(user, "123", request);
		}).getMessage());

		verifyZeroInteractions(mockAclManager, mockPortalDao);
	}

	@Test
	public void testUpdatePortalWithEmptyOrInvalidUrl() {

		request.setUrl("");

		assertEquals("The url is not a valid url: ", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.updatePortal(user, "123", request);
		}).getMessage());

		request.setUrl("http://malformed");
		
		assertEquals("The url is not a valid url: http://malformed", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.updatePortal(user, "123", request);
		}).getMessage());

		verifyZeroInteractions(mockAclManager, mockPortalDao);
	}

	@Test
	public void testDeletePortalWithAdmin() {
		when(mockPortalDao.getPortal(portal.getId())).thenReturn(Optional.of(portal));
		
		doNothing().when(mockPortalDao).deletePortal(portal.getId());
		
		// Call under test
		manager.deletePortal(user, portal.getId());
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testDeletePortalWithNotAdminAndAuthorized() {
		user = new UserInfo(false);
		
		when(mockAclManager.canAccess(user, portal.getId(), ObjectType.PORTAL, ACCESS_TYPE.DELETE)).thenReturn(AuthorizationStatus.authorized());
		when(mockPortalDao.getPortal(portal.getId())).thenReturn(Optional.of(portal));
		
		doNothing().when(mockPortalDao).deletePortal(portal.getId());
		
		// Call under test
		manager.deletePortal(user, portal.getId());
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testDeletePortalWithNotAdminAndNotAuthorized() {
		user = new UserInfo(false);
		
		when(mockAclManager.canAccess(user, portal.getId(), ObjectType.PORTAL, ACCESS_TYPE.DELETE)).thenReturn(AuthorizationStatus.accessDenied("Nope"));
		
		assertEquals("Nope", assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			manager.deletePortal(user, portal.getId());
		}).getMessage());
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testDeletePortalWithNonExisting() {
		when(mockPortalDao.getPortal(portal.getId())).thenReturn(Optional.empty());
		
		assertEquals("A portal with the given id does not exist.", assertThrows(NotFoundException.class, () -> {
			// Call under test
			manager.deletePortal(user, portal.getId());
		}).getMessage());
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testDeletePortalWithNoUser() {
		user = null;
		
		assertEquals("The user is required.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.deletePortal(user, portal.getId());
		}).getMessage());
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testDeletePortalWithNoPortalId() {
		portal.setId(null);
		
		assertEquals("The portalId is required.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.deletePortal(user, portal.getId());
		}).getMessage());
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testGetPortal() {
		when(mockPortalDao.getPortal(portal.getId())).thenReturn(Optional.of(portal));
		
		// Call under test
		assertEquals(portal, manager.getPortal(portal.getId()));
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testGetPortalWithNotExisting() {
		when(mockPortalDao.getPortal(portal.getId())).thenReturn(Optional.empty());
		
		assertEquals("A portal with the given id does not exist.", assertThrows(NotFoundException.class, () -> {			
			// Call under test
			manager.getPortal(portal.getId());
		}).getMessage());
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
		
	@Test
	public void testGetPortalWithNoPortalId() {
		portal.setId(null);
		
		assertEquals("The portalId is required.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.getPortal(portal.getId());
		}).getMessage());
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testListPortals() {
		when(mockPortalDao.getPortalPage(NextPageToken.DEFAULT_LIMIT + 1, NextPageToken.DEFAULT_OFFSET)).thenReturn(List.of(portal));
		
		// Call under test
		assertEquals(new ListPortalsResponse().setPage(List.of(portal)), manager.listPortals(new ListPortalsRequest()));
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testListPortalsWithNextPageToken() {
		NextPageToken nextPageToken = new NextPageToken(10, 3);
		
		when(mockPortalDao.getPortalPage(nextPageToken.getLimitForQuery(), nextPageToken.getOffset())).thenReturn(List.of(portal));
		
		// Call under test
		assertEquals(new ListPortalsResponse().setPage(List.of(portal)), manager.listPortals(new ListPortalsRequest().setNextPageToken(nextPageToken.toToken())));
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testListPortalsWithNoRequest() {
		
		assertEquals("The request is required.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.listPortals(null);
		}).getMessage());
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testGetPortalAcl() {
		when(mockAclManager.getAcl(portal.getId(), ObjectType.PORTAL)).thenReturn(Optional.of(acl));
		
		// Call under test
		manager.getPortalAcl(portal.getId());
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testGetPortalAclWithNotFound() {
		when(mockAclManager.getAcl(portal.getId(), ObjectType.PORTAL)).thenReturn(Optional.empty());
		
		assertEquals("Could not find an ACL for the portal with the given id.", assertThrows(NotFoundException.class, () -> {			
			// Call under test
			manager.getPortalAcl(portal.getId());
		}).getMessage());
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testGetPortalAclWithNoPortalId() {
		portal.setId(null);
		
		assertEquals("The portalId is required.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.getPortalAcl(portal.getId());
		}).getMessage());
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testUpdatePortalAclWithAdmin() {
		doNothing().when(mockAclManager).update(user, acl, ObjectType.PORTAL, Long.parseLong(portal.getCreatedBy()));
		when(mockAclManager.getAcl(portal.getId(), ObjectType.PORTAL)).thenReturn(Optional.of(acl));
		when(mockPortalDao.getPortal(portal.getId())).thenReturn(Optional.of(portal));
		// Call under test
		assertEquals(acl, manager.updatePortalAcl(user, portal.getId(), acl));
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testUpdatePortalAclWithNotAdminAndAuthorized() {
		user.setGroups(Set.of(user.getId()));

		when(mockAclManager.canAccess(user, portal.getId(), ObjectType.PORTAL, ACCESS_TYPE.CHANGE_PERMISSIONS)).thenReturn(AuthorizationStatus.authorized());
		doNothing().when(mockAclManager).update(user, acl, ObjectType.PORTAL, Long.parseLong(portal.getCreatedBy()));
		when(mockAclManager.getAcl(portal.getId(), ObjectType.PORTAL)).thenReturn(Optional.of(acl));
		when(mockPortalDao.getPortal(portal.getId())).thenReturn(Optional.of(portal));
		// Call under test
		assertEquals(acl, manager.updatePortalAcl(user, portal.getId(), acl));
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testUpdatePortalAclWithNotAdminAndNotAuthorized() {
		user.setGroups(Set.of(user.getId()));
		
		when(mockAclManager.canAccess(user, portal.getId(), ObjectType.PORTAL, ACCESS_TYPE.CHANGE_PERMISSIONS)).thenReturn(AuthorizationStatus.accessDenied("Nope"));
				
		assertEquals("Nope", assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.updatePortalAcl(user, portal.getId(), acl);
		}).getMessage());
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testCanMintDoi() {
		
		// Call under test
		assertEquals(AuthorizationStatus.authorized(), manager.canMintDoi(user, portal.getId()));
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testCanMintDoiWithNotAdminAndAuthorized() {
		user.setGroups(Set.of(user.getId()));
		
		when(mockAclManager.canAccess(user, portal.getId(), ObjectType.PORTAL, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		
		// Call under test
		assertEquals(AuthorizationStatus.authorized(), manager.canMintDoi(user, portal.getId()));
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testCanMintDoiWithNotAdminAndNotAuthorized() {
		user.setGroups(Set.of(user.getId()));
		
		when(mockAclManager.canAccess(user, portal.getId(), ObjectType.PORTAL, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.accessDenied("Nope"));
		
		// Call under test
		assertEquals(AuthorizationStatus.accessDenied("Nope"), manager.canMintDoi(user, portal.getId()));
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
	
	@Test
	public void testCanMintDoiWithSynapsePortal() {
		portal.setId(DBOPortal.SYNAPSE_PORTAL_ID.toString());
		
		// Call under test
		assertEquals(AuthorizationStatus.authorized(), manager.canMintDoi(user, portal.getId()));
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
						
	}
	
	@Test
	public void testGetUserPortalPermissions() {
		PortalManagerImpl managerSpy = Mockito.spy(manager);
		
		when(managerSpy.canMintDoi(user, portal.getId())).thenReturn(AuthorizationStatus.authorized());
		
		UserPortalPermissions expected = new UserPortalPermissions()
				.setCanMintDoi(true);
		
		// Call under test
		assertEquals(expected, managerSpy.getUserPortalPermissions(user, portal.getId()));
		
		verifyNoMoreInteractions(mockAclManager, mockPortalDao);
	}
}
