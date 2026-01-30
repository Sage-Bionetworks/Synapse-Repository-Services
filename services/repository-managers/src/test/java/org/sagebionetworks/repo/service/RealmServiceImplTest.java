package org.sagebionetworks.repo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.RealmManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.Realm;
import org.sagebionetworks.repo.model.auth.RealmIdList;
import org.sagebionetworks.repo.model.auth.RealmPrincipal;

@ExtendWith(MockitoExtension.class)
class RealmServiceImplTest {
	
	@Mock
	private RealmManager mockRealmManager;
	
	@Mock
	private UserManager mockUserManager;
	
	@InjectMocks
	private RealmServiceImpl realmService;
	
	private static final String REALM_ID = "1";
	
	private static final Long USER_ID = 101L;
	
	@Test
	public void testListRealms() {
		String realmId1 = "1";
		String realmId2 = "2";
		RealmIdList realmIdList = new RealmIdList();
		realmIdList.setRealms(List.of(realmId1, realmId2));
		when(mockRealmManager.listRealmIds()).thenReturn(realmIdList);
		// method under test
		RealmIdList result = realmService.listRealmIds();
		assertEquals(realmIdList, result);
		verify(mockRealmManager).listRealmIds();
	}
	
	@Test
	public void testGetRealm() {
		Realm expected = new Realm();
		expected.setId(REALM_ID);
		when (mockRealmManager.getRealm(REALM_ID)).thenReturn(expected);
		
		// method under test
		Realm actual = realmService.getRealm(REALM_ID);
		
		assertEquals(expected, actual);
		verify(mockRealmManager).getRealm(REALM_ID);
	}

	@Test
	public void testGetRealmPrincipalsGivenId() {
		RealmPrincipal expected = new RealmPrincipal();
		expected.setRealmId(REALM_ID);
		when (mockRealmManager.getRealmPrincipals(REALM_ID)).thenReturn(expected);
		
		// method under test
		RealmPrincipal actual = realmService.getRealmPrincipals(REALM_ID);
		
		assertEquals(expected, actual);
		verify(mockRealmManager).getRealmPrincipals(REALM_ID);
	}

	@Test
	public void testGetRealmPrincipals() {
		RealmPrincipal expected = new RealmPrincipal();
		expected.setRealmId(REALM_ID);
		when (mockRealmManager.getRealmPrincipals(REALM_ID)).thenReturn(expected);
		UserInfo userInfo = new UserInfo(false, USER_ID, REALM_ID);
		when(mockUserManager.getUserInfo(USER_ID)).thenReturn(userInfo);
		
		// method under test
		RealmPrincipal actual = realmService.getRealmPrincipals(USER_ID);
		
		assertEquals(expected, actual);
		verify(mockRealmManager).getRealmPrincipals(REALM_ID);
	}

}
