/**
 * 
 */
package org.sagebionetworks.repo.manager.team;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * @author brucehoff
 *
 */
public class MembershipInvitationManagerImpl implements
		MembershipInvitationManager {

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#create(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.MembershipInvtnSubmission)
	 */
	@Override
	public MembershipInvtnSubmission create(UserInfo userInfo,
			MembershipInvtnSubmission mis) throws DatastoreException,
			InvalidModelException, UnauthorizedException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#get(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public MembershipInvtnSubmission get(UserInfo userInfo, String id)
			throws DatastoreException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#delete(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public void delete(UserInfo userInfo, String id) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#getOpenForUserInRange(java.lang.String, long, long)
	 */
	@Override
	public QueryResults<MembershipInvitation> getOpenForUserInRange(
			String principalId, long offset, long limit)
			throws DatastoreException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#getOpenForUserAndTeamInRange(java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public QueryResults<MembershipInvitation> getOpenForUserAndTeamInRange(
			String principalId, String teamId, long offset, long limit)
			throws DatastoreException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

}
