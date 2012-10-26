package org.sagebionetworks.competition.manager;

import java.util.Set;

import org.sagebionetworks.competition.model.Participant;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class ParticipantManagerImpl implements ParticipantManager {
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipant(String userId, String compId, String idToAdd) {
		return null;
		
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeParticipant(String userId, String compId, String idToRemove) {
		
	}
	
	@Override
	public Set<Participant> getAllParticipants(String userId, String compId) {
		return null;
		
	}
}
