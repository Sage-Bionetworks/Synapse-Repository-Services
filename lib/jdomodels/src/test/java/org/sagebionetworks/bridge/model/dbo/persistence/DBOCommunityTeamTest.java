package org.sagebionetworks.bridge.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTeam;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOCommunityTeamTest {

	@Autowired
	DBOBasicDao dboBasicDao;

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private IdGenerator idGenerator;

	@SuppressWarnings("rawtypes")
	Map<Long, Class> toDelete = new HashMap<Long, Class>();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@After
	public void after() throws DatastoreException {
		if (dboBasicDao != null) {
			for (Map.Entry<Long, Class> id : toDelete.entrySet()) {
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id.getKey());
				dboBasicDao.deleteObjectByPrimaryKey(id.getValue(), params);
			}
		}
	}

	private DBOTeam createTeam() {
		Long id = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		DBOTeam team = new DBOTeam();
		team.setId(id);
		team.setEtag("1");
		team.setProperties((new String("12345")).getBytes());
		toDelete.put(id, DBOTeam.class);
		DBOTeam clone = dboBasicDao.createNew(team);
		assertNotNull(clone);
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
		toDelete.put(node.getId(), DBONode.class);
		return clone;
	}

	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException, UnsupportedEncodingException {
		// Make sure we can create it
		DBOTeam team = createTeam();
		DBONode node = createNode();

		DBOCommunityTeam communityTeam = new DBOCommunityTeam();
		communityTeam.setCommunityId(node.getId());
		communityTeam.setTeamId(team.getId());
		DBOCommunityTeam clone = dboBasicDao.createNew(communityTeam);
		assertNotNull(clone);
		assertEquals(communityTeam, clone);

		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("teamId", communityTeam.getTeamId());
		clone = dboBasicDao.getObjectByPrimaryKey(DBOCommunityTeam.class, params);
		assertNotNull(clone);
		assertEquals(communityTeam, clone);

		// make sure we can delete
		dboBasicDao.deleteObjectByPrimaryKey(DBOCommunityTeam.class, params);
	}
}
