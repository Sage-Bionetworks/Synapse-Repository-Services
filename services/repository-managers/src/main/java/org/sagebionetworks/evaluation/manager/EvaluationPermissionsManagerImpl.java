package org.sagebionetworks.evaluation.manager;

import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public class EvaluationPermissionsManagerImpl implements EvaluationPermissionsManager {

	@Override
	public AccessControlList createAcl(UserInfo userInfo, AccessControlList acl)
			throws NotFoundException, DatastoreException,
			InvalidModelException, UnauthorizedException,
			ConflictingUpdateException {
		// TODO Auto-generated method stub
		return acl;
	}

	@Override
	public AccessControlList updateAcl(UserInfo userInfo, AccessControlList acl)
			throws NotFoundException, DatastoreException,
			InvalidModelException, UnauthorizedException,
			ConflictingUpdateException {
		// TODO Auto-generated method stub
		return acl;
	}

	@Override
	public void deleteAcl(UserInfo userInfo, String evalId)
			throws NotFoundException, DatastoreException,
			InvalidModelException, UnauthorizedException,
			ConflictingUpdateException {
		// TODO Auto-generated method stub
	}

	@Override
	public AccessControlList getAcl(UserInfo userInfo, String evalId)
			throws NotFoundException, DatastoreException,
			ACLInheritanceException {
		// TODO Auto-generated method stub
		AccessControlList acl = new AccessControlList();
		return acl;
	}

	@Override
	public boolean hasAccess(UserInfo userInfo, String evalId,
			ACCESS_TYPE accessType) throws NotFoundException,
			DatastoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public UserEvaluationPermissions getUserPermissionsForEvaluation(
			UserInfo userInfo, String evalId) throws NotFoundException,
			DatastoreException {
		return new UserEvaluationPermissions();
	}
}
