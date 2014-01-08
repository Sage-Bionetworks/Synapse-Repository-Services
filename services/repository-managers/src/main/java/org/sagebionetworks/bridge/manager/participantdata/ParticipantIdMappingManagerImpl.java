package org.sagebionetworks.bridge.manager.participantdata;

import java.util.List;
import java.util.Random;

import org.sagebionetworks.bridge.model.BridgeParticipantDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class ParticipantIdMappingManagerImpl implements ParticipantDataIdManager {

	@Autowired
	private BridgeParticipantDAO participantDAO;

	Random random = new Random();

	@Override
	public List<String> mapSynapseUserToParticipantIds(UserInfo userInfo) {
		return Lists.newArrayList(Long.toString(Long.parseLong(userInfo.getIndividualGroup().getId()) ^ -1L));
	}

	@Override
	public String createNewParticipantForUser(UserInfo alias) {
		String id = Long.toString(Long.parseLong(alias.getIndividualGroup().getId()) ^ -1L);
		try {
			participantDAO.create(id);
		} catch (DatastoreException e) {
			// ignore, we don't have unique participants yet
		}
		return id;
	}
}
