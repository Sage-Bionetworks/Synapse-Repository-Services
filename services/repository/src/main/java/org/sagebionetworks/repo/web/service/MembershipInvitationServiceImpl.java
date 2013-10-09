/**
 * 
 */
package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * @author brucehoff
 *
 */
public class MembershipInvitationServiceImpl implements
		MembershipInvitationService {

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipInvitationService#create(java.lang.String, org.sagebionetworks.repo.model.MembershipInvtnSubmission)
	 */
	@Override
	public MembershipInvtnSubmission create(String userId,
			MembershipInvtnSubmission dto) throws UnauthorizedException,
			InvalidModelException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipInvitationService#getOpenInvitations(java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipInvitation> getOpenInvitations(
			String inviteeId, String teamId, long limit, long offset)
			throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipInvitationService#get(java.lang.String, java.lang.String)
	 */
	@Override
	public MembershipInvtnSubmission get(String userId, String dtoId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipInvitationService#delete(java.lang.String, java.lang.String)
	 */
	@Override
	public void delete(String userId, String dtoId) throws DatastoreException,
			UnauthorizedException {
		// TODO Auto-generated method stub

	}

}
