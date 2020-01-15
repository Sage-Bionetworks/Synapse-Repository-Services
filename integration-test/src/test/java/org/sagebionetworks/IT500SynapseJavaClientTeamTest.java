package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InviteeVerificationSignedToken;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.ResponseMessage;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMemberTypeFilterOptions;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.model.util.ModelConstants;
import org.sagebionetworks.util.SerializationUtils;

import com.google.common.collect.Sets;

/**
 * Integration tests for team services
 * 
 */
public class IT500SynapseJavaClientTeamTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static SynapseClient synapseTwo;
	private static Long user1ToDelete;
	private static Long user2ToDelete;
	
	private static final int RDS_WORKER_TIMEOUT = 1000*60; // One min
	
	private static final String MOCK_ACCEPT_INVITATION_ENDPOINT = "https://www.synapse.org/#invit:";
	private static final String MOCK_ACCEPT_MEMB_RQST_ENDPOINT = "https://www.synapse.org/#request:";
	private static final String MOCK_TEAM_ENDPOINT = "https://www.synapse.org/#Team:";
	private static final String MOCK_NOTIFICATION_UNSUB_ENDPOINT = "https://www.synapse.org/#unsub:";
	private static final String TEST_EMAIL = "test" + UUID.randomUUID() +"@test.com";

	private List<String> toDelete;
	private List<Long> accessRequirementsToDelete;
	private List<String> handlesToDelete;
	private List<String> teamsToDelete;
	private Project project;
	private Folder dataset;
	
	private static Set<String> bootstrappedTeams = Sets.newHashSet();
	static {
		// note, this must match the bootstrapped teams defined in managers-spb.xml
		bootstrappedTeams.add("2"); // Administrators
		bootstrappedTeams.add("464532"); // Access and Compliance Team
		bootstrappedTeams.add("4"); // Trusted message senders
	}
	
	private long getBootstrapCountPlus(long number) {
		return bootstrappedTeams.size() + number;
	}
	
	private Team getTestTeamFromResults(PaginatedResults<Team> results) {
		for (Team team : results.getResults()) {
			if (!bootstrappedTeams.contains(team.getId())) {
				return team;
			}
		}
		return null;
	}
	
	@BeforeAll
	public static void beforeClass() throws Exception {
		// Create 2 users
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseOne);
		user1ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseOne);
		
		synapseTwo = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseTwo);
		user2ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseTwo, UUID.randomUUID().toString(), "password"+UUID.randomUUID(), TEST_EMAIL);
	}
	
	@BeforeEach
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
		toDelete = new ArrayList<>();
		accessRequirementsToDelete = new ArrayList<>();
		handlesToDelete = new ArrayList<>();
		teamsToDelete = new ArrayList<>();
		
		project = synapseOne.createEntity(new Project());
		dataset = new Folder();
		dataset.setParentId(project.getId());
		dataset = synapseOne.createEntity(dataset);
		
		toDelete.add(project.getId());
		toDelete.add(dataset.getId());

		// The only teams we leave in the system are the bootstrap teams. The method
		// getBootstrapTeamsPlus(num) returns the right count for assertions.
		long numTeams = 0L;
		do {
			PaginatedResults<Team> teams = synapseOne.getTeams(null, 10, 0);
			numTeams = teams.getTotalNumberOfResults();
			for (Team team : teams.getResults()) {
				if (!bootstrappedTeams.contains(team.getId())) {
					adminSynapse.deleteTeam(team.getId());
				}
			}
		} while (numTeams > getBootstrapCountPlus(0));
	}
	
	@AfterEach
	public void after() throws Exception {
		for (String id: toDelete) {
			try {
				adminSynapse.deleteEntityById(id);
			} catch (SynapseNotFoundException e) {}
		}

		for (Long id : accessRequirementsToDelete) {
			try {
				adminSynapse.deleteAccessRequirement(id);
			} catch (SynapseNotFoundException e) {}
		}

		for (String id : handlesToDelete) {
			try {
				adminSynapse.deleteFileHandle(id);
			} catch (SynapseNotFoundException e) {
			} catch (SynapseException e) { }
		}
		
		for (String id : teamsToDelete) {
			try {
				adminSynapse.deleteTeam(id);
			} catch (SynapseNotFoundException e) {}
		}
	}
	
	@AfterAll
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(user1ToDelete);
		} catch (SynapseException e) { }
		try {
			adminSynapse.deleteUser(user2ToDelete);
		} catch (SynapseException e) { }
	}

	private Team createTeam(String name, String description) throws SynapseException {
		Team team = new Team();
		team.setName(name);
		team.setDescription(description);
		Team createdTeam = synapseOne.createTeam(team);
		teamsToDelete.add(createdTeam.getId());
		return createdTeam;
	}

	@Test
	public void testCRUDTeam() throws SynapseException {
		String name = "Test-Team-Name";
		String description = "Test-Team-Description";

		Team createdTeam = createTeam(name, description);

		UserProfile myProfile = synapseOne.getMyProfile();
		String myPrincipalId = myProfile.getOwnerId();
		assertNotNull(myPrincipalId);
		assertNotNull(createdTeam.getId());
		assertEquals(name, createdTeam.getName());
		assertEquals(description, createdTeam.getDescription());
		assertNotNull(createdTeam.getCreatedOn());
		assertNotNull(createdTeam.getModifiedOn());
		assertEquals(myPrincipalId, createdTeam.getCreatedBy());
		assertEquals(myPrincipalId, createdTeam.getModifiedBy());
		assertNotNull(createdTeam.getEtag());
		assertNull(createdTeam.getIcon());
		// get the Team
		Team retrievedTeam = synapseOne.getTeam(createdTeam.getId());
		assertEquals(createdTeam, retrievedTeam);

		String newTeamName = "A new team name";
		retrievedTeam.setName(newTeamName);
		Team updatedTeam = synapseOne.updateTeam(retrievedTeam);

		assertNotEquals(updatedTeam.getEtag(), retrievedTeam.getEtag());
		assertEquals(updatedTeam.getName(), retrievedTeam.getName());

		// delete Team
		synapseOne.deleteTeam(updatedTeam.getId());
		// delete the Team again (should be idempotent)
		synapseOne.deleteTeam(updatedTeam.getId());
		// retrieve the Team (should get a 404)
		try {
			synapseOne.getTeam(updatedTeam.getId());
			fail("Failed to delete Team "+updatedTeam.getId());
		} catch (SynapseException e) {
			// as expected
		}
	}

	@Test
	public void testGetTeamIcon() throws SynapseException, IOException {
		Team createdTeam = createTeam("TeamName", "Team Desc");
		// upload an icon and get the file handle
		// before setting the icon
		try {
			synapseOne.getTeamIcon(createdTeam.getId());
			fail("Expected: Not Found");
		} catch (SynapseException e) {
			// expected
		}

		PrintWriter pw = null;
		File file = File.createTempFile("testIcon", null);
		try {
			FileOutputStream fos = new FileOutputStream(file);
			pw = new PrintWriter(fos);
			pw.println("test");
			pw.close();
			pw = null;
		} finally {
			if (pw != null) pw.close();
		}
		FileHandle fileHandle = synapseOne.multipartUpload(file, null, true, false);
		handlesToDelete.add(fileHandle.getId());

		// update the Team with the icon
		createdTeam.setIcon(fileHandle.getId());
		Team updatedTeam = synapseOne.updateTeam(createdTeam);
		// get the icon url
		URL url = synapseOne.getTeamIcon(updatedTeam.getId());
		assertNotNull(url);
		// check that we can download the Team icon to a file
		File target = File.createTempFile("temp", null);
		synapseOne.downloadTeamIcon(updatedTeam.getId(), target);
		assertTrue(target.length() > 0);

	}

	@Test
	public void testGetTeams() throws SynapseException, InterruptedException {
		String teamName = "team name";
		Team team = createTeam(teamName, "Team desc");

		// query for all teams
		PaginatedResults<Team> teams = waitForTeams(null, 50, 0);
		assertEquals(getBootstrapCountPlus(1L), teams.getTotalNumberOfResults());
		assertEquals(team, getTestTeamFromResults(teams));
		// make sure pagination works
		teams = waitForTeams(null, 10, 1);
		assertEquals(getBootstrapCountPlus(0L), teams.getResults().size());

		// query for all teams, based on name fragment
		// need to update cache.  the service to trigger an update
		// requires admin privileges, so we log in as an admin:
		teams = waitForTeams(teamName.substring(0, 3), 1, 0);
		assertEquals(2L, teams.getTotalNumberOfResults());
		assertEquals(team, getTestTeamFromResults(teams));
		// again, make sure pagination works
		teams = waitForTeams(teamName.substring(0, 3), 10, 1);
		assertEquals(0L, teams.getResults().size());

		List<Team> teamList = synapseOne.listTeams(Collections.singletonList(Long.parseLong(team.getId())));
		assertEquals(1L, teamList.size());
		assertEquals(team, teamList.get(0));
	}

	@Test
	public void testGetTeamMembers() throws SynapseException, InterruptedException {
		String name = "Test-Team-Name";
		String description = "Test-Team-Description";
		Team team = createTeam(name, description);
		UserProfile myProfile = synapseOne.getMyProfile();
		String myPrincipalId = myProfile.getOwnerId();

		// query for team members.  should get just the creator
		PaginatedResults<TeamMember> members = synapseOne.getTeamMembers(team.getId(), null, TeamMemberTypeFilterOptions.ALL, 1, 0);
		TeamMember tm = members.getResults().get(0);
		assertEquals(myPrincipalId, tm.getMember().getOwnerId());
		assertEquals(team.getId(), tm.getTeamId());
		assertTrue(tm.getIsAdmin());

		// check that if we get admin team members, we get the creator
		PaginatedResults<TeamMember> adminMembers = synapseOne.getTeamMembers(team.getId(), null, TeamMemberTypeFilterOptions.ADMIN, 1, 0);
		tm = adminMembers.getResults().get(0);
		assertEquals(myPrincipalId, tm.getMember().getOwnerId());
		assertEquals(team.getId(), tm.getTeamId());
		assertTrue(tm.getIsAdmin());

		// check that if we get nonadmin team members, we get no one
		PaginatedResults<TeamMember> nonAdminMembers = synapseOne.getTeamMembers(team.getId(), null, TeamMemberTypeFilterOptions.MEMBER, 1, 0);
		assertEquals(0, nonAdminMembers.getTotalNumberOfResults());

		// check that if we get members with the fragment, we get the creator
		String myDisplayName = myProfile.getUserName();
		PaginatedResults<TeamMember> matchingMembers = waitForTeamMembers(team.getId(), myDisplayName, TeamMemberTypeFilterOptions.ALL, 1, 0);
		tm = matchingMembers.getResults().get(0);
		assertEquals(myPrincipalId, tm.getMember().getOwnerId());
		assertEquals(team.getId(), tm.getTeamId());
		assertTrue(tm.getIsAdmin());

		assertEquals(1L, synapseOne.countTeamMembers(team.getId(), null));

		// while we're at it, check the 'getTeamMember' service
		assertEquals(tm, synapseOne.getTeamMember(team.getId(), myPrincipalId));

		TeamMembershipStatus tms = synapseOne.getTeamMembershipStatus(team.getId(), myPrincipalId);
		assertEquals(team.getId(), tms.getTeamId());
		assertEquals(myPrincipalId, tms.getUserId());
		assertTrue(tms.getIsMember());
		assertFalse(tms.getHasOpenInvitation());
		assertFalse(tms.getHasOpenRequest());
		assertTrue(tms.getCanJoin());
	}

	@Test
	public void testAddRemoveMember() throws SynapseException, InterruptedException {
		Team team = createTeam("A team name", "A team description");
		UserProfile myProfile = synapseOne.getMyProfile();
		String myPrincipalId = myProfile.getOwnerId();

		// add a member to the team
		UserProfile otherUp = synapseTwo.getMyProfile();
		String otherDName = otherUp.getUserName();
		String otherPrincipalId = otherUp.getOwnerId();
		// the other has to ask to be added
		MembershipRequest mrs = new MembershipRequest();
		mrs.setTeamId(team.getId());
		synapseTwo.createMembershipRequest(mrs, MOCK_ACCEPT_MEMB_RQST_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		// check membership status
		TeamMembershipStatus tms = synapseOne.getTeamMembershipStatus(team.getId(), otherPrincipalId);
		assertEquals(team.getId(), tms.getTeamId());
		assertEquals(otherPrincipalId, tms.getUserId());
		assertFalse(tms.getIsMember());
		assertFalse(tms.getHasOpenInvitation());
		assertTrue(tms.getHasOpenRequest());
		assertTrue(tms.getCanJoin());

		// a subtle difference:  if the other user requests the status, 'canJoin' is false
		tms = synapseTwo.getTeamMembershipStatus(team.getId(), otherPrincipalId);
		assertEquals(team.getId(), tms.getTeamId());
		assertEquals(otherPrincipalId, tms.getUserId());
		assertFalse(tms.getIsMember());
		assertFalse(tms.getHasOpenInvitation());
		assertTrue(tms.getHasOpenRequest());
		assertFalse(tms.getCanJoin());

		// Test listTeamMembers
		PaginatedResults<TeamMember> members = waitForTeamMembers(team.getId(), null, TeamMemberTypeFilterOptions.ALL, 10, 0);
		List<TeamMember> teamMembers = synapseOne.listTeamMembers(team.getId(), Collections.singletonList(Long.parseLong(myPrincipalId)));
		assertEquals(members.getResults(), teamMembers);

		// Add the other user to the team
		synapseOne.addTeamMember(team.getId(), otherPrincipalId, MOCK_TEAM_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);

		tms = synapseTwo.getTeamMembershipStatus(team.getId(), otherPrincipalId);
		assertEquals(team.getId(), tms.getTeamId());
		assertEquals(otherPrincipalId, tms.getUserId());
		assertTrue(tms.getIsMember());
		assertFalse(tms.getHasOpenInvitation());
		assertFalse(tms.getHasOpenRequest());
		assertTrue(tms.getCanJoin());

		// query for team members.  should get creator as well as new member back
		members = waitForTeamMembers(team.getId(), null, TeamMemberTypeFilterOptions.ALL, 2, 0);
		assertEquals(2L, members.getResults().size());

		assertEquals(2L, synapseOne.countTeamMembers(team.getId(), null));

		// query for teams based on member's id
		PaginatedResults<Team> teams = synapseOne.getTeamsForUser(otherPrincipalId, 1, 0);
		assertEquals(2L, teams.getTotalNumberOfResults());
		assertEquals(team, teams.getResults().get(0));
		// remove the member from the team
		synapseOne.removeTeamMember(team.getId(), otherPrincipalId);

		// query for teams based on member's id (should get nothing)
		teams = synapseOne.getTeamsForUser(otherPrincipalId, 1, 0);
		assertEquals(0L, teams.getTotalNumberOfResults());
	}

	@Test
	public void testAddRemoveAdminPrivilegesForTeamMember() throws SynapseException, InterruptedException {
		Team team = createTeam("A team name", "A team description");
		// add a member to the team
		UserProfile otherUp = synapseTwo.getMyProfile();
		String otherDName = otherUp.getUserName();
		String otherPrincipalId = otherUp.getOwnerId();
		// the other has to ask to be added
		MembershipRequest mrs = new MembershipRequest();
		mrs.setTeamId(team.getId());
		synapseTwo.createMembershipRequest(mrs, MOCK_ACCEPT_MEMB_RQST_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		synapseOne.addTeamMember(team.getId(), otherPrincipalId, MOCK_TEAM_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);

		// make the other member an admin
		synapseOne.setTeamMemberPermissions(team.getId(), otherPrincipalId, true);

		PaginatedResults<TeamMember> members = waitForTeamMembers(team.getId(), otherDName.substring(0,otherDName.length()-4), TeamMemberTypeFilterOptions.ALL,1, 0);
		assertFalse(members.getResults().isEmpty());
		// now the other member is an admin
		TeamMember otherMember = members.getResults().get(0);
		assertEquals(otherPrincipalId, otherMember.getMember().getOwnerId());
		assertTrue(otherMember.getIsAdmin());

		// remove admin privileges
		synapseOne.setTeamMemberPermissions(team.getId(), otherPrincipalId, false);

		// now repeat the permissions change, but by accessing the ACL
		AccessControlList acl = synapseOne.getTeamACL(team.getId());

		Set<ACCESS_TYPE> otherAccessTypes = null;
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (ra.getPrincipalId().equals(otherPrincipalId)) otherAccessTypes=ra.getAccessType();
		}
		// since 'other' is not an admin, he won't have his own entry in the ACL
		assertNull(otherAccessTypes);

		ResourceAccess adminRa = new ResourceAccess();
		adminRa.setPrincipalId(Long.parseLong(otherPrincipalId));
		adminRa.setAccessType(ModelConstants.TEAM_ADMIN_PERMISSIONS);
		acl.getResourceAccess().add(adminRa);
		AccessControlList updatedACL = synapseOne.updateTeamACL(acl);
		assertEquals(acl.getResourceAccess(), updatedACL.getResourceAccess());
	}
	
	private PaginatedResults<Team> waitForTeams(String prefix, int limit, int offset) throws SynapseException, InterruptedException{
		long start = System.currentTimeMillis();
		while(true){
			PaginatedResults<Team> teams = synapseOne.getTeams(prefix,limit, offset);
			if(teams.getTotalNumberOfResults() < 1){
				Thread.sleep(1000);
				System.out.println("Waiting for principal prefix worker");
				if(System.currentTimeMillis() - start > RDS_WORKER_TIMEOUT){
					fail("Timed out waiting for principal prefix worker.");
				}
			}else{
				return teams;
			}
		}
	}
	
	private PaginatedResults<TeamMember> waitForTeamMembers(String teamId, String prefix, TeamMemberTypeFilterOptions memberType, int limit, int offset) throws SynapseException, InterruptedException{
		long start = System.currentTimeMillis();
		while (true){
			PaginatedResults<TeamMember> teamMembers = synapseOne.getTeamMembers(teamId, prefix, memberType, limit, offset);
			if (teamMembers.getResults().isEmpty()) {
				System.out.println("Waiting for principal prefix worker");
				Thread.sleep(1000);
				if(System.currentTimeMillis() - start > RDS_WORKER_TIMEOUT){
					fail("Timed out waiting for principal prefix worker.");
				}
			} else{
				return teamMembers;
			}
		}
	}

	@Test
	public void testTeamRestrictionRoundTrip() throws SynapseException {
		// create a Team
		String name = "Test-Team-Name";
		String description = "Test-Team-Description";
		Team team = new Team();
		team.setName(name);
		team.setDescription(description);
		Team createdTeam = synapseOne.createTeam(team);
		teamsToDelete.add(createdTeam.getId());

		// Create AccessRestriction
		TermsOfUseAccessRequirement tou = new TermsOfUseAccessRequirement();
		tou.setAccessType(ACCESS_TYPE.PARTICIPATE);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setType(RestrictableObjectType.TEAM);
		rod.setId(createdTeam.getId());
		tou.setSubjectIds(Collections.singletonList(rod));
		tou = adminSynapse.createAccessRequirement(tou);
		assertNotNull(tou.getId());
		accessRequirementsToDelete.add(tou.getId());
		
		// Query AccessRestriction
		PaginatedResults<AccessRequirement> paginatedResults;
		paginatedResults = adminSynapse.getAccessRequirements(rod, 10L, 0L);
		AccessRequirementUtil.checkTOUlist(paginatedResults, tou);
		
		paginatedResults = adminSynapse.getAccessRequirements(rod, 10L, 10L);
		assertTrue(paginatedResults.getResults().isEmpty());

		// Create AccessApproval
		AccessApproval aa = new AccessApproval();
		aa.setRequirementId(tou.getId());
		synapseTwo.createAccessApproval(aa);
		
		// Query AccessRestriction
		paginatedResults = adminSynapse.getAccessRequirements(rod, 10L, 0L);
		AccessRequirementUtil.checkTOUlist(paginatedResults, tou);		
	}

	@Test
	public void testMembershipInvitationAPI() throws Exception {
		// create a Team
		String name = "Test-Team-Name";
		String description = "Test-Team-Description";
		UserProfile synapseOneProfile = synapseOne.getMyProfile();
		String myPrincipalId = synapseOneProfile.getOwnerId();
		assertNotNull(myPrincipalId);
		Team team = new Team();
		team.setName(name);
		team.setDescription(description);
		Team createdTeam = synapseOne.createTeam(team);
		teamsToDelete.add(createdTeam.getId());
		
		// create an invitation
		MembershipInvitation dto = new MembershipInvitation();
		UserProfile inviteeUserProfile = synapseTwo.getMyProfile();
		List<String> inviteeEmails = inviteeUserProfile.getEmails();
		assertEquals(1, inviteeEmails.size());
		
		String inviteePrincipalId = inviteeUserProfile.getOwnerId();
		Date expiresOn = new Date(System.currentTimeMillis()+100000L);
		dto.setExpiresOn(expiresOn);
		dto.setInviteeId(inviteePrincipalId);
		String message = "Please accept this invitation";
		dto.setMessage(message);
		dto.setTeamId(createdTeam.getId());
		MembershipInvitation created = synapseOne.createMembershipInvitation(dto, MOCK_ACCEPT_INVITATION_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		assertEquals(myPrincipalId, created.getCreatedBy());
		assertNotNull(created.getCreatedOn());
		assertEquals(expiresOn, created.getExpiresOn());
		assertNotNull(created.getId());
		assertEquals(inviteePrincipalId, created.getInviteeId());
		assertEquals(message, created.getMessage());
		assertEquals(createdTeam.getId(), created.getTeamId());
		
		// check that open invitation count is 1
		Count openInvitationCount = synapseTwo.getOpenMembershipInvitationCount();
		assertEquals(1L, openInvitationCount.getCount().longValue());

		// get the invitation
		MembershipInvitation retrieved = synapseOne.getMembershipInvitation(created.getId());
		assertEquals(created, retrieved);
		
		{
			// query for invitations based on user
			PaginatedResults<MembershipInvitation> invitations = synapseOne.getOpenMembershipInvitations(inviteePrincipalId, null, 1, 0);
			assertEquals(1L, invitations.getTotalNumberOfResults());
			MembershipInvitation invitation = invitations.getResults().get(0);
			assertEquals(expiresOn, invitation.getExpiresOn());
			assertEquals(message, invitation.getMessage());
			assertEquals(createdTeam.getId(), invitation.getTeamId());
			assertEquals(inviteePrincipalId, invitation.getInviteeId());
			// check pagination
			invitations = synapseOne.getOpenMembershipInvitations(inviteePrincipalId, null, 2, 1);
			assertEquals(0L, invitations.getResults().size());
			// query for invitations based on user and team
			invitations = synapseOne.getOpenMembershipInvitations(inviteePrincipalId, createdTeam.getId(), 1, 0);
			assertEquals(1L, invitations.getTotalNumberOfResults());
			MembershipInvitation invitation2 = invitations.getResults().get(0);
			assertEquals(invitation, invitation2);
			// again, check pagination
			invitations = synapseOne.getOpenMembershipInvitations(inviteePrincipalId, createdTeam.getId(), 2, 1);
			assertEquals(1L, invitations.getTotalNumberOfResults());
			assertEquals(0L, invitations.getResults().size());
		}
		
		// query for invitation SUBMISSIONs based on team
		{
			PaginatedResults<MembershipInvitation> invitationSubmissions =
					synapseOne.getOpenMembershipInvitationSubmissions(createdTeam.getId(), null, 1, 0);
			assertEquals(1L, invitationSubmissions.getTotalNumberOfResults());
			MembershipInvitation submission = invitationSubmissions.getResults().get(0);
			assertEquals(created, submission);
			// check pagination
			invitationSubmissions = synapseOne.getOpenMembershipInvitationSubmissions(createdTeam.getId(), null, 2, 1);
			assertEquals(0L, invitationSubmissions.getResults().size());
			// query for SUBMISSIONs based on team and invitee
			invitationSubmissions = synapseOne.getOpenMembershipInvitationSubmissions(createdTeam.getId(), inviteePrincipalId, 1, 0);
			assertEquals(1L, invitationSubmissions.getTotalNumberOfResults());
			assertEquals(created, invitationSubmissions.getResults().get(0));
			// again, check pagination
			invitationSubmissions = synapseOne.getOpenMembershipInvitationSubmissions(createdTeam.getId(), inviteePrincipalId, 2, 1);
			assertEquals(1L, invitationSubmissions.getTotalNumberOfResults());
			assertEquals(0L, invitationSubmissions.getResults().size());
		}
		
		// delete the invitation
		synapseOne.deleteMembershipInvitation(created.getId());
		try {
			synapseOne.getMembershipInvitation(created.getId());
			fail("Failed to delete membership invitation.");
		} catch (SynapseException e) {
			// as expected
		}
		
		// create an invitation with null inviteeId and non null inviteeEmail
		dto.setInviteeId(null);
		dto.setInviteeEmail(TEST_EMAIL);
		MembershipInvitation mis = synapseOne.createMembershipInvitation(dto, MOCK_ACCEPT_INVITATION_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		InviteeVerificationSignedToken token = synapseTwo.getInviteeVerificationSignedToken(mis.getId());
		// test if getInviteeVerificationSignedToken succeeded
		assertNotNull(token);
		String inviteeId = inviteeUserProfile.getOwnerId();
		assertEquals(inviteeId, token.getInviteeId());
		assertEquals(mis.getId(), token.getMembershipInvitationId());

		// update the inviteeIinviteeUserProfile.getOwnerId() of the invitation
		synapseTwo.updateInviteeId(mis.getId(), token);
		mis = synapseTwo.getMembershipInvitation(mis.getId());
		// test if updateInviteeId succeeded
		assertEquals(inviteeId, mis.getInviteeId());

		// delete the second invitation
		synapseOne.deleteMembershipInvitation(mis.getId());
		try {
			synapseOne.getMembershipInvitation(mis.getId());
			fail("Failed to delete membership invitation.");
		} catch (SynapseException e) {
			// as expected
		}
	}

	@Test
	public void testMembershipRequestAPI() throws SynapseException {
		// create a Team
		String name = "Test-Team-Name";
		String description = "Test-Team-Description";
		String myPrincipalId = synapseOne.getMyProfile().getOwnerId();
		assertNotNull(myPrincipalId);
		Team team = new Team();
		team.setName(name);
		team.setDescription(description);
		Team createdTeam = synapseOne.createTeam(team);
		teamsToDelete.add(createdTeam.getId());
		
		// create a request
		String otherPrincipalId = synapseTwo.getMyProfile().getOwnerId();
		MembershipRequest dto = new MembershipRequest();
		Date expiresOn = new Date(System.currentTimeMillis()+100000L);
		dto.setExpiresOn(expiresOn);
		String message = "Please accept this request";
		dto.setMessage(message);
		dto.setTeamId(createdTeam.getId());
		MembershipRequest created = synapseTwo.createMembershipRequest(dto, MOCK_ACCEPT_MEMB_RQST_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		assertEquals(otherPrincipalId, created.getCreatedBy());
		assertNotNull(created.getCreatedOn());
		assertEquals(expiresOn, created.getExpiresOn());
		assertNotNull(created.getId());
		assertEquals(otherPrincipalId, created.getUserId());
		assertEquals(message, created.getMessage());
		assertEquals(createdTeam.getId(), created.getTeamId());
		// get the request
		MembershipRequest retrieved = synapseTwo.getMembershipRequest(created.getId());
		assertEquals(created, retrieved);

		// check that request count is 1
		Count requestCount = synapseOne.getOpenMembershipRequestCount();
		assertEquals(1L, requestCount.getCount().longValue());

		// query for requests based on team
		{
			PaginatedResults<MembershipRequest> requests = synapseOne.getOpenMembershipRequests(createdTeam.getId(), null, 1, 0);
			assertEquals(1L, requests.getTotalNumberOfResults());
			MembershipRequest request = requests.getResults().get(0);
			assertEquals(expiresOn, request.getExpiresOn());
			assertEquals(message, request.getMessage());
			assertEquals(createdTeam.getId(), request.getTeamId());
			assertEquals(otherPrincipalId, request.getUserId());
			// check pagination
			requests = synapseOne.getOpenMembershipRequests(createdTeam.getId(), null, 2, 1);
			assertEquals(1L, requests.getTotalNumberOfResults());
			assertEquals(0L, requests.getResults().size());
			// query for requests based on team and member
			requests = synapseOne.getOpenMembershipRequests(createdTeam.getId(), otherPrincipalId, 1, 0);
			assertEquals(1L, requests.getTotalNumberOfResults());
			MembershipRequest request2 = requests.getResults().get(0);
			assertEquals(request, request2);
			// again, check pagination
			requests = synapseOne.getOpenMembershipRequests(createdTeam.getId(), otherPrincipalId, 2, 1);
			assertEquals(1L, requests.getTotalNumberOfResults());
			assertEquals(0L, requests.getResults().size());
		}
		
		// query for request SUBMISSIONs based on team
		{
			PaginatedResults<MembershipRequest> requestSubmissions = synapseTwo.getOpenMembershipRequestSubmissions(otherPrincipalId, null, 1, 0);
			assertEquals(1L, requestSubmissions.getTotalNumberOfResults());
			MembershipRequest requestSubmission = requestSubmissions.getResults().get(0);
			assertEquals(created, requestSubmission);
			// check pagination
			requestSubmissions = synapseTwo.getOpenMembershipRequestSubmissions(otherPrincipalId, null, 2, 1);
			assertEquals(1L, requestSubmissions.getTotalNumberOfResults());
			assertEquals(0L, requestSubmissions.getResults().size());
			// query for requests based on team and member
			requestSubmissions = synapseTwo.getOpenMembershipRequestSubmissions(otherPrincipalId, createdTeam.getId(), 1, 0);
			assertEquals(1L, requestSubmissions.getTotalNumberOfResults());
			assertEquals(created, requestSubmissions.getResults().get(0));
			// again, check pagination
			requestSubmissions = synapseTwo.getOpenMembershipRequestSubmissions(otherPrincipalId, createdTeam.getId(), 2, 1);
			assertEquals(1L, requestSubmissions.getTotalNumberOfResults());
			assertEquals(0L, requestSubmissions.getResults().size());
		}

		// delete the request
		synapseTwo.deleteMembershipRequest(created.getId());
		try {
			synapseTwo.getMembershipRequest(created.getId());
			fail("Failed to delete membership request.");
		} catch (SynapseException e) {
			// as expected
		}
	}

	@Disabled // See PLFM-4131
	@Test
	public void testMembershipInvitationAndAcceptanceViaNotification() throws Exception {
		// create a Team
		String name = "Test-Team-Name";
		String description = "Test-Team-Description";
		UserProfile synapseOneProfile = synapseOne.getMyProfile();
		String myPrincipalId = synapseOneProfile.getOwnerId();
		assertNotNull(myPrincipalId);
		Team team = new Team();
		team.setName(name);
		team.setDescription(description);
		Team createdTeam = synapseOne.createTeam(team);
		teamsToDelete.add(createdTeam.getId());
		
		// create an invitation
		MembershipInvitation dto = new MembershipInvitation();
		UserProfile inviteeUserProfile = synapseTwo.getMyProfile();
		List<String> inviteeEmails = inviteeUserProfile.getEmails();
		assertEquals(1, inviteeEmails.size());
		String inviteeEmail = inviteeEmails.get(0);
		String inviteeNotification = EmailValidationUtil.getBucketKeyForEmail(inviteeEmail);
		if (EmailValidationUtil.doesFileExist(inviteeNotification, 2000L))
			EmailValidationUtil.deleteFile(inviteeNotification);
		
		String inviteePrincipalId = inviteeUserProfile.getOwnerId();
		Date expiresOn = new Date(System.currentTimeMillis()+100000L);
		dto.setExpiresOn(expiresOn);
		dto.setInviteeId(inviteePrincipalId);
		String message = "Please accept this invitation";
		dto.setMessage(message);
		dto.setTeamId(createdTeam.getId());
		synapseOne.createMembershipInvitation(dto, MOCK_ACCEPT_INVITATION_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		
		// check that a notification was sent to the invitee
		assertTrue(EmailValidationUtil.doesFileExist(inviteeNotification, 60000L));
		
		// make sure there's no lingering inviter notification
		String inviterNotification = EmailValidationUtil.getBucketKeyForEmail(synapseOneProfile.getEmails().get(0));
		if (EmailValidationUtil.doesFileExist(inviterNotification, 2000L))
			EmailValidationUtil.deleteFile(inviterNotification);
		
		// now get the embedded tokens
		String startString = "<a href=\""+MOCK_ACCEPT_INVITATION_ENDPOINT;
		String endString = "\"";
		String jsst = EmailValidationUtil.getTokenFromFile(inviteeNotification, startString, endString);
		JoinTeamSignedToken joinTeamSignedToken = SerializationUtils.hexDecodeAndDeserialize(jsst, JoinTeamSignedToken.class);

		startString = "<a href=\""+MOCK_NOTIFICATION_UNSUB_ENDPOINT;
		endString = "\"";
		String nsst = EmailValidationUtil.getTokenFromFile(inviteeNotification, startString, endString);
		NotificationSettingsSignedToken notificationSettingsSignedToken = 
				SerializationUtils.hexDecodeAndDeserialize(nsst, NotificationSettingsSignedToken.class);
		// delete the message
		EmailValidationUtil.deleteFile(inviteeNotification);
		
		ResponseMessage m = synapseTwo.addTeamMember(joinTeamSignedToken, MOCK_TEAM_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		assertNotNull(m.getMessage());
		
		// now I should be in the team
		TeamMembershipStatus tms = synapseTwo.getTeamMembershipStatus(createdTeam.getId(), inviteePrincipalId);
		assertTrue(tms.getIsMember());
		
		m = synapseTwo.updateNotificationSettings(notificationSettingsSignedToken);
		assertNotNull(m.getMessage());
		
		// settings should now be updated
		inviteeUserProfile = synapseTwo.getMyProfile();
		assertFalse(inviteeUserProfile.getNotificationSettings().getSendEmailNotifications());
		
		// finally, the invitER should have been notified that the invitEE joined the team
		//	assertTrue(EmailValidationUtil.doesFileExist(inviterNotification, 60000L));
		EmailValidationUtil.deleteFile(inviterNotification);
	}

	@Disabled // See PLFM-4131
	@Test
	public void testMembershipRequestAndAcceptanceViaNotification() throws Exception {
		// create a Team
		String name = "Test-Team-Name";
		String description = "Test-Team-Description";
		String myPrincipalId = synapseOne.getMyProfile().getOwnerId();
		assertNotNull(myPrincipalId);
		Team team = new Team();
		team.setName(name);
		team.setDescription(description);
		Team createdTeam = synapseOne.createTeam(team);
		teamsToDelete.add(createdTeam.getId());
		
		// clear any existing notification
		UserProfile adminUserProfile = synapseOne.getMyProfile();
		List<String> adminEmails = adminUserProfile.getEmails();
		assertEquals(1, adminEmails.size());
		String adminEmail = adminEmails.get(0);
		String adminNotification = EmailValidationUtil.getBucketKeyForEmail(adminEmail);
		if (EmailValidationUtil.doesFileExist(adminNotification, 2000L))
			EmailValidationUtil.deleteFile(adminNotification);

		// create a request
		UserProfile requesterProfile = synapseTwo.getMyProfile();
		String requesterPrincipalId = requesterProfile.getOwnerId();
		MembershipRequest dto = new MembershipRequest();
		Date expiresOn = new Date(System.currentTimeMillis()+100000L);
		dto.setExpiresOn(expiresOn);
		String message = "Please accept this request";
		dto.setMessage(message);
		dto.setTeamId(createdTeam.getId());
		synapseTwo.createMembershipRequest(dto, MOCK_ACCEPT_MEMB_RQST_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);

		// check that a notification was sent to the admin
		assertTrue(EmailValidationUtil.doesFileExist(adminNotification, 60000L));
		
		// make sure there's no lingering requester notification
		String requesterNotification = EmailValidationUtil.getBucketKeyForEmail(requesterProfile.getEmails().get(0));
		if (EmailValidationUtil.doesFileExist(requesterNotification, 2000L))
			EmailValidationUtil.deleteFile(requesterNotification);
		
		// now get the embedded tokens
		String startString = "<a href=\""+MOCK_ACCEPT_MEMB_RQST_ENDPOINT;
		String endString = "\"";
		String jsst = EmailValidationUtil.getTokenFromFile(adminNotification, startString, endString);
		JoinTeamSignedToken joinTeamSignedToken = SerializationUtils.hexDecodeAndDeserialize(jsst, JoinTeamSignedToken.class);

		startString = "<a href=\""+MOCK_NOTIFICATION_UNSUB_ENDPOINT;
		endString = "\"";
		String nsst = EmailValidationUtil.getTokenFromFile(adminNotification, startString, endString);
		NotificationSettingsSignedToken notificationSettingsSignedToken = 
				SerializationUtils.hexDecodeAndDeserialize(nsst, NotificationSettingsSignedToken.class);
		// delete the message
		EmailValidationUtil.deleteFile(adminNotification);
		
		ResponseMessage m = synapseOne.addTeamMember(joinTeamSignedToken, MOCK_TEAM_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		assertNotNull(m.getMessage());
		
		// now requester should be in the team
		TeamMembershipStatus tms = synapseTwo.getTeamMembershipStatus(createdTeam.getId(), requesterPrincipalId);
		assertTrue(tms.getIsMember());
		
		m = synapseOne.updateNotificationSettings(notificationSettingsSignedToken);
		assertNotNull(m.getMessage());
		
		// admin settings should now be updated
		adminUserProfile = synapseOne.getMyProfile();
		assertFalse(adminUserProfile.getNotificationSettings().getSendEmailNotifications());
		
		// finally, the requester should have been notified that the admin added her to the team
		// assertTrue(EmailValidationUtil.doesFileExist(requesterNotification, 2000L),"Can't find file "+requesterNotification);
		EmailValidationUtil.deleteFile(requesterNotification);
	}

}
