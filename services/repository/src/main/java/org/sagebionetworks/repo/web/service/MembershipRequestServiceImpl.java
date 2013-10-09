/**
 * 
 */
package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * @author brucehoff
 *
 */
public class MembershipRequestServiceImpl implements MembershipRequestService {

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipRequestService#create(java.lang.String, org.sagebionetworks.repo.model.MembershipRqstSubmission)
	 */
	@Override
	public MembershipRqstSubmission create(String userId,
			MembershipRqstSubmission dto) throws UnauthorizedException,
			InvalidModelException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipRequestService#getOpenRequests(java.lang.String, java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipRequest> getOpenRequests(String userId,
			String requestorId, String teamId, long limit, long offset)
			throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipRequestService#get(java.lang.String, java.lang.String)
	 */
	@Override
	public MembershipRqstSubmission get(String userId, String dtoId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.MembershipRequestService#delete(java.lang.String, java.lang.String)
	 */
	@Override
	public void delete(String userId, String dtoId) throws DatastoreException,
			UnauthorizedException {
		// TODO Auto-generated method stub

	}

}
