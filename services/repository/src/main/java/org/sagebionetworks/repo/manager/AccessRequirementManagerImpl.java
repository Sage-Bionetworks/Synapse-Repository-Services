package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
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

public class AccessRequirementManagerImpl implements AccessRequirementManager {
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;
	
	@Autowired
	private AuthorizationManager authorizationManager;
	
	@Autowired
	private AccessApprovalDAO accessApprovalDAO;
	
	// this is the type of access the user needs to have on the owner Entity in order to be
	// able to administer (create, update, delete) the AccessRequirements of the Entity
	public static final ACCESS_TYPE ADMINISTER_ACCESS_REQUIREMENTS_ACCESS_TYPE = ACCESS_TYPE.CHANGE_PERMISSIONS;
	
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
	
	private void verifyAccess(UserInfo userInfo, AccessRequirement accessRequirement, ACCESS_TYPE accessType) throws DatastoreException, NotFoundException, ForbiddenException {
		List<String> entityIds = accessRequirement.getEntityIds();
		List<String> lackAccess = new ArrayList<String>();
		for (String entityId : entityIds) {
			if (!authorizationManager.canAccess(userInfo, entityId, accessType)) lackAccess.add(entityId);
		}
		if (!lackAccess.isEmpty()) {
			throw new ForbiddenException("Based on your permission levels on these entities: "+lackAccess+
					", you are forbidden from accessing the requested resource.");
		}
	}

	@Override
	public <T extends AccessRequirement> T createAccessRequirement(UserInfo userInfo, T accessRequirement) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ForbiddenException {
		validateAccessRequirement(accessRequirement);
		verifyAccess(userInfo, accessRequirement, ADMINISTER_ACCESS_REQUIREMENTS_ACCESS_TYPE);
		populateCreationFields(userInfo, accessRequirement);
		return accessRequirementDAO.create(accessRequirement);
	}

	@Override
	public QueryResults<AccessRequirement> getAccessRequirementsForEntity(UserInfo userInfo, String entityId) throws DatastoreException, NotFoundException, ForbiddenException {
		if (!authorizationManager.canAccess(userInfo, entityId, ACCESS_TYPE.READ)) {
			throw new ForbiddenException("You are not allowed to access the requested resource.");
		}
		List<AccessRequirement> ars = accessRequirementDAO.getForNode(entityId);
		QueryResults<AccessRequirement> result = new QueryResults<AccessRequirement>(ars, ars.size());
		return result;
	}
	
	@Override
	public QueryResults<AccessRequirement> getUnmetAccessRequirementIntern(UserInfo userInfo, String entityId) throws DatastoreException, NotFoundException {
		List<AccessRequirement> ars = accessRequirementDAO.getForNode(entityId);
		Map<String, AccessRequirement> arIds = new HashMap<String, AccessRequirement>();
		for (AccessRequirement ar : ars) arIds.put(ar.getId().toString(), ar);
		List<String> principalIds = new ArrayList<String>();
		principalIds.add(userInfo.getIndividualGroup().getId());
		for (UserGroup ug : userInfo.getGroups()) principalIds.add(ug.getId());
		List<AccessApproval> aas = accessApprovalDAO.getForAccessRequirementsAndPrincipals(arIds.keySet(), principalIds);
		Set<String> aaRequirementIds = new HashSet<String>(); // this should be a subset of arIds
		for (AccessApproval aa : aas) {
			if (!arIds.keySet().contains(aa.getRequirementId().toString())) throw new DatastoreException("Approval "+aa.getId()+
					" references requirement "+aa.getRequirementId()+" which does not belong to "+entityId);
			aaRequirementIds.add(aa.getRequirementId().toString());
		}
		// now find out what values in arIds are NOT in aaRequirementIds.  These are the unmet requirements
		Set<String> unmetRequirementIds = new HashSet<String>(arIds.keySet());
		unmetRequirementIds.removeAll(aaRequirementIds);
		List<AccessRequirement> unmetRequirements = new ArrayList<AccessRequirement>();
		for (String rId : unmetRequirementIds) unmetRequirements.add(arIds.get(rId));
		QueryResults<AccessRequirement> result = new QueryResults<AccessRequirement>(unmetRequirements, (int)unmetRequirements.size());
		return result;
	}	

	@Override
	public QueryResults<AccessRequirement> getUnmetAccessRequirements(UserInfo userInfo, String entityId) throws DatastoreException, NotFoundException, ForbiddenException {
		if (!authorizationManager.canAccess(userInfo, entityId, ACCESS_TYPE.READ)) {
			throw new ForbiddenException("You are not allowed to access the requested resource.");
		}
		return getUnmetAccessRequirementIntern(userInfo, entityId);
	}

	@Override
	public <T extends AccessRequirement> T updateAccessRequirement(UserInfo userInfo, T accessRequirement) throws NotFoundException, UnauthorizedException, ConflictingUpdateException, InvalidModelException, ForbiddenException, DatastoreException {
		validateAccessRequirement(accessRequirement);
		verifyAccess(userInfo, accessRequirement, ADMINISTER_ACCESS_REQUIREMENTS_ACCESS_TYPE);
		populateModifiedFields(userInfo, accessRequirement);
		return accessRequirementDAO.update(accessRequirement);
	}

	@Override
	public void deleteAccessRequirement(UserInfo userInfo,
			String accessRequirementId) throws NotFoundException,
			DatastoreException, UnauthorizedException, ForbiddenException {
		AccessRequirement accessRequirement = accessRequirementDAO.get(accessRequirementId);
		verifyAccess(userInfo, accessRequirement, ADMINISTER_ACCESS_REQUIREMENTS_ACCESS_TYPE);
		accessRequirementDAO.delete(accessRequirement.getId().toString());
	}
}
