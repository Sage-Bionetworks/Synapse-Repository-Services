package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessApprovalManager {
	
	/**
	 *  create access approval
	 * @throws ForbiddenException 
	 */
	public <T extends AccessApproval> T createAccessApproval(UserInfo userInfo, T accessApproval) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ForbiddenException;
	
	/**
	 *  get all the access approvals for an entity
	 * @throws ForbiddenException 
	 */
	public QueryResults<AccessApproval> getAccessApprovalsForSubject(UserInfo userInfo, RestrictableObjectDescriptor subjectId) throws DatastoreException, NotFoundException, ForbiddenException;
	
	/**
	 *  update an access approval
	 * @throws ForbiddenException 
	 */
	public <T extends AccessApproval> T  updateAccessApproval(UserInfo userInfo, T accessApproval) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException, InvalidModelException, ForbiddenException;
	
	/*
	 *  delete an access approval
	 */
	public void deleteAccessApproval(UserInfo userInfo, String AccessApprovalId) throws NotFoundException, DatastoreException, UnauthorizedException, ForbiddenException;
}
