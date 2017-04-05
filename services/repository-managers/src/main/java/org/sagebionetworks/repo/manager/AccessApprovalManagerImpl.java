package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessApproval;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PostMessageContentAccessApproval;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import org.sagebionetworks.repo.transactions.WriteTransaction;

public class AccessApprovalManagerImpl implements AccessApprovalManager {
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;
	@Autowired
	private AccessApprovalDAO accessApprovalDAO;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private NodeDAO nodeDao;
	
	// check an incoming object (i.e. during 'create' and 'update')
	private void validateAccessApproval(UserInfo userInfo, AccessApproval a) throws 
	InvalidModelException, UnauthorizedException, DatastoreException, NotFoundException {
		if (a.getAccessorId()==null || a.getRequirementId()==null) throw new InvalidModelException();

		// make sure the approval matches the requirement
		AccessRequirement ar = accessRequirementDAO.get(a.getRequirementId().toString());
		if (((ar instanceof TermsOfUseAccessRequirement) && !(a instanceof TermsOfUseAccessApproval))
			|| ((ar instanceof PostMessageContentAccessRequirement) && !(a instanceof PostMessageContentAccessApproval))
			|| ((ar instanceof ACTAccessRequirement) && !(a instanceof ACTAccessApproval))) {
			throw new InvalidModelException("Cannot apply an approval of type "+a.getClass().getSimpleName()+" to an access requirement of type "+ar.getClass().getSimpleName());
		}
		
	}

	public static void populateCreationFields(UserInfo userInfo, AccessApproval a) {
		Date now = new Date();
		a.setCreatedBy(userInfo.getId().toString());
		a.setCreatedOn(now);
		a.setModifiedBy(userInfo.getId().toString());
		a.setModifiedOn(now);
	}

	public static void populateModifiedFields(UserInfo userInfo, AccessApproval a) {
		Date now = new Date();
		a.setCreatedBy(null); // by setting to null we are telling the DAO to use the current values
		a.setCreatedOn(null);
		a.setModifiedBy(userInfo.getId().toString());
		a.setModifiedOn(now);
	}
	
	@Override
	public AccessApproval getAccessApproval(UserInfo userInfo, String approvalId) 
			throws DatastoreException, NotFoundException {
		return accessApprovalDAO.get(approvalId);
	}


	@WriteTransaction
	@Override
	public <T extends AccessApproval> T createAccessApproval(UserInfo userInfo, T accessApproval) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException {
		
		if (accessApproval instanceof TermsOfUseAccessApproval) {
			// fill in the user's identity
			accessApproval.setAccessorId(userInfo.getId().toString());
		}
		
		validateAccessApproval(userInfo, accessApproval);

		AuthorizationManagerUtil.checkAuthorizationAndThrowException(authorizationManager.canCreateAccessApproval(userInfo, accessApproval));

		
		populateCreationFields(userInfo, accessApproval);
		return accessApprovalDAO.create(accessApproval);
	}

	@Deprecated
	@Override
	public List<AccessApproval> getAccessApprovalsForSubject(
			UserInfo userInfo, RestrictableObjectDescriptor subjectId) throws DatastoreException,
			NotFoundException, UnauthorizedException {
		
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(authorizationManager.canAccessAccessApprovalsForSubject(userInfo, subjectId, ACCESS_TYPE.READ));
		

		List<String> subjectIds = new ArrayList<String>();
		if (RestrictableObjectType.ENTITY==subjectId.getType()) {
			subjectIds.addAll(AccessRequirementUtil.getNodeAncestorIds(nodeDao, subjectId.getId(), true));
		} else {
			subjectIds.add(subjectId.getId());
		}
		List<AccessRequirement> ars = accessRequirementDAO.getAllAccessRequirementsForSubject(subjectIds, subjectId.getType());
		List<AccessApproval> aas = new ArrayList<AccessApproval>();
		for (AccessRequirement ar : ars) {
			aas.addAll(accessApprovalDAO.getForAccessRequirement(ar.getId().toString()));
		}
		return aas;
	}

	@WriteTransaction
	@Override
	public <T extends AccessApproval> T  updateAccessApproval(UserInfo userInfo, T accessApproval) throws NotFoundException,
			DatastoreException, UnauthorizedException,
			ConflictingUpdateException, InvalidModelException {
		
		if (accessApproval instanceof TermsOfUseAccessApproval) {
			// fill in the user's identity
			accessApproval.setAccessorId(userInfo.getId().toString());
		}
		
		validateAccessApproval(userInfo, accessApproval);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(authorizationManager.canAccess(userInfo, accessApproval.getId().toString(), ObjectType.ACCESS_APPROVAL, ACCESS_TYPE.UPDATE));
		populateModifiedFields(userInfo, accessApproval);
		return accessApprovalDAO.update(accessApproval);
	}

	@WriteTransaction
	@Override
	public void deleteAccessApproval(UserInfo userInfo, String accessApprovalId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		AccessApproval accessApproval = accessApprovalDAO.get(accessApprovalId);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, accessApproval.getId().toString(), 
						ObjectType.ACCESS_APPROVAL, ACCESS_TYPE.DELETE));
			
		accessApprovalDAO.delete(accessApproval.getId().toString());
	}

	@WriteTransaction
	@Override
	public void deleteAccessApprovals(UserInfo userInfo, String accessRequirementId, String accessorId)
			throws UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(accessorId, "accessorId");
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member may delete an access approval.");
		}
		AccessRequirement accessRequirement = accessRequirementDAO.get(accessRequirementId);
		ValidateArgument.requirement(accessRequirement.getConcreteType().equals(ACTAccessRequirement.class.getName()),
				"Do not support access approval deletion for access requirement type: "+accessRequirement.getConcreteType());
		accessApprovalDAO.delete(accessRequirementId, accessorId);
	}
}
