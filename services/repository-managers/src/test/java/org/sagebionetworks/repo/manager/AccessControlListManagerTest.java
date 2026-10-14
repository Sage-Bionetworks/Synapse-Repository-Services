package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;

import com.google.common.collect.Sets;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;

@ExtendWith(MockitoExtension.class)
public class AccessControlListManagerTest {
	
	@Mock
	private AccessControlListDAO aclDao;

	@Mock
	private UserGroupDAO userGroupDAO;
	
	@InjectMocks
	private AccessControlListManagerImpl aclManager;
	
	private UserInfo userInfo;
	private UserInfo adminUser;
	private AccessControlList acl;

	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, 123L, AuthorizationConstants.DEFAULT_REALM_ID);
		adminUser = new UserInfo(true, 456L, AuthorizationConstants.DEFAULT_REALM_ID);
		acl = new AccessControlList().setId("222").setCreationDate(new Date());
		acl.setResourceAccess(Set.of(new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.READ)),
				new ResourceAccess().setPrincipalId(123L).setAccessType(Set.of(ACCESS_TYPE.READ)) ));
	}

	@Test
	public void testCanReadBenefactorsAdmin(){
		Set<Long> benefactors = Sets.newHashSet(1L,2L);
		// call under test
		Set<Long> results = aclManager.getAccessibleBenefactors(adminUser, ObjectType.ENTITY, benefactors);
		assertEquals(benefactors, results);
		verify(aclDao, never()).getAccessibleBenefactors(any(Set.class), any(Set.class), any(ObjectType.class), any(ACCESS_TYPE.class));
	}
	
	@Test
	public void testCanReadBenefactorsNonAdmin(){
		Set<Long> benefactors = Sets.newHashSet(1L,2L);
		// call under test
		aclManager.getAccessibleBenefactors(userInfo, ObjectType.ENTITY, benefactors);
		verify(aclDao, times(1)).getAccessibleBenefactors(any(Set.class), any(Set.class), any(ObjectType.class), any(ACCESS_TYPE[].class));
	}
	
	@Test
	public void testCanReadBenefactorsTrashAdmin(){
		Set<Long> benefactors = Sets.newHashSet(AuthorizationManagerImpl.TRASH_FOLDER_ID);
		// call under test
		Set<Long> results = aclManager.getAccessibleBenefactors(adminUser, ObjectType.ENTITY, benefactors);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	@Test
	public void testCanReadBenefactorsTrashNonAdmin(){
		Set<Long> benefactors = Sets.newHashSet(AuthorizationManagerImpl.TRASH_FOLDER_ID);
		when(aclDao.getAccessibleBenefactors(any(Set.class), any(Set.class), any(ObjectType.class), any(ACCESS_TYPE[].class))).thenReturn(benefactors);
		// call under test
		Set<Long> results = aclManager.getAccessibleBenefactors(userInfo, ObjectType.ENTITY, benefactors);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	@Test
	public void testGetAccessibleProjectIds(){
		Set<Long> expectedProjectIds = Sets.newHashSet(555L);
		Set<Long> principalIds = Sets.newHashSet(123L);
		when(aclDao.getAccessibleProjectIds(principalIds, ACCESS_TYPE.READ)).thenReturn(expectedProjectIds);
		Set<Long> results = aclManager.getAccessibleProjectIds(principalIds);
		assertEquals(expectedProjectIds,results);
	}
	
	@Test
	public void testGetAccessibleProjectIdsEmpty(){
		Set<Long> principalIds = new HashSet<>();
		Set<Long> results = aclManager.getAccessibleProjectIds(principalIds);
		assertNotNull(results);
		assertTrue(results.isEmpty());
		verify(aclDao, never()).getAccessibleProjectIds(any(Set.class), any(ACCESS_TYPE.class));
	}
	
	@Test
	public void testGetAccessibleProjectIdsNullPrincipals(){
		Set<Long> principalIds = null;
		assertThrows(IllegalArgumentException.class, ()-> {
			aclManager.getAccessibleProjectIds(principalIds);
		});
	}

	@Test
	public void testCreate(){
		when(userGroupDAO.getUsersRealms(anyList())).thenReturn(Map.of("0", Set.of("1", "123")));

		// call under test
		aclManager.create(userInfo, acl, ObjectType.ENTITY, userInfo.getId());
		verify(aclDao, times(1)).create(acl, ObjectType.ENTITY);
	}


	@Test
	public void testDefaultRealmAdminCreateACLForDifferentRealm() {
		UserInfo otherRealmUser = new UserInfo(true, 666L, "1");
		acl.setResourceAccess(Set.of(new ResourceAccess().setPrincipalId(otherRealmUser.getId()).setAccessType(Set.of(ACCESS_TYPE.READ)),
				new ResourceAccess().setPrincipalId(adminUser.getId()).setAccessType(Set.of(ACCESS_TYPE.READ))));
		when(userGroupDAO.getUsersRealms(anyList())).thenReturn(Map.of("1", Set.of(otherRealmUser.getId().toString())));

		// Admin should not be able to create ACL for a different realm
		String message = assertThrows(InvalidModelException.class, () -> {
			aclManager.create(adminUser, acl, ObjectType.ENTITY, adminUser.getId());
		}).getMessage();
		assertEquals("All principals in the ACL must be from the same realm as the caller principal.", message);

		// non admin should not be able to create ACL for a different realm
		String messageTwo = assertThrows(InvalidModelException.class, () -> {
			aclManager.create(userInfo, acl, ObjectType.ENTITY, userInfo.getId());
		}).getMessage();
		assertEquals("All principals in the ACL must be from the same realm as the caller principal.", messageTwo);
	}

	@Test
	public void testCreateWithDifferentRealmPrincipalInACL(){
		when(userGroupDAO.getUsersRealms(anyList())).thenReturn(Map.of("1", Set.of("1"), "0", Set.of("123")));

		// call under test
		String message = assertThrows(InvalidModelException.class, ()-> {
			aclManager.create(userInfo, acl, ObjectType.ENTITY , userInfo.getId());
		}).getMessage();
		assertEquals("All principals in the ACL must be from the same realm.", message);
		verifyNoMoreInteractions(aclDao);
	}

	@Test
	public void testCreateWithDifferentRealmUser(){
		when(userGroupDAO.getUsersRealms(anyList())).thenReturn(Map.of("0", Set.of("1", "123")));

		// call under test
		userInfo = new UserInfo(false, 123L, "1");
		String message = assertThrows(InvalidModelException.class, () -> {
			aclManager.create(userInfo, acl, ObjectType.ENTITY, userInfo.getId());
		}).getMessage();

		assertEquals("All principals in the ACL must be from the same realm as the caller principal.", message);
		verifyNoMoreInteractions(aclDao);

		//admin is also not allowed to change other realm acl
		adminUser = new UserInfo(true, 456L, "1");
		userInfo = new UserInfo(false, 123L, AuthorizationConstants.DEFAULT_REALM_ID);

		String messageTwo = assertThrows(InvalidModelException.class, () -> {
			aclManager.create(adminUser, acl, ObjectType.ENTITY, userInfo.getId());
		}).getMessage();

		assertEquals("All principals in the ACL must be from the same realm as the caller principal.", messageTwo);
	}

	@Test
	public void testCreateWithInvalidPrincipalIdsInACL() {
		when(userGroupDAO.getUsersRealms(anyList())).thenReturn(Map.of("0", Set.of("55", "66")));
		when(userGroupDAO.getUsersRealms(anyList())).thenReturn(Collections.emptyMap());

		// call under test
		aclManager.create(userInfo, acl, ObjectType.ENTITY, userInfo.getId());
	}

	@Test
	public void testUpdate(){
		AccessControlList acl = new AccessControlList().setId("222").setCreationDate(new Date());
		acl.setResourceAccess(Set.of(new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.READ)),
				new ResourceAccess().setPrincipalId(2L).setAccessType(Set.of(ACCESS_TYPE.READ)) ));
		when(userGroupDAO.getUsersRealms(anyList())).thenReturn(Map.of("0", Set.of("1", "2")));

		// call under test
		aclManager.update(userInfo, acl, ObjectType.ENTITY, userInfo.getId());
		verify(aclDao, times(1)).update(acl, ObjectType.ENTITY);
	}

	@Test
	public void testCanAccess(){
		String objectId = "123";
		when(aclDao.canAccess(userInfo, objectId, ObjectType.EVALUATION, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		AuthorizationStatus canAccess = aclManager.canAccess(userInfo, objectId, ObjectType.EVALUATION, ACCESS_TYPE.READ);
		assertNotNull(canAccess);
		verify(aclDao, times(1)).canAccess(userInfo, objectId, ObjectType.EVALUATION, ACCESS_TYPE.READ);
	}

	@Test
	public void testGetAcl(){
		String objectId = "123";
		AccessControlList expected = new AccessControlList().setId(objectId);
		when(aclDao.getAcl(objectId, ObjectType.ENTITY)).thenReturn(Optional.ofNullable(expected));
		Optional<AccessControlList> result = aclManager.getAcl(objectId, ObjectType.ENTITY);
		assertTrue(result.isPresent());
		assertEquals(expected, result.get());
		verify(aclDao, times(1)).getAcl(objectId, ObjectType.ENTITY);
	}

	@Test
	public void testGetChildrenEntitiesWithAcls(){
		List<Long> parentIds = List.of(123L, 456L);
		List<Long> expected = List.of(789L, 101L);
		when(aclDao.getChildrenEntitiesWithAcls(parentIds)).thenReturn(expected);
		List<Long> result = aclManager.getChildrenEntitiesWithAcls(parentIds);
		assertEquals(expected, result);
		verify(aclDao, times(1)).getChildrenEntitiesWithAcls(parentIds);
	}

	@Test
	public void testGetNonVisibleChildrenOfEntity(){
		Set<Long> groups = Set.of(123L, 456L);
		String parentId = "789";
		Set<Long> expected = Set.of(101L, 102L);
		when(aclDao.getNonVisibleChilrenOfEntity(groups, parentId)).thenReturn(expected);
		Set<Long> result = aclManager.getNonVisibleChilrenOfEntity(groups, parentId);
		assertEquals(expected, result);
		verify(aclDao, times(1)).getNonVisibleChilrenOfEntity(groups, parentId);
	}

	@Test
	public void testDeleteWithIds(){
		Long id1 = 123L;
		Long id2 = 456L;
		when(aclDao.delete(any(List.class), any(ObjectType.class))).thenReturn(2);
		int count = aclManager.delete(List.of(id1, id2), ObjectType.ENTITY);
		assertEquals(2, count);
		verify(aclDao, times(1)).delete(List.of(id1, id2), ObjectType.ENTITY);
	}

	@Test
	public void testTruncateALL(){
		// call under test
		aclManager.truncateAll();
		verify(aclDao, times(1)).truncateAll();
	}
}
