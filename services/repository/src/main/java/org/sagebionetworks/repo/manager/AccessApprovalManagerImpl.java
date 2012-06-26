package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessApprovalType;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class AccessApprovalManagerImpl implements AccessApprovalManager {
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;
	
	@Autowired
	private AuthorizationManager authorizationManager;
	
	@Autowired
	private AccessApprovalDAO accessApprovalDAO;
	
	// this is the type of access the user needs to have on the owner Entity in order to be
	// able to administer (create, update, delete) the AccessApproval of the Entity
	public static final ACCESS_TYPE ADMINISTER_ACCESS_APPROVAL_ACCESS_TYPE = ACCESS_TYPE.CHANGE_PERMISSIONS;
	
	public static void validateAccessApproval(UserInfo userInfo, AccessApproval a) throws InvalidModelException, UnauthorizedException {
		if (a.getAccessorId()==null ||
				a.getApprovalType()==null || 
				a.getRequirementId()==null ) throw new InvalidModelException();
		
		if (a.getApprovalType().equals(AccessApprovalType.TOU_Agreement)) {
			if (!userInfo.isAdmin() && !userInfo.getIndividualGroup().getId().equals(a.getAccessorId()))
				throw new UnauthorizedException("A user may not sign Terms of Use on another's behalf");
		}
	}

	public static void populateCreationFields(UserInfo userInfo, AccessApproval a) {
		Date now = new Date();
		a.setCreatedBy(userInfo.getIndividualGroup().getId());
		a.setCreatedOn(now);
		a.setModifiedBy(userInfo.getIndividualGroup().getId());
		a.setModifiedOn(now);
	}

	public static void populateModifiedFields(UserInfo userInfo, AccessApproval a) {
		Date now = new Date();
		a.setCreatedBy(null);
		a.setCreatedOn(null);
		a.setModifiedBy(userInfo.getIndividualGroup().getId());
		a.setModifiedOn(now);
	}

	private String getEntityId(AccessApproval accessApproval) throws DatastoreException, NotFoundException {
		String requirementId = accessApproval.getRequirementId().toString();
		AccessRequirement accessRequirement = accessRequirementDAO.get(requirementId);
		return accessRequirement.getEntityId();		
	}

	@Override
	public <T extends AccessApproval> T createAccessApproval(UserInfo userInfo, T accessApproval) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException,ForbiddenException {
		validateAccessApproval(userInfo, accessApproval);
		String entityId = getEntityId(accessApproval);
		if (!authorizationManager.canAccess(userInfo, entityId, ADMINISTER_ACCESS_APPROVAL_ACCESS_TYPE)) {
			throw new ForbiddenException("You are not allowed to access the requested resource.");
		}
		populateCreationFields(userInfo, accessApproval);
		return accessApprovalDAO.create(accessApproval);
	}

	@Override
	public QueryResults<AccessApproval> getAccessApprovalsForEntity(
			UserInfo userInfo, String entityId) throws DatastoreException,
			NotFoundException, ForbiddenException {
		if (!authorizationManager.canAccess(userInfo, entityId, ACCESS_TYPE.READ)) {
			throw new ForbiddenException("You are not allowed to access the requested resource.");
		}
		List<AccessRequirement> ars = accessRequirementDAO.getForNode(entityId);
		List<String> arIds = new ArrayList<String>();
		for (AccessRequirement ar : ars) arIds.add(ar.getId().toString());
		List<String> principalIds = new ArrayList<String>();
		principalIds.add(userInfo.getIndividualGroup().getId());
		for (UserGroup ug : userInfo.getGroups()) principalIds.add(ug.getId());
		List<AccessApproval> aas = accessApprovalDAO.getForAccessRequirementsAndPrincipals(arIds, principalIds);
		QueryResults<AccessApproval> result = new QueryResults<AccessApproval>(aas, aas.size());
		return result;
	}

	@Override
	public <T extends AccessApproval> T  updateAccessApproval(UserInfo userInfo, T accessApproval) throws NotFoundException,
			DatastoreException, UnauthorizedException,
			ConflictingUpdateException, InvalidModelException, ForbiddenException {
		validateAccessApproval(userInfo, accessApproval);
		String entityId = getEntityId(accessApproval);
		if (!authorizationManager.canAccess(userInfo, entityId, ADMINISTER_ACCESS_APPROVAL_ACCESS_TYPE)) {
			throw new ForbiddenException("You are not allowed to access the requested resource.");
		}
		populateModifiedFields(userInfo, accessApproval);
		return accessApprovalDAO.update(accessApproval);
	}

	@Override
	public void deleteAccessApproval(UserInfo userInfo, String accessApprovalId)
			throws NotFoundException, DatastoreException, UnauthorizedException, ForbiddenException {
		AccessApproval accessApproval = accessApprovalDAO.get(accessApprovalId);
		String entityId = getEntityId(accessApproval);
		if (!authorizationManager.canAccess(userInfo, entityId, ADMINISTER_ACCESS_APPROVAL_ACCESS_TYPE)) {
			throw new ForbiddenException("You are not allowed to access the requested resource.");
		}
		accessApprovalDAO.delete(accessApproval.getId().toString());
	}

}
