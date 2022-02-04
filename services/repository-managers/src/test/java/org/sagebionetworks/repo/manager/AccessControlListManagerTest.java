package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;

import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class AccessControlListManagerTest {
	
	@Mock
	private AccessControlListDAO aclDao;
	
	@InjectMocks
	private AccessControlListManagerImpl aclManager;
	
	private UserInfo userInfo;
	private UserInfo adminUser;
	
	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, 123L);
		adminUser = new UserInfo(true, 456L);
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
		verify(aclDao, times(1)).getAccessibleBenefactors(any(Set.class), any(Set.class), any(ObjectType.class), any(ACCESS_TYPE.class));
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
		when(aclDao.getAccessibleBenefactors(any(Set.class), any(Set.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(benefactors);
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
}
