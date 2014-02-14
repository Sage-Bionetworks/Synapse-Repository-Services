package org.sagebionetworks.repo.manager.team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
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
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.MembershipRqstSubmissionDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;

public class MembershipRequestManagerImplTest {
	
	private AuthorizationManager mockAuthorizationManager = null;
	private MembershipRequestManagerImpl membershipRequestManagerImpl = null;
	private MembershipRqstSubmissionDAO mockMembershipRqstSubmissionDAO = null;
	
	private UserInfo userInfo = null;
	private UserInfo adminInfo = null;
	private static final String MEMBER_PRINCIPAL_ID = "999";

	@Before
	public void setUp() throws Exception {
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		mockMembershipRqstSubmissionDAO = Mockito.mock(MembershipRqstSubmissionDAO.class);
		membershipRequestManagerImpl = new MembershipRequestManagerImpl(
				mockAuthorizationManager,
				mockMembershipRqstSubmissionDAO
				);
		userInfo = new UserInfo(false);
		userInfo.setId(Long.parseLong(MEMBER_PRINCIPAL_ID));
		// admin
		adminInfo = new UserInfo(true);
		adminInfo.setId(-1l);
	}
	
	private void validateForCreateExpectFailure(MembershipRqstSubmission mrs, UserInfo userInfo) {
		try {
			MembershipRequestManagerImpl.validateForCreate(mrs, userInfo);
			fail("InvalidModelException expected");
		} catch (InvalidModelException e) {
			// as expected
		}		
	}

	@Test
	public void testValidateForCreate() throws Exception {
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		
		// Happy case
		mrs.setTeamId("101");
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		MembershipRequestManagerImpl.validateForCreate(mrs, userInfo);
		
		// try to request for someone else, as an admin
		mrs.setTeamId("101");
		mrs.setUserId("102");
		MembershipRequestManagerImpl.validateForCreate(mrs, adminInfo);
		
		// try to request for someone else
		mrs.setTeamId("101");
		mrs.setUserId("102");
		validateForCreateExpectFailure(mrs, userInfo);

		// can't set createdBy
		mrs.setTeamId("101");
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		mrs.setCreatedBy("me");
		validateForCreateExpectFailure(mrs, userInfo);

		// can't set createdOn
		mrs.setTeamId("101");
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		mrs.setCreatedBy(null);
		mrs.setCreatedOn(new Date());
		validateForCreateExpectFailure(mrs, userInfo);

		// must set Team
		mrs.setTeamId(null);
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		mrs.setCreatedBy(null);
		mrs.setCreatedOn(null);
		validateForCreateExpectFailure(mrs, userInfo);

		// can't set id
		mrs.setTeamId("101");
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		mrs.setCreatedBy(null);
		mrs.setCreatedOn(null);
		mrs.setId("007");
		validateForCreateExpectFailure(mrs, userInfo);

	}
	
	@Test
	public void testPopulateCreationFields() throws Exception {
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		Date now = new Date();
		MembershipRequestManagerImpl.populateCreationFields(userInfo, mrs, now);
		assertEquals(MEMBER_PRINCIPAL_ID, mrs.getCreatedBy());
		assertEquals(now, mrs.getCreatedOn());
		assertEquals(MEMBER_PRINCIPAL_ID, mrs.getUserId());
		
		// don't override userId if already set
		mrs.setUserId("something else");
		MembershipRequestManagerImpl.populateCreationFields(userInfo, mrs, now);
		assertEquals("something else", mrs.getUserId());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testAnonymousCreate() throws Exception {
		UserInfo anonymousInfo = new UserInfo(false);
		anonymousInfo.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		membershipRequestManagerImpl.create(anonymousInfo, mrs);
	}
	
	@Test
	public void testCreate() throws Exception {
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setTeamId("111");
		when(mockMembershipRqstSubmissionDAO.create((MembershipRqstSubmission)any())).thenReturn(mrs);
		assertEquals(mrs, membershipRequestManagerImpl.create(userInfo, mrs));
	}
	
	@Test
	public void testGet() throws Exception {
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setTeamId("111");
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		when(mockMembershipRqstSubmissionDAO.get(anyString())).thenReturn(mrs);
		assertEquals(mrs, membershipRequestManagerImpl.get(userInfo, "001"));
		
		// ok to get for another user, if you are an admin
		mrs.setUserId("-1");
		assertEquals(mrs, membershipRequestManagerImpl.get(adminInfo, "001"));
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetForAnotherUser() throws Exception {
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setTeamId("111");
		mrs.setUserId("-1");
		when(mockMembershipRqstSubmissionDAO.get(anyString())).thenReturn(mrs);
		assertEquals(mrs, membershipRequestManagerImpl.get(userInfo, "001"));
	}

	@Test
	public void testDelete() throws Exception {
		String MRS_ID = "222";
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setTeamId("111");
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		mrs.setId(MRS_ID);
		when(mockMembershipRqstSubmissionDAO.get(MRS_ID)).thenReturn(mrs);
		membershipRequestManagerImpl.delete(userInfo, MRS_ID);
		Mockito.verify(mockMembershipRqstSubmissionDAO).delete(MRS_ID);
		
		// ok to delete if you are an admin
		mrs.setUserId("333");
		membershipRequestManagerImpl.delete(adminInfo, MRS_ID);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testDeleteOther() throws Exception {
		String MRS_ID = "222";
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setTeamId("111");
		mrs.setUserId("333");
		mrs.setId(MRS_ID);
		when(mockMembershipRqstSubmissionDAO.get(MRS_ID)).thenReturn(mrs);
		membershipRequestManagerImpl.delete(userInfo, MRS_ID);
		Mockito.verify(mockMembershipRqstSubmissionDAO).delete(MRS_ID);
	}

	@Test
	public void testGetOpenByTeam() throws Exception {
		MembershipRequest mr = new MembershipRequest();
		mr.setTeamId("111");
		mr.setUserId("333");
		long teamId = 101L;
		List<MembershipRequest> expected = Arrays.asList(new MembershipRequest[]{mr});
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamInRange(eq(teamId), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamCount(eq(teamId), anyLong())).thenReturn((long)expected.size());
		when(mockAuthorizationManager.canAccess(userInfo, ""+teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		PaginatedResults<MembershipRequest> actual = membershipRequestManagerImpl.getOpenByTeamInRange(userInfo, ""+teamId,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testGetOpenByTeamUnauthorized() throws Exception {
		long teamId = 101L;
		when(mockAuthorizationManager.canAccess(userInfo, ""+teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		membershipRequestManagerImpl.getOpenByTeamInRange(userInfo, ""+teamId,1,0);
	}
	
	@Test
	public void testGetOpenByTeamAndRequester() throws Exception {
		MembershipRequest mr = new MembershipRequest();
		mr.setTeamId("111");
		long userId = 333L;
		mr.setUserId(""+userId);
		long teamId = 101L;
		List<MembershipRequest> expected = Arrays.asList(new MembershipRequest[]{mr});
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamAndRequesterInRange(eq(teamId), eq(userId), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(eq(teamId), eq(userId), anyLong())).thenReturn((long)expected.size());
		when(mockAuthorizationManager.canAccess(userInfo, ""+teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		PaginatedResults<MembershipRequest> actual = membershipRequestManagerImpl.getOpenByTeamAndRequesterInRange(userInfo, ""+teamId,""+userId,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetOpenByTeamAndRequesterUnauthorized() throws Exception {
		long userId = 333L;
		long teamId = 101L;
		when(mockAuthorizationManager.canAccess(userInfo, ""+teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		membershipRequestManagerImpl.getOpenByTeamAndRequesterInRange(userInfo, ""+teamId,""+userId,1,0);
	}

	@Test
	public void testGetOpenSubmissionsByRequester() throws Exception {
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setTeamId("111");
		long userId = userInfo.getId();
		mrs.setUserId(""+userId);
		List<MembershipRqstSubmission> expected = Arrays.asList(new MembershipRqstSubmission[]{mrs});
		when(mockMembershipRqstSubmissionDAO.getOpenSubmissionsByRequesterInRange(eq(userId), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipRqstSubmissionDAO.getOpenByRequesterCount(eq(userId), anyLong())).thenReturn((long)expected.size());
		PaginatedResults<MembershipRqstSubmission> actual = membershipRequestManagerImpl.getOpenSubmissionsByRequesterInRange(userInfo, ""+userId,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testGetOpenSubmissionsByRequesterUnauthorized() throws Exception {
		long userId = userInfo.getId();
		membershipRequestManagerImpl.getOpenSubmissionsByRequesterInRange(userInfo, ""+(userId+999),1,0);
	}
	
	@Test
	public void testGetOpenSubmissionsByRequesterAndTeam() throws Exception {
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		long teamId = 111L;
		mrs.setTeamId(""+teamId);
		long userId = userInfo.getId();
		mrs.setUserId(""+userId);
		List<MembershipRqstSubmission> expected = Arrays.asList(new MembershipRqstSubmission[]{mrs});
		when(mockMembershipRqstSubmissionDAO.getOpenSubmissionsByTeamAndRequesterInRange(eq(teamId), eq(userId), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(eq(teamId), eq(userId), anyLong())).thenReturn((long)expected.size());
		PaginatedResults<MembershipRqstSubmission> actual = membershipRequestManagerImpl.getOpenSubmissionsByTeamAndRequesterInRange(userInfo, ""+teamId, ""+userId,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetOpenSubmissionsByRequesterAndTeamUnauthorized() throws Exception {
		long teamId = 111L;
		long userId = userInfo.getId();
		membershipRequestManagerImpl.getOpenSubmissionsByTeamAndRequesterInRange(userInfo, ""+teamId, ""+(userId+999),1,0);

	}

}
