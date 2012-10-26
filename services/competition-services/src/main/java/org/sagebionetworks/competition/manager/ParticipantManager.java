package org.sagebionetworks.competition.manager;

import java.util.Set;

import org.sagebionetworks.competition.model.Participant;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface ParticipantManager {

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipant(String userId, String compId,
			String idToAdd);

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeParticipant(String userId, String compId,
			String idToRemove);

	public Set<Participant> getAllParticipants(String userId, String compId);

}