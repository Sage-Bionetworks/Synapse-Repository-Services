package org.sagebionetworks.bridge.manager.participantdata;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Random;

import org.sagebionetworks.bridge.model.BridgeParticipantDAO;
import org.sagebionetworks.bridge.model.BridgeUserParticipantMappingDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

public class ParticipantDataIdMappingManagerImpl implements ParticipantDataIdMappingManager {

	@Autowired
	private BridgeParticipantDAO participantDAO;

	@Autowired
	private BridgeUserParticipantMappingDAO userParticipantMappingDAO;

	private Random random = new Random();

	public ParticipantDataIdMappingManagerImpl() {
	}

	// for unit testing only
	ParticipantDataIdMappingManagerImpl(BridgeParticipantDAO participantDAO, BridgeUserParticipantMappingDAO userParticipantMappingDAO,
			Random random) {
		this.participantDAO = participantDAO;
		this.userParticipantMappingDAO = userParticipantMappingDAO;
		this.random = random;
	}

	@Override
	public List<String> mapSynapseUserToParticipantIds(UserInfo userInfo) throws IOException, GeneralSecurityException {
		List<String> participantIdsForUser = userParticipantMappingDAO.getParticipantIdsForUser(userInfo.getId());
		return participantIdsForUser;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String createNewParticipantForUser(UserInfo alias) throws IOException, GeneralSecurityException {
		// we need to find a unique id
		long id;
		int retry = 0;
		for (;;) {
			id = random.nextLong();
			try {
				participantDAO.create(id);
				break;
			} catch (DatastoreException e) {
				if (++retry >= 10) {
					throw e;
				}
				continue;
			}
		}
		String idStr = Long.toString(id);
		List<String> participantIdsForUser = userParticipantMappingDAO.getParticipantIdsForUser(alias.getId());
		participantIdsForUser = Lists.newArrayList(participantIdsForUser);
		participantIdsForUser.add(idStr);
		userParticipantMappingDAO.setParticipantIdsForUser(alias.getId(), participantIdsForUser);
		return idStr;
	}
}
