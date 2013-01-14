package org.sagebionetworks.competition.manager;

import java.util.List;
import org.sagebionetworks.competition.dao.ParticipantDAO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.competition.util.CompetitionUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
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
	
	public ParticipantManagerImpl() {};
	
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
		return participantDAO.get(userId, compId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipant(UserInfo userInfo, String compId) throws NotFoundException {
		CompetitionUtils.ensureNotNull(compId, "Competition ID");
		UserInfo.validateUserInfo(userInfo);
		String principalIdToAdd = userInfo.getIndividualGroup().getId();
		
		// create the new Participant
		Participant part = new Participant();
		part.setCompetitionId(compId);
		part.setUserId(principalIdToAdd);
		participantDAO.create(part);
		
		// trigger etag update of the parent Competition
		// this is required for migration consistency
		competitionManager.updateCompetitionEtag(compId);
		
		return getParticipant(principalIdToAdd, compId);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipantAsAdmin(UserInfo userInfo, String compId, String principalIdToAdd) throws NotFoundException {
		CompetitionUtils.ensureNotNull(compId, "Competition ID");
		CompetitionUtils.ensureNotNull(principalIdToAdd, "Principal ID");
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		
		// verify permissions
		Competition comp = competitionManager.getCompetition(compId);

		// add other user (requires admin rights)
		if (!competitionManager.isCompAdmin(principalId, compId)) {
			CompetitionUtils.ensureCompetitionIsOpen(comp);
			throw new UnauthorizedException("User Principal ID: " + principalId + " is not authorized to add other users to Competition ID: " + compId);
		}
			
		// create the new Participant
		Participant part = new Participant();
		part.setCompetitionId(compId);
		part.setUserId(principalIdToAdd);
		participantDAO.create(part);
		
		// trigger etag update of the parent Competition
		// this is required for migration consistency
		competitionManager.updateCompetitionEtag(compId);
		
		return getParticipant(principalIdToAdd, compId);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeParticipant(UserInfo userInfo, String compId, String idToRemove) throws DatastoreException, NotFoundException {
		CompetitionUtils.ensureNotNull(compId, "Competition ID");
		CompetitionUtils.ensureNotNull(idToRemove, "Participant User ID");
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		
		// verify permissions
		if (!competitionManager.isCompAdmin(principalId, compId)) {
			// user is not an admin; only authorized to cancel their own participation
			CompetitionUtils.ensureCompetitionIsOpen(competitionManager.getCompetition(compId));
			if (!principalId.equals(idToRemove))
				throw new UnauthorizedException("User Principal ID: " + principalId + " is not authorized to remove other users from Competition ID: " + compId);
		}
		
		// trigger etag update of the parent Competition
		// this is required for migration consistency
		competitionManager.updateCompetitionEtag(compId);
		
		participantDAO.delete(idToRemove, compId);
	}
	
	@Override
	public List<Participant> getAllParticipants(String compId) throws NumberFormatException, DatastoreException, NotFoundException {
		CompetitionUtils.ensureNotNull(compId, "Competition ID");
		return participantDAO.getAllByCompetition(compId);
	}
	
	@Override
	public long getNumberofParticipants(String compId) throws DatastoreException, NotFoundException {
		CompetitionUtils.ensureNotNull(compId, "Competition ID");
		return participantDAO.getCountByCompetition(compId);
	}
}
