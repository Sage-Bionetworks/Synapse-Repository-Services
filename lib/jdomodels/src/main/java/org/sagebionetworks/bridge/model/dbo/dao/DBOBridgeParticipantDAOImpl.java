package org.sagebionetworks.bridge.model.dbo.dao;

import org.sagebionetworks.bridge.model.BridgeParticipantDAO;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOParticipant;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOBridgeParticipantDAOImpl implements BridgeParticipantDAO {

	@Autowired
	private DBOBasicDao basicDao;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void create(String participantId) {
		DBOParticipant dboParticipant = new DBOParticipant();
		dboParticipant.setParticipantId(Long.parseLong(participantId));
		basicDao.createNew(dboParticipant);
	}
}
