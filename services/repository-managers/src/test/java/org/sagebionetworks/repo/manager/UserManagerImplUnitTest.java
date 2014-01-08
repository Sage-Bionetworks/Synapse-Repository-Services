package org.sagebionetworks.repo.manager;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;

public class UserManagerImplUnitTest {
	
	private UserManager userManager;
	
	private UserGroupDAO mockUserGroupDAO;
	private UserProfileDAO mockUserProfileDAO;
	private GroupMembersDAO mockGroupMembersDAO;
	private AuthenticationDAO mockAuthDAO;
	private DBOBasicDao basicDAO;
	
	private UserInfo admin;
	private UserInfo notAdmin;
	
	private final String mockId = "-1";
	private UserGroup mockUserGroup;
	private UserProfile mockUserProfile;
	
	@Before
	public void setUp() throws Exception {
		mockUserGroupDAO = mock(UserGroupDAO.class);
		mockUserProfileDAO = mock(UserProfileDAO.class);
		mockGroupMembersDAO = mock(GroupMembersDAO.class);
		mockAuthDAO = mock(AuthenticationDAO.class);
		basicDAO = mock(DBOBasicDao.class);

		when(mockUserGroupDAO.create(any(UserGroup.class))).thenReturn(mockId);
		mockUserGroup = new UserGroup();
		mockUserGroup.setId(mockId);
		mockUserGroup.setIsIndividual(true);
		when(mockUserGroupDAO.get(anyString())).thenReturn(mockUserGroup);
		
		mockUserProfile = new UserProfile();
		when(mockUserProfileDAO.get(anyString())).thenReturn(mockUserProfile);
		
		userManager = new UserManagerImpl(mockUserGroupDAO, mockUserProfileDAO, mockGroupMembersDAO, mockAuthDAO, basicDAO);
		
		admin = new UserInfo(true);
		notAdmin = new UserInfo(false);
	}
	
	@Test
	public void testCreateUserAdmin() throws Exception {
		// Call with an admin
		userManager.createUser(admin, null, null, null);
		verify(mockUserGroupDAO).doesPrincipalExist(anyString());
		
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
