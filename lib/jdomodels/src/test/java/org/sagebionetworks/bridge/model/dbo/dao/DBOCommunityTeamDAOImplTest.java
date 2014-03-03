package org.sagebionetworks.bridge.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.bridge.model.CommunityTeamDAO;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOCommunityTeam;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTeam;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOCommunityTeamDAOImplTest extends TestBase {

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

	private Team createTeam(String... memberIds) throws Exception {
		UserGroup newGroup = new UserGroup();
		newGroup.setIsIndividual(false);
		String newGroupId = userGroupDAO.create(newGroup).toString();
		addToDelete(UserGroup.class, newGroupId);

		groupMembersDAO.addMembers(newGroupId, Arrays.asList(memberIds));

		Team team = new Team();
		team.setId(newGroupId);
		team = teamDAO.create(team);
		addToDelete(DBOTeam.class, team.getId());
		return team;
	}

	private Node createNode(String creatorId) throws NotFoundException {
		Long createdById = Long.parseLong(creatorId);
		
		Node node = new Node();
		node.setName("SomeCommunity");
		node.setCreatedByPrincipalId(createdById);
		node.setCreatedOn(new Date());
		node.setModifiedByPrincipalId(createdById);
		node.setModifiedOn(new Date());
		node.setNodeType(EntityType.project.name());
		String nodeId = nodeDAO.createNew(node);
		node.setId(nodeId);
		
		addToDelete(DBONode.class, nodeId);

		return node;
	}

	@Test
	public void testRoundTrip() throws Exception {
		Team team = createTeam();
		Node node = createNode(createMember());

		communityTeamDAO.create(KeyFactory.stringToKey(node.getId()), Long.parseLong(team.getId()));
		addToDelete(DBOCommunityTeam.class, team.getId());

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
		addToDelete(DBOCommunityTeam.class, team1.getId());
		addToDelete(DBOCommunityTeam.class, team2.getId());
		addToDelete(DBOCommunityTeam.class, team3.getId());

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
		addToDelete(DBOCommunityTeam.class, team1.getId());
		addToDelete(DBOCommunityTeam.class, team2.getId());
		addToDelete(DBOCommunityTeam.class, team3.getId());
		addToDelete(DBOCommunityTeam.class, team4.getId());

		assertEquals(1, communityTeamDAO.getCommunityIdsByMember(member1).size());
		assertEquals(2, communityTeamDAO.getCommunityIdsByMember(member2).size());
		assertEquals(0, communityTeamDAO.getCommunityIdsByMember(member3).size());

		groupMembersDAO.removeMembers("" + team2.getId(), Arrays.asList(member1, member2));

		assertEquals(1, communityTeamDAO.getCommunityIdsByMember(member1).size());
		assertEquals(1, communityTeamDAO.getCommunityIdsByMember(member2).size());
	}
}
