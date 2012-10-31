package org.sagebionetworks.competition.manager;

import java.util.List;

import org.sagebionetworks.competition.dao.CompetitionDAO;
import org.sagebionetworks.competition.dao.ParticipantDAO;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class ParticipantManagerImpl {
	
	@Autowired
	CompetitionDAO competitionDAO;
	@Autowired
	ParticipantDAO participantDAO;
	@Autowired
	UserManager userManager;
	
	// for testing purposes
	protected ParticipantManagerImpl(CompetitionDAO competitionDAO, 
			ParticipantDAO participantDAO, UserManager userManager) {
		this.competitionDAO = competitionDAO;
		this.participantDAO = participantDAO;
		this.userManager = userManager;
	}
	
	public Participant getParticipant(String userId, String compId) throws DatastoreException, NotFoundException {
		Participant part = participantDAO.get(userId, compId);
		part.setName(userManager.getDisplayName(Long.parseLong(userId)));
		// TODO: part.setScore()
		return part;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipant(String userId, String compId, String idToAdd) throws NotFoundException {
		// TODO: verify userID has admin permissions
		
		
		// ensure user exists
		if (userManager.getDisplayName(Long.parseLong(idToAdd)) == null)
			throw new NotFoundException("User ID: " + idToAdd + " does not exist");
		
		// ensure competition exists
		if (!competitionDAO.doesIdExist(compId))
			throw new NotFoundException("Competition ID: " + compId + " does not exist");

		Participant part = new Participant();
		part.setCompetitionId(compId);
		part.setUserId(userId);
		
		participantDAO.create(part);
		return getParticipant(userId, compId);
	}
	

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeParticipant(String userId, String compId, String idToRemove) {
		
	}
	

	public List<Participant> getAllParticipants(String userId, String compId) {
		return null;
		
	}
}
