package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
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
		ValidateArgument.required(userInfo, "The userInfo");
		ValidateArgument.required(request, "The request");
		ValidateArgument.requiredNotEmpty(request.getObjectIds(), "The objectIds");
		ValidateArgument.requirement(request.getObjectIds().size() <= MAX_BATCH_SIZE, "The maximum number of allowed object ids is " + MAX_BATCH_SIZE + ".");
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
			
			Supplier<List<Long>> unmetArIdsSupplier = () -> UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForEntity(permissionsState, restrictionStatus);
			
			return buildRestrictionInformationResponse(restrictionStatus, isUserDataContributor, unmetArIdsSupplier);
			
		}).collect(Collectors.toList());
		
		return new RestrictionInformationBatchResponse().setRestrictionInformation(restrictionList);
	}

	RestrictionInformationBatchResponse getTeamRestrictionInformationBatchResponse(UserInfo userInfo, RestrictionInformationBatchRequest request) {
		List<Long> objectIds = KeyFactory.stringToKey(request.getObjectIds());
		
		List<RestrictionInformationResponse> restrictionList = accessRestrictionStatusDao.getNonEntityStatus(objectIds, request.getRestrictableObjectType(), userInfo.getId()).stream().map( restrictionStatus -> {
			// Does not apply for teams
			boolean isUserDataContributor = false;
			
			Supplier<List<Long>> unmetArIdsSupplier = () -> UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForNonEntity(restrictionStatus);
			
			return buildRestrictionInformationResponse(restrictionStatus, isUserDataContributor, unmetArIdsSupplier);
			
		}).collect(Collectors.toList());
		
		return new RestrictionInformationBatchResponse().setRestrictionInformation(restrictionList);
	}
	
	static RestrictionInformationResponse buildRestrictionInformationResponse(UsersRestrictionStatus restrictionStatus, boolean isUserDataContributor, Supplier<List<Long>> unmetArIdsSupplier) {
		
		RestrictionInformationResponse response = new RestrictionInformationResponse()
				.setObjectId(restrictionStatus.getSubjectId())
				.setIsUserDataContributor(isUserDataContributor);
		
		if (restrictionStatus.getAccessRestrictions().isEmpty()) {
			return response
				.setHasUnmetAccessRequirement(false)
				.setRestrictionLevel(RestrictionLevel.OPEN)
				.setRestrictionDetails(Collections.emptyList());
		}
		
		Set<Long> unmetArIds = new HashSet<>(unmetArIdsSupplier.get());
		
		List<RestrictionFulfillment> restrictionDetails = restrictionStatus.getAccessRestrictions().stream().map(userRequirementStatus -> new RestrictionFulfillment()
			.setAccessRequirementId(userRequirementStatus.getRequirementId())
			.setIsApproved(!userRequirementStatus.isUnmet())
			.setIsMet(!unmetArIds.contains(userRequirementStatus.getRequirementId()))
			.setIsExempt(UserAccessRestrictionUtils.isUserExempt(userRequirementStatus, isUserDataContributor))
		).collect(Collectors.toList());
		
		return response
			.setHasUnmetAccessRequirement(!unmetArIds.isEmpty())
			.setRestrictionLevel(restrictionStatus.getMostRestrictiveLevel())
			.setRestrictionDetails(restrictionDetails);
	}
}
