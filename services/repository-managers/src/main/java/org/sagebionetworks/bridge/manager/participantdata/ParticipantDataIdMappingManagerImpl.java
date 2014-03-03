package org.sagebionetworks.bridge.manager.participantdata;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Random;

import org.sagebionetworks.bridge.model.BridgeParticipantDAO;
import org.sagebionetworks.bridge.model.BridgeUserParticipantMappingDAO;
import org.sagebionetworks.bridge.model.ParticipantDataId;
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
	public List<ParticipantDataId> mapSynapseUserToParticipantIds(UserInfo userInfo) throws IOException, GeneralSecurityException {
		List<ParticipantDataId> participantIdsForUser = userParticipantMappingDAO.getParticipantIdsForUser(userInfo.getId());
		return participantIdsForUser;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public ParticipantDataId createNewParticipantIdForUser(UserInfo alias) throws IOException, GeneralSecurityException {
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
		ParticipantDataId participantDataId = new ParticipantDataId(id);
		List<ParticipantDataId> participantIdsForUser = userParticipantMappingDAO.getParticipantIdsForUser(alias.getId());
		participantIdsForUser = Lists.newArrayList(participantIdsForUser);
		participantIdsForUser.add(participantDataId);
		userParticipantMappingDAO.setParticipantIdsForUser(alias.getId(), participantIdsForUser);
		return participantDataId;
	}
}
