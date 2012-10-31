package org.sagebionetworks.competition.manager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.competition.dao.CompetitionDAO;
import org.sagebionetworks.competition.dao.ParticipantDAO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
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
	@Autowired
	CompetitionManager competitionManager;
	
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
		// ensure user exists
		if (userManager.getDisplayName(Long.parseLong(idToAdd)) == null)
			throw new NotFoundException("User ID: " + idToAdd + " does not exist");
		
		// ensure competition exists and that we can add participants	
		Competition comp = competitionDAO.get(compId);
		if (comp == null) {
			// competition does not exist
			throw new NotFoundException("Competition ID: " + compId + " does not exist");
		}
		if (!competitionManager.isCompAdmin(userId, comp))	{
			// user is not an admin; only authorized to add self as a Participant
			if (comp.getStatus() != CompetitionStatus.OPEN)
				throw new IllegalStateException("Competition ID: " + compId + " is not currently open");
			if (userId.equals(idToAdd))
				throw new UnauthorizedException("User ID: " + userId + " is not authorized to add other users to Competition ID: " + compId);
		}
			
		// create and return the new Participant
		Participant part = new Participant();
		part.setCompetitionId(compId);
		part.setUserId(idToAdd);		
		participantDAO.create(part);
		return getParticipant(idToAdd, compId);
	}
	

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeParticipant(String userId, String compId, String idToRemove) throws DatastoreException, NotFoundException {
		participantDAO.delete(idToRemove, compId);
	}
	

	public Set<Participant> getAllParticipants(String userId, String compId) throws NumberFormatException, DatastoreException, NotFoundException {
		Set<Participant> participants = new HashSet<Participant>();
		List<Participant> fromDAO = participantDAO.getAllByCompetition(compId);
		for (Participant p : fromDAO) {
			p.setName(userManager.getDisplayName(Long.parseLong(p.getUserId())));
			participants.add(p);
		}
		return participants;
	}
}
