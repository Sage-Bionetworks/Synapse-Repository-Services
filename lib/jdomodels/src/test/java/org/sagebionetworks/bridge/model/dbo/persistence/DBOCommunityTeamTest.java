package org.sagebionetworks.bridge.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOCommunityTeamTest {
	
	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private NodeDAO nodeDAO;

	@Autowired
	private TeamDAO teamDAO;
	
	@Autowired
	private DBOBasicDao basicDAO;
	
	private UserGroup group;
	private Team team;
	private Node node;
	
	@Before
	public void before() throws Exception {
		// To satisfy the Team's FK
		group = new UserGroup();
		group.setIsIndividual(false);
		String newGroupId = userGroupDAO.create(group).toString();
		group.setId(newGroupId);

		// Create a Team
		team = new Team();
		team.setId(newGroupId);
		team = teamDAO.create(team);

		// Create a Node
		Long createdById = Long.parseLong(newGroupId);
		node = new Node();
		node.setName("SomeCommunity");
		node.setCreatedByPrincipalId(createdById);
		node.setCreatedOn(new Date());
		node.setModifiedByPrincipalId(createdById);
		node.setModifiedOn(new Date());
		node.setNodeType(EntityType.project.name());
		String nodeId = nodeDAO.createNew(node);
		node.setId(nodeId);
	}
	
	@After
	public void after() throws Exception {
		teamDAO.delete(team.getId());
		nodeDAO.delete(node.getId());
		userGroupDAO.delete(group.getId());
	}

	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException, UnsupportedEncodingException {
		DBOCommunityTeam communityTeam = new DBOCommunityTeam();
		communityTeam.setCommunityId(KeyFactory.stringToKey(node.getId()));
		communityTeam.setTeamId(Long.parseLong(team.getId()));
		DBOCommunityTeam clone = basicDAO.createNew(communityTeam);
		assertNotNull(clone);
		assertEquals(communityTeam, clone);

		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("teamId", communityTeam.getTeamId());
		clone = basicDAO.getObjectByPrimaryKey(DBOCommunityTeam.class, params);
		assertNotNull(clone);
		assertEquals(communityTeam, clone);

		// make sure we can delete
		basicDAO.deleteObjectByPrimaryKey(DBOCommunityTeam.class, params);
	}
}
