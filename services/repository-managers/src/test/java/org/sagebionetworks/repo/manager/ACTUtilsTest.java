package org.sagebionetworks.repo.manager;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_AND_COMPLIANCE_TEAM_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ForbiddenException;

public class ACTUtilsTest {
	
	private static UserGroup actTeam = null;
	private static UserGroupDAO userGroupDAO = null;
	private static AuthorizationManager authorizationManager = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		actTeam = new UserGroup();
		actTeam.setId("101");
		actTeam.setIsIndividual(false);
		actTeam.setName(ACCESS_AND_COMPLIANCE_TEAM_NAME);
		userGroupDAO = Mockito.mock(UserGroupDAO.class);
		when(userGroupDAO.findGroup(ACCESS_AND_COMPLIANCE_TEAM_NAME, false)).thenReturn(actTeam);
		authorizationManager = Mockito.mock(AuthorizationManager.class);
		when(authorizationManager.canAccess((UserInfo)any(), anyString(), (ACCESS_TYPE)any())).thenReturn(true);
	}

	@Test
	public void testVerifyACTTeamMembershipOrIsAdmin_Admin() {
		UserInfo adminInfo = new UserInfo(true);
		ACTUtils.verifyACTTeamMembershipOrIsAdmin(adminInfo, userGroupDAO);
	}

	@Test
	public void testVerifyACTTeamMembershipOrIsAdmin_ACT() {
		UserInfo userInfo = new UserInfo(false);
		Set<UserGroup> ugs = new HashSet<UserGroup>();
		ugs.add(actTeam);
		userInfo.setGroups(ugs);
		ACTUtils.verifyACTTeamMembershipOrIsAdmin(userInfo, userGroupDAO);
	}

	@Test(expected=UnauthorizedException.class)
	public void testVerifyACTTeamMembershipOrIsAdmin_NONE() {
		UserInfo userInfo = new UserInfo(false);
		Set<UserGroup> ugs = new HashSet<UserGroup>();
		userInfo.setGroups(ugs);
		ACTUtils.verifyACTTeamMembershipOrIsAdmin(userInfo, userGroupDAO);
	}

	@Test
	public void testVerifyACTTeamMembershipOrCanCreateOrEdit_isAdmin() throws Exception {
		UserInfo adminInfo = new UserInfo(true);
		Set<UserGroup> ugs = new HashSet<UserGroup>();
		adminInfo.setGroups(ugs);
		List<String> ids = new ArrayList<String>(Arrays.asList(new String[]{"101"}));
		ACTUtils.verifyACTTeamMembershipOrCanCreateOrEdit(adminInfo, ids, userGroupDAO, authorizationManager);
	}

	@Test
	public void testVerifyACTTeamMembershipOrCanCreateOrEdit_ACT() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		Set<UserGroup> ugs = new HashSet<UserGroup>();
		ugs.add(actTeam);
		userInfo.setGroups(ugs);
		List<String> ids = new ArrayList<String>(Arrays.asList(new String[]{"101"}));
		ACTUtils.verifyACTTeamMembershipOrCanCreateOrEdit(userInfo, ids, userGroupDAO, authorizationManager);
	}

	@Test(expected=UnauthorizedException.class)
	public void testVerifyACTTeamMembershipOrCanCreateOrEdit_multiple() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		Set<UserGroup> ugs = new HashSet<UserGroup>();
		userInfo.setGroups(ugs);
		List<String> ids = new ArrayList<String>(Arrays.asList(new String[]{"101", "102"}));
		ACTUtils.verifyACTTeamMembershipOrCanCreateOrEdit(userInfo, ids, userGroupDAO, authorizationManager);
	}

	@Test
	public void testVerifyACTTeamMembershipOrCanCreateOrEdit_editAccess() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		Set<UserGroup> ugs = new HashSet<UserGroup>();
		userInfo.setGroups(ugs);
		List<String> ids = new ArrayList<String>(Arrays.asList(new String[]{"101"}));
		ACTUtils.verifyACTTeamMembershipOrCanCreateOrEdit(userInfo, ids, userGroupDAO, authorizationManager);
	}

	@Test(expected=UnauthorizedException.class)
	public void testVerifyACTTeamMembershipOrCanCreateOrEdit_none() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		Set<UserGroup> ugs = new HashSet<UserGroup>();
		userInfo.setGroups(ugs);
		List<String> ids = new ArrayList<String>(Arrays.asList(new String[]{"101"}));
		AuthorizationManager authorizationManager = Mockito.mock(AuthorizationManager.class);
		when(authorizationManager.canAccess((UserInfo)any(), anyString(), (ACCESS_TYPE)any())).thenReturn(false);
		ACTUtils.verifyACTTeamMembershipOrCanCreateOrEdit(userInfo, ids, userGroupDAO, authorizationManager);
	}

}
