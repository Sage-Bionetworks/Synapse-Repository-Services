package org.sagebionetworks.bridge.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.bridge.model.BridgeParticipantDAO;
import org.sagebionetworks.bridge.model.BridgeUserParticipantMappingDAO;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOUserParticipantMap;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOBridgeUserParticipantMappingDAOImplTest extends TestBase {

	@Autowired
	private BridgeUserParticipantMappingDAO userParticipantMappingDAO;

	@Autowired
	private BridgeParticipantDAO bridgeParticipantDAO;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Test
	public void testEmpty() throws Exception {
		List<String> ids = userParticipantMappingDAO.getParticipantIdsForUser(-1L);
		assertEquals(0, ids.size());
	}

	@Test
	public void testGetAndSet() throws Exception {
		String userId = this.createMember();
		long id = Long.parseLong(userId);

		addToDelete(DBOUserParticipantMap.class, userId);

		List<String> ids = userParticipantMappingDAO.getParticipantIdsForUser(id);
		assertEquals(0, ids.size());

		userParticipantMappingDAO.setParticipantIdsForUser(id, Lists.<String> newArrayList());
		ids = userParticipantMappingDAO.getParticipantIdsForUser(id);
		assertEquals(0, ids.size());

		userParticipantMappingDAO.setParticipantIdsForUser(id, Lists.<String> newArrayList("a", "b"));
		ids = userParticipantMappingDAO.getParticipantIdsForUser(id);
		assertEquals(Lists.<String> newArrayList("a", "b"), ids);

		userParticipantMappingDAO.setParticipantIdsForUser(id, Lists.<String> newArrayList("c", "d"));
		ids = userParticipantMappingDAO.getParticipantIdsForUser(id);
		assertEquals(Lists.<String> newArrayList("c", "d"), ids);

		userParticipantMappingDAO.setParticipantIdsForUser(id, Lists.<String> newLinkedList(Lists.<String> newArrayList("e", "f")));
		ids = userParticipantMappingDAO.getParticipantIdsForUser(id);
		assertEquals(Lists.<String> newArrayList("e", "f"), ids);

		userParticipantMappingDAO.setParticipantIdsForUser(id, Lists.<String> newArrayList());
		ids = userParticipantMappingDAO.getParticipantIdsForUser(id);
		assertEquals(0, ids.size());
	}

	@Test(expected = GeneralSecurityException.class)
	public void testSalt() throws Exception {
		String userId = this.createMember();
		long id = Long.parseLong(userId);
		String userId2 = this.createMember();
		long id2 = Long.parseLong(userId2);

		addToDelete(DBOUserParticipantMap.class, userId);
		addToDelete(DBOUserParticipantMap.class, userId2);

		userParticipantMappingDAO.setParticipantIdsForUser(id, Lists.<String> newArrayList("a", "b"));
		List<String> ids = userParticipantMappingDAO.getParticipantIdsForUser(id);
		assertEquals(Lists.<String> newArrayList("a", "b"), ids);

		ids = userParticipantMappingDAO.getParticipantIdsForUser(id2);
		assertEquals(0, ids.size());

		simpleJdbcTemplate.update("update " + SqlConstants.TABLE_USER_PARTICIPANT_MAP + " set "
				+ SqlConstants.COL_USER_PARTICIPANT_MAP_USER_ID + " = ? where " + SqlConstants.COL_USER_PARTICIPANT_MAP_USER_ID + " = ?",
				userId2, userId);

		userParticipantMappingDAO.getParticipantIdsForUser(id2);
	}
}
