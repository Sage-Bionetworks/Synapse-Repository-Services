package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.AccessApprovalDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.AccessRequirementDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RestrictionInformationManagerImpl implements RestrictionInformationManager {
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;

	@Autowired
	private AccessApprovalDAO accessApprovalDAO;
	
	@Autowired
	private NodeDAO nodeDao;
	

	@Override
	public RestrictionInformationResponse getRestrictionInformation(UserInfo userInfo, RestrictionInformationRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getObjectId(), "RestrictionInformationRequest.objectId");
		ValidateArgument.required(request.getRestrictableObjectType(), "RestrictionInformationRequest.restrictableObjectType");
		RestrictionInformationResponse info = new RestrictionInformationResponse();

		boolean userIsFileCreator=false;
		if (RestrictableObjectType.ENTITY == request.getRestrictableObjectType()) {
			// if the user is the owner of the entity (and the entity is a File), then she automatically 
			// has access to the object and therefore has no unmet access requirements
			Long principalId = userInfo.getId();
			Node node = nodeDao.getNode(request.getObjectId());
			if (node.getCreatedByPrincipalId().equals(principalId) && EntityType.file.equals(node.getNodeType())) {
				userIsFileCreator=true;
			}
		}
		
		List<Long> subjectIds;
		if (RestrictableObjectType.ENTITY == request.getRestrictableObjectType()) {
			subjectIds = nodeDao.getEntityPathIds(request.getObjectId());
		} else if (RestrictableObjectType.TEAM == request.getRestrictableObjectType()){
			subjectIds = Arrays.asList(KeyFactory.stringToKey(request.getObjectId()));
		} else {
			throw new IllegalArgumentException("Do not support retrieving restriction information for type: "+request.getRestrictableObjectType());
		}
		AccessRequirementStats stats = accessRequirementDAO.getAccessRequirementStats(subjectIds, request.getRestrictableObjectType());
		if (stats.getRequirementIdSet().isEmpty()) {
			info.setRestrictionLevel(RestrictionLevel.OPEN);
			info.setHasUnmetAccessRequirement(false);
		} else {
			if (stats.getHasACT() || stats.getHasLock()) {
				info.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT);
			} else if (stats.getHasToU()) {
				info.setRestrictionLevel(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE);
			} else {
				throw new IllegalStateException("Access Requirement does not contain either ACT or ToU: "+stats.getRequirementIdSet().toString());
			}
			if (userIsFileCreator) {
				info.setHasUnmetAccessRequirement(false);
			} else {
				info.setHasUnmetAccessRequirement(accessApprovalDAO.hasUnmetAccessRequirement(stats.getRequirementIdSet(), userInfo.getId().toString()));
			}
		}
		return info;
	}



}
