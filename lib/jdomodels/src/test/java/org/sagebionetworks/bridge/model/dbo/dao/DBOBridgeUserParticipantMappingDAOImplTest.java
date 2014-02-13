package org.sagebionetworks.bridge.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import java.security.GeneralSecurityException;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.bridge.model.BridgeParticipantDAO;
import org.sagebionetworks.bridge.model.BridgeUserParticipantMappingDAO;
import org.sagebionetworks.bridge.model.ParticipantDataId;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOUserParticipantMap;
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

	private static ParticipantDataId dataId1 = new ParticipantDataId(1);
	private static ParticipantDataId dataId2 = new ParticipantDataId(2);
	private static ParticipantDataId dataId3 = new ParticipantDataId(3);
	private static ParticipantDataId dataId4 = new ParticipantDataId(4);
	private static ParticipantDataId dataId5 = new ParticipantDataId(5);
	private static ParticipantDataId dataId6 = new ParticipantDataId(6);

	@Test
	public void testEmpty() throws Exception {
		List<ParticipantDataId> ids = userParticipantMappingDAO.getParticipantIdsForUser(-1L);
		assertEquals(0, ids.size());
	}

	@Test
	public void testGetAndSet() throws Exception {
		String userId = this.createMember();
		long id = Long.parseLong(userId);

		addToDelete(DBOUserParticipantMap.class, userId);

		List<ParticipantDataId> ids = userParticipantMappingDAO.getParticipantIdsForUser(id);
		assertEquals(0, ids.size());

		userParticipantMappingDAO.setParticipantIdsForUser(id, Lists.<ParticipantDataId> newArrayList());
		ids = userParticipantMappingDAO.getParticipantIdsForUser(id);
		assertEquals(0, ids.size());

		userParticipantMappingDAO.setParticipantIdsForUser(id, Lists.<ParticipantDataId> newArrayList(dataId1, dataId2));
		ids = userParticipantMappingDAO.getParticipantIdsForUser(id);
		assertEquals(Lists.<ParticipantDataId> newArrayList(dataId1, dataId2), ids);

		userParticipantMappingDAO.setParticipantIdsForUser(id, Lists.<ParticipantDataId> newArrayList(dataId3, dataId4));
		ids = userParticipantMappingDAO.getParticipantIdsForUser(id);
		assertEquals(Lists.<ParticipantDataId> newArrayList(dataId3, dataId4), ids);

		userParticipantMappingDAO.setParticipantIdsForUser(id,
				Lists.<ParticipantDataId> newLinkedList(Lists.<ParticipantDataId> newArrayList(dataId5, dataId6)));
		ids = userParticipantMappingDAO.getParticipantIdsForUser(id);
		assertEquals(Lists.<ParticipantDataId> newArrayList(dataId5, dataId6), ids);

		userParticipantMappingDAO.setParticipantIdsForUser(id, Lists.<ParticipantDataId> newArrayList());
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

		userParticipantMappingDAO.setParticipantIdsForUser(id, Lists.<ParticipantDataId> newArrayList(dataId1, dataId2));
		List<ParticipantDataId> ids = userParticipantMappingDAO.getParticipantIdsForUser(id);
		assertEquals(Lists.<ParticipantDataId> newArrayList(dataId1, dataId2), ids);

		ids = userParticipantMappingDAO.getParticipantIdsForUser(id2);
		assertEquals(0, ids.size());

		simpleJdbcTemplate.update("update " + SqlConstants.TABLE_USER_PARTICIPANT_MAP + " set "
				+ SqlConstants.COL_USER_PARTICIPANT_MAP_USER_ID + " = ? where " + SqlConstants.COL_USER_PARTICIPANT_MAP_USER_ID + " = ?",
				userId2, userId);

		userParticipantMappingDAO.getParticipantIdsForUser(id2);
	}
}
