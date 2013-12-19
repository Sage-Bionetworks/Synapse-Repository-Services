package org.sagebionetworks.bridge.model.dbo.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.bridge.model.BridgeParticipantDAO;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOParticipant;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOBridgeParticipantDAOImplTest extends TestBase {

	@Autowired
	private BridgeParticipantDAO participantDAO;

	@Autowired
	private DBOBasicDao dboBasicDao;

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private GroupMembersDAO groupMembersDAO;

	@Autowired
	private IdGenerator idGenerator;

	@Test
	public void testCreateParticipant() throws Exception {
		String user = createMember();
		participantDAO.create(user);
		addToDelete(DBOParticipant.class, user);
	}
}
