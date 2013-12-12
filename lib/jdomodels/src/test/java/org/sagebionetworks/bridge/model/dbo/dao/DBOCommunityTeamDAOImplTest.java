package org.sagebionetworks.bridge.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.bridge.model.CommunityTeamDAO;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOCommunityTeamDAOImplTest {

	@Autowired
	private CommunityTeamDAO communityTeamDAO;

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private GroupMembersDAO groupMembersDAO;

	@Autowired
	private NodeDAO nodeDAO;

	@Autowired
	private TeamDAO teamDAO;

	private List<String> teamsToDelete;
	private List<String> nodesToDelete;
	private List<String> usersToDelete;
	
	@Before
	public void before() throws Exception {
		teamsToDelete = new ArrayList<String>();
		nodesToDelete = new ArrayList<String>();
		usersToDelete = new ArrayList<String>();
	}

	@After
	public void after() throws Exception {
		for (String id : teamsToDelete) {
			teamDAO.delete(id);
		}
		
		for (String id : nodesToDelete) {
			nodeDAO.delete(id);
		}

		for (String id : usersToDelete) {
			userGroupDAO.delete(id);
		}
	}

	private String createMember() throws Exception {
		UserGroup newMember = new UserGroup();
		newMember.setName(UUID.randomUUID().toString());
		newMember.setIsIndividual(true);
		String newMemberId = userGroupDAO.create(newMember);
		usersToDelete.add(newMemberId);
		return newMemberId;
	}

	private Team createTeam(String... memberIds) throws Exception {
		UserGroup newGroup = new UserGroup();
		newGroup.setName(UUID.randomUUID().toString());
		String newGroupId = userGroupDAO.create(newGroup);
		usersToDelete.add(newGroupId);

		groupMembersDAO.addMembers(newGroupId, Arrays.asList(memberIds));

		Team team = new Team();
		team.setId(newGroupId);
		team = teamDAO.create(team);
		teamsToDelete.add(team.getId());
		return team;
	}

	private Node createNode(String creatorId) throws NotFoundException {
		Long createdById = Long.parseLong(userGroupDAO.get(creatorId).getId());
		
		Node node = new Node();
		node.setName("SomeCommunity");
		node.setCreatedByPrincipalId(createdById);
		node.setCreatedOn(new Date());
		node.setModifiedByPrincipalId(createdById);
		node.setModifiedOn(new Date());
		node.setNodeType(EntityType.project.name());
		String nodeId = nodeDAO.createNew(node);
		node.setId(nodeId);
		
		nodesToDelete.add(nodeId);

		return node;
	}

	@Test
	public void testRoundTrip() throws Exception {
		Team team = createTeam();
		Node node = createNode(createMember());

		communityTeamDAO.create(KeyFactory.stringToKey(node.getId()), Long.parseLong(team.getId()));

		assertEquals(KeyFactory.stringToKey(node.getId()).longValue(), communityTeamDAO.getCommunityId(Long.parseLong(team.getId())));
	}

	@Test
	public void testGetMultiple() throws Exception {
		int startNodeCount = communityTeamDAO.getCommunityIds().size();
		Team team1 = createTeam();
		Team team2 = createTeam();
		Team team3 = createTeam();
		String nodeCreator = createMember();
		Node node1 = createNode(nodeCreator);
		Node node2 = createNode(nodeCreator);

		communityTeamDAO.create(KeyFactory.stringToKey(node1.getId()), Long.parseLong(team1.getId()));
		communityTeamDAO.create(KeyFactory.stringToKey(node1.getId()), Long.parseLong(team2.getId()));
		communityTeamDAO.create(KeyFactory.stringToKey(node2.getId()), Long.parseLong(team3.getId()));

		assertEquals(2, communityTeamDAO.getCommunityIds().size() - startNodeCount);

		teamDAO.delete(team3.getId());
		nodeDAO.delete(node2.getId());

		assertEquals(1, communityTeamDAO.getCommunityIds().size() - startNodeCount);
	}

	@Test
	public void testGetByMemberMultiple() throws Exception {
		String member1 = createMember();
		String member2 = createMember();
		String member3 = createMember();

		Team team1 = createTeam(member1);
		Team team2 = createTeam(member1, member2);
		Team team3 = createTeam(member2);
		Team team4 = createTeam();
		Node node1 = createNode(member1);
		Node node2 = createNode(member1);
		Node node3 = createNode(member1);

		communityTeamDAO.create(KeyFactory.stringToKey(node1.getId()), Long.parseLong(team1.getId()));
		communityTeamDAO.create(KeyFactory.stringToKey(node1.getId()), Long.parseLong(team2.getId()));
		communityTeamDAO.create(KeyFactory.stringToKey(node2.getId()), Long.parseLong(team3.getId()));
		communityTeamDAO.create(KeyFactory.stringToKey(node3.getId()), Long.parseLong(team4.getId()));

		assertEquals(1, communityTeamDAO.getCommunityIdsByMember(member1).size());
		assertEquals(2, communityTeamDAO.getCommunityIdsByMember(member2).size());
		assertEquals(0, communityTeamDAO.getCommunityIdsByMember(member3).size());

		groupMembersDAO.removeMembers("" + team2.getId(), Arrays.asList(member1, member2));

		assertEquals(1, communityTeamDAO.getCommunityIdsByMember(member1).size());
		assertEquals(1, communityTeamDAO.getCommunityIdsByMember(member2).size());
	}
}
