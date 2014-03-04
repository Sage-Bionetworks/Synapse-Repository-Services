package org.sagebionetworks.repo.manager.team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.MembershipInvtnSubmissionDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;

public class MembershipInvitationManagerImplTest {
	
	private MembershipInvitationManagerImpl membershipInvitationManagerImpl = null;
	private AuthorizationManager mockAuthorizationManager = null;
	private MembershipInvtnSubmissionDAO mockMembershipInvtnSubmissionDAO = null;
	
	private UserInfo userInfo = null;
	private UserInfo adminInfo = null;
	private static final String MEMBER_PRINCIPAL_ID = "999";

	private static final String TEAM_ID = "123";
	private static final String MIS_ID = "987";
	
	private static MembershipInvtnSubmission createMembershipInvtnSubmission(String id) {
		MembershipInvtnSubmission mis = new MembershipInvtnSubmission();
		mis.setId(id);
		mis.setTeamId(TEAM_ID);
		mis.setInviteeId(MEMBER_PRINCIPAL_ID);
		return mis;
	}
	
	private static MembershipInvitation createMembershipInvitation() {
		MembershipInvitation mi = new MembershipInvitation();
		mi.setTeamId(TEAM_ID);
		mi.setUserId(MEMBER_PRINCIPAL_ID);
		return mi;
	}
	
	@Before
	public void setUp() throws Exception {
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		mockMembershipInvtnSubmissionDAO = Mockito.mock(MembershipInvtnSubmissionDAO.class);
		membershipInvitationManagerImpl = new MembershipInvitationManagerImpl(
				mockAuthorizationManager,
				mockMembershipInvtnSubmissionDAO
				);
		userInfo = new UserInfo(false, MEMBER_PRINCIPAL_ID);
		adminInfo = new UserInfo(true, -1l);
	}

	
	private void validateForCreateExpectFailure(MembershipInvtnSubmission mis) {
		try {
			MembershipInvitationManagerImpl.validateForCreate(mis);
			fail("InvalidModelException expected");
		} catch (InvalidModelException e) {
			// as expected
		}		
	}

	@Test
	public void testValidateForCreate() throws Exception {
		MembershipInvtnSubmission mis = new MembershipInvtnSubmission();
		
		// Happy case
		mis.setTeamId("101");
		mis.setInviteeId(MEMBER_PRINCIPAL_ID);
		MembershipInvitationManagerImpl.validateForCreate(mis);

		// can't set createdBy
		mis.setTeamId("101");
		mis.setInviteeId(MEMBER_PRINCIPAL_ID);
		mis.setCreatedBy("me");
		validateForCreateExpectFailure(mis);
		
		
		// must set invitees
		mis.setTeamId("101");
		mis.setInviteeId(null);
		mis.setCreatedBy(null);
		validateForCreateExpectFailure(mis);

		// can't set createdOn
		mis.setTeamId("101");
		mis.setInviteeId(MEMBER_PRINCIPAL_ID);
		mis.setCreatedBy(null);
		mis.setCreatedOn(new Date());
		validateForCreateExpectFailure(mis);

		// must set Team
		mis.setTeamId(null);
		mis.setInviteeId(MEMBER_PRINCIPAL_ID);
		mis.setCreatedBy(null);
		mis.setCreatedOn(null);
		validateForCreateExpectFailure(mis);

		// can't set id
		mis.setTeamId("101");
		mis.setInviteeId(MEMBER_PRINCIPAL_ID);
		mis.setCreatedBy(null);
		mis.setCreatedOn(null);
		mis.setId("007");
		validateForCreateExpectFailure(mis);

	}
	
	@Test
	public void testPopulateCreationFields() throws Exception {
		MembershipInvtnSubmission mis = new MembershipInvtnSubmission();
		Date now = new Date();
		MembershipInvitationManagerImpl.populateCreationFields(userInfo, mis, now);
		assertEquals(MEMBER_PRINCIPAL_ID, mis.getCreatedBy());
		assertEquals(now, mis.getCreatedOn());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testNonAdminCreate() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(null);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		membershipInvitationManagerImpl.create(userInfo, mis);
	}
	
	@Test
	public void testAdminCreate() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(null);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		membershipInvitationManagerImpl.create(userInfo, mis);
		Mockito.verify(mockMembershipInvtnSubmissionDAO).create(mis);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testNonAdminGet() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		when(mockMembershipInvtnSubmissionDAO.get(MIS_ID)).thenReturn(mis);
		membershipInvitationManagerImpl.get(userInfo, MIS_ID);
	}
	
	@Test
	public void testAdminGet() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		when(mockMembershipInvtnSubmissionDAO.get(MIS_ID)).thenReturn(mis);
		assertEquals(mis, membershipInvitationManagerImpl.get(userInfo, MIS_ID));
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testNonAdminDelete() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		when(mockMembershipInvtnSubmissionDAO.get(MIS_ID)).thenReturn(mis);
		membershipInvitationManagerImpl.delete(userInfo, MIS_ID);
	}
	
	@Test
	public void testAdminDelete() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		when(mockMembershipInvtnSubmissionDAO.get(MIS_ID)).thenReturn(mis);
		membershipInvitationManagerImpl.delete(userInfo, MIS_ID);
		Mockito.verify(mockMembershipInvtnSubmissionDAO).delete(MIS_ID);
	}
	

	@Test
	public void testGetOpenForUserInRange() throws Exception {
		MembershipInvitation mi = createMembershipInvitation();
		List<MembershipInvitation> expected = Arrays.asList(new MembershipInvitation[]{mi});
		when(mockMembershipInvtnSubmissionDAO.getOpenByUserInRange(eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipInvtnSubmissionDAO.getOpenByUserCount(eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn((long)expected.size());
		PaginatedResults<MembershipInvitation> actual = membershipInvitationManagerImpl.getOpenForUserInRange(MEMBER_PRINCIPAL_ID,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}
	
	@Test
	public void testGetOpenForUserAndTeamInRange() throws Exception {
		MembershipInvitation mi = createMembershipInvitation();
		List<MembershipInvitation> expected = Arrays.asList(new MembershipInvitation[]{mi});
		when(mockMembershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn((long)expected.size());
		PaginatedResults<MembershipInvitation> actual = membershipInvitationManagerImpl.getOpenForUserAndTeamInRange(MEMBER_PRINCIPAL_ID, TEAM_ID,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}

	@Test
	public void testGetOpenSubmissionsForTeamInRange() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		List<MembershipInvtnSubmission> expected = Arrays.asList(new MembershipInvtnSubmission[]{mis});
		when(mockMembershipInvtnSubmissionDAO.getOpenSubmissionsByTeamInRange(eq(Long.parseLong(TEAM_ID)), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipInvtnSubmissionDAO.getOpenByTeamCount(eq(Long.parseLong(TEAM_ID)), anyLong())).thenReturn((long)expected.size());
		PaginatedResults<MembershipInvtnSubmission> actual = membershipInvitationManagerImpl.getOpenSubmissionsForTeamInRange(userInfo, TEAM_ID,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testGetOpenSubmissionsForTeamInRangeUnauthorized() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		membershipInvitationManagerImpl.getOpenSubmissionsForTeamInRange(userInfo, TEAM_ID,1,0);
	}
	
	@Test
	public void testGetOpenSubmissionsForTeamAndRequesterInRange() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		List<MembershipInvtnSubmission> expected = Arrays.asList(new MembershipInvtnSubmission[]{mis});
		when(mockMembershipInvtnSubmissionDAO.getOpenSubmissionsByTeamAndUserInRange(eq(Long.parseLong(TEAM_ID)), anyLong(), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipInvtnSubmissionDAO.getOpenByTeamCount(eq(Long.parseLong(TEAM_ID)), anyLong())).thenReturn((long)expected.size());
		PaginatedResults<MembershipInvtnSubmission> actual = membershipInvitationManagerImpl.
				getOpenSubmissionsForUserAndTeamInRange(userInfo, MEMBER_PRINCIPAL_ID, TEAM_ID,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetOpenSubmissionsForTeamAndRequesterInRangeUnauthorized() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		membershipInvitationManagerImpl.getOpenSubmissionsForUserAndTeamInRange(userInfo, MEMBER_PRINCIPAL_ID, TEAM_ID,1,0);
	}

}
