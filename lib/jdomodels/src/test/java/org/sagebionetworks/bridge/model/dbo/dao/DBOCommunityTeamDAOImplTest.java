package org.sagebionetworks.bridge.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.bridge.model.CommunityTeamDAO;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTeam;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOCommunityTeamDAOImplTest {

	@SuppressWarnings("rawtypes")
	public class Deletable {
		Long id;
		Class clazz;

		public Deletable(Long l, Class c) {
			this.id = l;
			this.clazz = c;
		}
	}

	@Autowired
	private CommunityTeamDAO communityTeamDAO;

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private GroupMembersDAO groupMembersDAO;

	@Autowired
	private NodeDAO nodeDAO;

	@Autowired
	private DBOBasicDao dboBasicDao;

	@Autowired
	private IdGenerator idGenerator;

	List<Deletable> toDelete = Lists.newArrayList();

	@SuppressWarnings({ "unchecked" })
	@After
	public void after() throws Exception {
		if (dboBasicDao != null) {
			for (Deletable item : toDelete) {
				if (item.clazz == UserGroup.class) {
					userGroupDAO.delete("" + item.id);
				} else {
					MapSqlParameterSource params = new MapSqlParameterSource("id", item.id);
					try {
						dboBasicDao.getObjectByPrimaryKey(item.clazz, params);
						dboBasicDao.deleteObjectByPrimaryKey(item.clazz, params);
					} catch (NotFoundException e) {
						// ignore, expected
					}
				}
			}
		}
	}

	private String createMember() throws Exception {
		UserGroup newMember = new UserGroup();
		newMember.setName("u-" + idGenerator.generateNewId());
		newMember.setIsIndividual(true);
		String newMemberId = userGroupDAO.create(newMember);
		toDelete.add(new Deletable(Long.parseLong(newMemberId), UserGroup.class));
		return newMemberId;
	}

	private DBOTeam createTeam(String... memberIds) throws Exception {
		UserGroup newGroup = new UserGroup();
		newGroup.setName("group-" + idGenerator.generateNewId());
		String newGroupId = userGroupDAO.create(newGroup);

		groupMembersDAO.addMembers(newGroupId, Arrays.asList(memberIds));

		DBOTeam team = new DBOTeam();
		Long id = Long.parseLong(newGroupId);
		team.setId(id);
		team.setEtag("1");
		team.setProperties((new String("12345")).getBytes());
		DBOTeam clone = dboBasicDao.createNew(team);
		assertNotNull(clone);

		toDelete.add(new Deletable(id, DBOTeam.class));
		toDelete.add(new Deletable(id, UserGroup.class));

		return clone;
	}

	private DBONode createNode() {
		DBONode node = new DBONode();
		node.setId(idGenerator.generateNewId());
		node.setName("SomeCommunity");
		node.setBenefactorId(node.getId());
		Long createdById = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		node.setCreatedBy(createdById);
		node.setCreatedOn(System.currentTimeMillis());
		node.setCurrentRevNumber(null);
		node.seteTag("1");
		node.setNodeType(EntityType.project.getId());
		// Make sure we can create it
		DBONode clone = dboBasicDao.createNew(node);
		assertNotNull(clone);

		toDelete.add(new Deletable(node.getId(), DBONode.class));

		return clone;
	}

	@Test
	public void testRoundTrip() throws Exception {
		DBOTeam team = createTeam();
		DBONode node = createNode();

		communityTeamDAO.create(node.getId(), team.getId());

		assertEquals(node.getId().longValue(), communityTeamDAO.getCommunityId(team.getId()));
	}

	@Test
	public void testGetMultiple() throws Exception {
		int startNodeCount = communityTeamDAO.getCommunityIds().size();
		DBOTeam team1 = createTeam();
		DBOTeam team2 = createTeam();
		DBOTeam team3 = createTeam();
		DBONode node1 = createNode();
		DBONode node2 = createNode();

		communityTeamDAO.create(node1.getId(), team1.getId());
		communityTeamDAO.create(node1.getId(), team2.getId());
		communityTeamDAO.create(node2.getId(), team3.getId());

		assertEquals(2, communityTeamDAO.getCommunityIds().size() - startNodeCount);

		dboBasicDao.deleteObjectByPrimaryKey(DBOTeam.class, new MapSqlParameterSource("id", team3.getId()));
		dboBasicDao.deleteObjectByPrimaryKey(DBONode.class, new MapSqlParameterSource("id", node2.getId()));

		assertEquals(1, communityTeamDAO.getCommunityIds().size() - startNodeCount);
	}

	@Test
	public void testGetByMemberMultiple() throws Exception {
		String member1 = createMember();
		String member2 = createMember();
		String member3 = createMember();

		DBOTeam team1 = createTeam(member1);
		DBOTeam team2 = createTeam(member1, member2);
		DBOTeam team3 = createTeam(member2);
		DBOTeam team4 = createTeam();
		DBONode node1 = createNode();
		DBONode node2 = createNode();
		DBONode node3 = createNode();

		communityTeamDAO.create(node1.getId(), team1.getId());
		communityTeamDAO.create(node1.getId(), team2.getId());
		communityTeamDAO.create(node2.getId(), team3.getId());
		communityTeamDAO.create(node3.getId(), team4.getId());

		assertEquals(1, communityTeamDAO.getCommunityIdsByMember(member1).size());
		assertEquals(2, communityTeamDAO.getCommunityIdsByMember(member2).size());
		assertEquals(0, communityTeamDAO.getCommunityIdsByMember(member3).size());

		groupMembersDAO.removeMembers("" + team2.getId(), Arrays.asList(member1, member2));

		assertEquals(1, communityTeamDAO.getCommunityIdsByMember(member1).size());
		assertEquals(1, communityTeamDAO.getCommunityIdsByMember(member2).size());
	}
}
