package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.entity.EntityStateProvider;
import org.sagebionetworks.repo.manager.entity.LazyEntityStateProvider;
import org.sagebionetworks.repo.manager.util.UserAccessRestrictionUtils;
import org.sagebionetworks.repo.model.RestrictionFulfillment;
import org.sagebionetworks.repo.model.RestrictionInformationBatchRequest;
import org.sagebionetworks.repo.model.RestrictionInformationBatchResponse;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.AccessRestrictionStatusDao;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.repo.model.dbo.entity.UsersEntityPermissionsDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RestrictionInformationManagerImpl implements RestrictionInformationManager {

	@Autowired
	private AccessRestrictionStatusDao accessRestrictionStatusDao;
	
	@Autowired
	private UsersEntityPermissionsDao usersEntityPermissionsDao;	
	
	@Override
	public RestrictionInformationResponse getRestrictionInformation(UserInfo userInfo,
			RestrictionInformationRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getObjectId(), "RestrictionInformationRequest.objectId");
		ValidateArgument.required(request.getRestrictableObjectType(), "RestrictionInformationRequest.restrictableObjectType");
		
		RestrictionInformationBatchRequest batchRequest = new RestrictionInformationBatchRequest()
			.setRestrictableObjectType(request.getRestrictableObjectType())
			.setObjectIds(List.of(request.getObjectId()));
		
		RestrictionInformationBatchResponse batchResponse = getRestrictionInformationBatch(userInfo, batchRequest);
		
		if (batchResponse.getRestrictionInformation().size() != 1) {
			throw new IllegalStateException("Could not fetch restriction information for object " + request.getObjectId() + " of type " + request.getRestrictableObjectType());
		}
		
		return batchResponse.getRestrictionInformation().get(0);
	}
	
	@Override
	public RestrictionInformationBatchResponse getRestrictionInformationBatch(UserInfo userInfo, RestrictionInformationBatchRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.requiredNotEmpty(request.getObjectIds(), "The objectIds");
		ValidateArgument.required(request.getRestrictableObjectType(), "The restrictableObjectType");
		
		switch (request.getRestrictableObjectType()) {
		case ENTITY:
			return getEntityRestrictionInformationBatchResponse(userInfo, request);
		case TEAM:
			return getTeamRestrictionInformationBatchResponse(userInfo, request);
		default:
			throw new IllegalArgumentException("Unsupported type: " + request.getRestrictableObjectType());

	}
	}

	RestrictionInformationBatchResponse getEntityRestrictionInformationBatchResponse(UserInfo userInfo, RestrictionInformationBatchRequest request) {
		List<Long> objectIds = KeyFactory.stringToKey(request.getObjectIds());
		
		EntityStateProvider stateProvider = new LazyEntityStateProvider(accessRestrictionStatusDao, usersEntityPermissionsDao, userInfo, objectIds);
		
		List<RestrictionInformationResponse> restrictionList = objectIds.stream().map( objectId -> {
			UsersRestrictionStatus restrictionStatus = stateProvider.getRestrictionStatus(objectId);
			UserEntityPermissionsState permissionsState = stateProvider.getPermissionsState(objectId);
			
			boolean isUserDataContributor = UserAccessRestrictionUtils.isUserDataContributor(permissionsState);
			
			RestrictionInformationResponse response = new RestrictionInformationResponse()
				.setObjectId(KeyFactory.keyToString(objectId))
				.setIsUserDataContributor(isUserDataContributor);
				
			// If there are no restrictions then the data is open and ARs are met.
			if (restrictionStatus.getAccessRestrictions().isEmpty()) {
				response
					.setHasUnmetAccessRequirement(false)
					.setRestrictionLevel(RestrictionLevel.OPEN)
					.setRestrictionDetails(Collections.emptyList());
			} else {
			
				Set<Long> unmetARIdList = new HashSet<>(UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForEntity(permissionsState, restrictionStatus));
				
				List<RestrictionFulfillment> restrictionDetails = restrictionStatus.getAccessRestrictions().stream().map( userRequirementStatus -> new RestrictionFulfillment()
					.setAccessRequirementId(userRequirementStatus.getRequirementId())
					.setIsApproved(!userRequirementStatus.isUnmet())
					.setIsExempt(UserAccessRestrictionUtils.isUserExempt(userRequirementStatus, isUserDataContributor))
					.setIsMet(!unmetARIdList.contains(userRequirementStatus.getRequirementId()))
				).collect(Collectors.toList());
			
				response
					.setHasUnmetAccessRequirement(!unmetARIdList.isEmpty())
					.setRestrictionLevel(restrictionStatus.getMostRestrictiveLevel())
					.setRestrictionDetails(restrictionDetails);
			}
			
			return response;
			
		}).collect(Collectors.toList());
		
		return new RestrictionInformationBatchResponse().setRestrictionInformation(restrictionList);
	}

	RestrictionInformationBatchResponse getTeamRestrictionInformationBatchResponse(UserInfo userInfo, RestrictionInformationBatchRequest request) {
		List<Long> objectIds = KeyFactory.stringToKey(request.getObjectIds());
		
		List<RestrictionInformationResponse> restrictionList = accessRestrictionStatusDao.getNonEntityStatus(objectIds, request.getRestrictableObjectType(), userInfo.getId()).stream().map( restrictionStatus -> {
			
			RestrictionInformationResponse response = new RestrictionInformationResponse()
					.setObjectId(restrictionStatus.getSubjectId().toString())
					// Does not apply for teams
					.setIsUserDataContributor(false);
					
			// If there are no restrictions then the team is open and ARs are met.
			if (restrictionStatus.getAccessRestrictions().isEmpty()) {
				response
					.setHasUnmetAccessRequirement(false)
					.setRestrictionLevel(RestrictionLevel.OPEN)
					.setRestrictionDetails(Collections.emptyList());
			} else {
			
				Set<Long> unmetARIdList = new HashSet<>(UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForNonEntity(restrictionStatus));
				
				List<RestrictionFulfillment> restrictionDetails = restrictionStatus.getAccessRestrictions().stream().map(userRequirementStatus -> new RestrictionFulfillment()
					.setAccessRequirementId(userRequirementStatus.getRequirementId())
					.setIsApproved(!userRequirementStatus.isUnmet())
					.setIsMet(!unmetARIdList.contains(userRequirementStatus.getRequirementId()))
					// Does not apply for teams
					.setIsExempt(false)
				).collect(Collectors.toList());
				
				response
					.setHasUnmetAccessRequirement(!unmetARIdList.isEmpty())
					.setRestrictionLevel(restrictionStatus.getMostRestrictiveLevel())
					.setRestrictionDetails(restrictionDetails);
			}
			
			return response;
		}).collect(Collectors.toList());
		
		return new RestrictionInformationBatchResponse().setRestrictionInformation(restrictionList);
	}
}
