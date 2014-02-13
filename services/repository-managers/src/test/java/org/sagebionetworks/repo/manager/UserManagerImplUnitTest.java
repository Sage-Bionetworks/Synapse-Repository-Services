package org.sagebionetworks.repo.manager;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;

public class UserManagerImplUnitTest {
	
	private UserManager userManager;
	
	private UserGroupDAO mockUserGroupDAO;
	private UserProfileManager mockUserProfileManger;
	private GroupMembersDAO mockGroupMembersDAO;
	private AuthenticationDAO mockAuthDAO;
	private DBOBasicDao basicDAO;
	private PrincipalAliasDAO mockPrincipalAliasDAO;
	
	private UserInfo admin;
	private UserInfo notAdmin;
	
	private final String mockId = "-1";
	private UserGroup mockUserGroup;
	private UserProfile mockUserProfile;
	
	@Before
	public void setUp() throws Exception {
		mockUserGroupDAO = mock(UserGroupDAO.class);
		mockUserProfileManger = mock(UserProfileManager.class);
		mockGroupMembersDAO = mock(GroupMembersDAO.class);
		mockAuthDAO = mock(AuthenticationDAO.class);
		basicDAO = mock(DBOBasicDao.class);
		mockPrincipalAliasDAO = mock(PrincipalAliasDAO.class);

		when(mockUserGroupDAO.create(any(UserGroup.class))).thenReturn(Long.parseLong(mockId));
		mockUserGroup = new UserGroup();
		mockUserGroup.setId(mockId);
		mockUserGroup.setIsIndividual(true);
		when(mockUserGroupDAO.get(anyLong())).thenReturn(mockUserGroup);
		
		mockUserProfile = new UserProfile();
		when(mockUserProfileManger.getUserProfile(any(UserInfo.class), anyString())).thenReturn(mockUserProfile);
		
		userManager = new UserManagerImpl(mockUserGroupDAO, mockUserProfileManger, mockGroupMembersDAO, mockAuthDAO, basicDAO, mockPrincipalAliasDAO);
		
		admin = new UserInfo(true);
		notAdmin = new UserInfo(false);
	}
	
	@Test
	public void testCreateUserAdmin() throws Exception {
		// Call with an admin
		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString()+"@testing.com");
		nu.setUserName(UUID.randomUUID().toString());
		userManager.createUser(admin, nu, null, null);
		
		// Call with a non admin
		try {
			userManager.createUser(notAdmin, null, null, null);
			fail();
		} catch (UnauthorizedException e) { }
	}
	
	@Test
	public void testDeleteUserAdmin() throws Exception {
		// Call with an admin
		userManager.deletePrincipal(admin, Long.parseLong(mockId));
		verify(mockUserGroupDAO).delete(anyString());
		
		// Call with a non admin
		try {
			userManager.deletePrincipal(notAdmin, Long.parseLong(mockId));
			fail();
		} catch (UnauthorizedException e) { }
	}
}
