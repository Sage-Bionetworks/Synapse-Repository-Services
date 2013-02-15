package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AccessRequirementManagerImpl implements AccessRequirementManager {
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;		
	@Autowired
	private AccessApprovalDAO accessApprovalDAO;	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private AuthorizationManager authorizationManager;
	
	@Autowired
	NodeDAO nodeDAO;

	public static void validateAccessRequirement(AccessRequirement a) throws InvalidModelException {
		if (a.getEntityType()==null ||
				a.getAccessType()==null ||
				a.getEntityIds()==null) throw new InvalidModelException();
		
		if (!a.getEntityType().equals(a.getClass().getName())) throw new InvalidModelException("entity type differs from class");
	}
	
	public static void populateCreationFields(UserInfo userInfo, AccessRequirement a) {
		Date now = new Date();
		a.setCreatedBy(userInfo.getIndividualGroup().getId());
		a.setCreatedOn(now);
		a.setModifiedBy(userInfo.getIndividualGroup().getId());
		a.setModifiedOn(now);
	}

	public static void populateModifiedFields(UserInfo userInfo, AccessRequirement a) {
		Date now = new Date();
		a.setCreatedBy(null); // by setting to null we are telling the DAO to use the current values
		a.setCreatedOn(null);
		a.setModifiedBy(userInfo.getIndividualGroup().getId());
		a.setModifiedOn(now);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends AccessRequirement> T createAccessRequirement(UserInfo userInfo, T accessRequirement) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ForbiddenException {
		validateAccessRequirement(accessRequirement);
		ACTUtils.verifyACTTeamMembershipOrCanCreateOrEdit(
				userInfo, 
				accessRequirement.getEntityIds(),
				userGroupDAO, 
				authorizationManager);
		populateCreationFields(userInfo, accessRequirement);
		return accessRequirementDAO.create(accessRequirement);
	}

	@Override
	public QueryResults<AccessRequirement> getAccessRequirementsForEntity(UserInfo userInfo, String entityId) throws DatastoreException, NotFoundException, ForbiddenException {
		List<AccessRequirement> ars = accessRequirementDAO.getForNode(entityId);
		QueryResults<AccessRequirement> result = new QueryResults<AccessRequirement>(ars, ars.size());
		return result;
	}
	
	@Override
	public QueryResults<AccessRequirement> getUnmetAccessRequirements(UserInfo userInfo, String entityId) throws DatastoreException, NotFoundException {
		// first check if there *are* any unmet requirements.  (If not, no further queries will be executed.)
		List<Long> unmetIds = AccessRequirementUtil.unmetAccessRequirementIds(
				userInfo, entityId, ACCESS_TYPE.DOWNLOAD, nodeDAO, accessRequirementDAO); // TODO make access type a param
		
		List<AccessRequirement> unmetRequirements = new ArrayList<AccessRequirement>();
		// if there are any unmet requirements, retrieve the object(s)
		if (!unmetIds.isEmpty()) {
			List<AccessRequirement> allRequirementsForEntity = accessRequirementDAO.getForNode(entityId);
			for (Long unmetId : unmetIds) { // typically there will be just one id here
				for (AccessRequirement ar : allRequirementsForEntity) { // typically there will be just one id here
					if (ar.getId().equals(unmetId)) unmetRequirements.add(ar);
				}
			}
		}
		QueryResults<AccessRequirement> result = new QueryResults<AccessRequirement>(unmetRequirements, (int)unmetRequirements.size());
		return result;
	}	

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends AccessRequirement> T updateAccessRequirement(UserInfo userInfo, T accessRequirement) throws NotFoundException, UnauthorizedException, ConflictingUpdateException, InvalidModelException, ForbiddenException, DatastoreException {
		validateAccessRequirement(accessRequirement);
		ACTUtils.verifyACTTeamMembershipOrIsAdmin(userInfo, userGroupDAO);
		populateModifiedFields(userInfo, accessRequirement);
		return accessRequirementDAO.update(accessRequirement);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAccessRequirement(UserInfo userInfo,
			String accessRequirementId) throws NotFoundException,
			DatastoreException, UnauthorizedException, ForbiddenException {
		AccessRequirement accessRequirement = accessRequirementDAO.get(accessRequirementId);
		ACTUtils.verifyACTTeamMembershipOrIsAdmin(userInfo, userGroupDAO);
		accessRequirementDAO.delete(accessRequirement.getId().toString());
	}
}
