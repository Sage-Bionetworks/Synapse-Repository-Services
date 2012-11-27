package org.sagebionetworks.competition.manager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.competition.dao.ParticipantDAO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.competition.util.CompetitionUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class ParticipantManagerImpl implements ParticipantManager {
	
	@Autowired
	ParticipantDAO participantDAO;
	@Autowired
	UserManager userManager;
	@Autowired
	CompetitionManager competitionManager;
	
	// for testing purposes
	protected ParticipantManagerImpl(ParticipantDAO participantDAO, 
			UserManager userManager, CompetitionManager competitionManager) {		
		this.participantDAO = participantDAO;
		this.userManager = userManager;
		this.competitionManager = competitionManager;
	}
	
	@Override
	public Participant getParticipant(String userId, String compId) throws DatastoreException, NotFoundException {
		CompetitionUtils.ensureNotNull(userId, compId);
		Participant part = participantDAO.get(userId, compId);
		part.setName(userManager.getDisplayName(Long.parseLong(userId)));
		// TODO: part.setScore()
		return part;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipant(String userId, String compId, String idToAdd) throws NotFoundException {
		CompetitionUtils.ensureNotNull(userId, "Requesting User's ID");
		CompetitionUtils.ensureNotNull(compId, "Competition ID");
		CompetitionUtils.ensureNotNull(idToAdd, "Participant user ID");
		
		// ensure user exists
		if (userManager.getDisplayName(Long.parseLong(idToAdd)) == null)
			throw new NotFoundException("User ID: " + idToAdd + " does not exist");
		
		// verify permissions
		Competition comp = competitionManager.getCompetition(compId);
		if (!competitionManager.isCompAdmin(userId, comp))	{
			// user is not an admin; only authorized to add self as a Participant
			CompetitionUtils.ensureCompetitionIsOpen(comp);
			if (!userId.equals(idToAdd))
				throw new UnauthorizedException("User ID: " + userId + " is not authorized to add other users to Competition ID: " + compId);
		}
			
		// create and return the new Participant
		Participant part = new Participant();
		part.setCompetitionId(compId);
		part.setUserId(idToAdd);		
		participantDAO.create(part);
		return getParticipant(idToAdd, compId);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeParticipant(String userId, String compId, String idToRemove) throws DatastoreException, NotFoundException {
		CompetitionUtils.ensureNotNull(userId, "Requesting User's ID");
		CompetitionUtils.ensureNotNull(compId, "Competition ID");
		CompetitionUtils.ensureNotNull(idToRemove, "Participant User ID");
		
		// verify permissions	
		Competition comp = competitionManager.getCompetition(compId);
		if (!competitionManager.isCompAdmin(userId, comp))	{
			// user is not an admin; only authorized to cancel their own participation
			CompetitionUtils.ensureCompetitionIsOpen(comp);
			if (!userId.equals(idToRemove))
				throw new UnauthorizedException("User ID: " + userId + " is not authorized to remove other users from Competition ID: " + compId);
		}
		participantDAO.delete(idToRemove, compId);
	}
	
	@Override
	public Set<Participant> getAllParticipants(String userId, String compId) throws NumberFormatException, DatastoreException, NotFoundException {
		CompetitionUtils.ensureNotNull(userId, compId);
		Set<Participant> participants = new HashSet<Participant>();
		List<Participant> fromDAO = participantDAO.getAllByCompetition(compId);
		for (Participant p : fromDAO) {
			p.setName(userManager.getDisplayName(Long.parseLong(p.getUserId())));
			participants.add(p);
		}
		return participants;
	}
	
	@Override
	public long getNumberofParticipants(String compId) throws DatastoreException, NotFoundException {
		CompetitionUtils.ensureNotNull(compId, "Competition ID");
		return participantDAO.getCountByCompetition(compId);
	}
}
