/**
 * 
 */
package org.sagebionetworks.repo.manager.team;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * @author brucehoff
 *
 */
public class MembershipRequestManagerImpl implements MembershipRequestManager {

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#create(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.MembershipRqstSubmission)
	 */
	@Override
	public MembershipRqstSubmission create(UserInfo userInfo,
			MembershipRqstSubmission mrs) throws DatastoreException,
			InvalidModelException, UnauthorizedException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#get(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public MembershipRqstSubmission get(UserInfo userInfo, String id)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#delete(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public void delete(UserInfo userInfo, String id) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#getOpenByTeamInRange(java.lang.String, long, long)
	 */
	@Override
	public QueryResults<MembershipRequest> getOpenByTeamInRange(
			String principalId, long offset, long limit)
			throws DatastoreException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#getOpenByTeamAndRequestorInRange(java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public QueryResults<MembershipRequest> getOpenByTeamAndRequestorInRange(
			String principalId, String teamId, long offset, long limit)
			throws DatastoreException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

}
