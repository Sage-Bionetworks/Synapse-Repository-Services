package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Date;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRenewal;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessRequestDAO;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessRequestManagerImpl implements DataAccessRequestManager{

	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private AccessRequirementDAO accessRequirementDao;
	@Autowired
	private DataAccessRequestDAO dataAccessRequestDao;

	@WriteTransactionReadCommitted
	@Override
	public DataAccessRequest create(UserInfo userInfo, DataAccessRequest toCreate) {
		ValidateArgument.required(userInfo, "userInfo");
		validateRequest(toCreate);
		ValidateArgument.requirement(accessRequirementDao.get(toCreate.getAccessRequirementId())
				.getConcreteType().equals(ACTAccessRequirement.class.getName()),
				"A DataAccessRequest can only associate with an ACTAccessRequirement.");
		toCreate = prepareCreationFields(toCreate, userInfo.getId().toString());
		return dataAccessRequestDao.create(toCreate);
	}

	public DataAccessRequest prepareCreationFields(DataAccessRequest toCreate, String createdBy) {
		toCreate.setId(idGenerator.generateNewId(TYPE.DATA_ACCESS_REQUEST_ID).toString());
		toCreate.setCreatedBy(createdBy);
		toCreate.setCreatedOn(new Date());
		toCreate = (DataAccessRequest) prepareUpdateFields(toCreate, createdBy);
		return toCreate;
	}

	public DataAccessRequestInterface prepareUpdateFields(DataAccessRequestInterface toUpdate, String modifiedBy) {
		toUpdate.setModifiedBy(modifiedBy);
		toUpdate.setModifiedOn(new Date());
		toUpdate.setEtag(UUID.randomUUID().toString());
		toUpdate.setConcreteType(toUpdate.getClass().getName());
		return toUpdate;
	}

	public void validateRequest(DataAccessRequestInterface toUpdate) {
		ValidateArgument.required(toUpdate, "toCreate");
		ValidateArgument.required(toUpdate.getAccessRequirementId(), "DataAccessRequest.accessRequirementId");
		ValidateArgument.required(toUpdate.getResearchProjectId(), "DataAccessRequest.researchProjectId");
	}

	@Override
	public DataAccessRequestInterface getUserOwnCurrentRequest(UserInfo userInfo, String accessRequirementId)
			throws NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		return dataAccessRequestDao.getUserOwnCurrentRequest(accessRequirementId, userInfo.getId().toString());
	}

	@Override
	public DataAccessRequestInterface getDataAccessRequestForUpdate(UserInfo userInfo, String accessRequirementId)
			throws NotFoundException {
		try {
			DataAccessRequestInterface current = getUserOwnCurrentRequest(userInfo, accessRequirementId);
			if (current instanceof DataAccessRenewal) {
				return current;
			}
			ACTAccessRequirement requirement = (ACTAccessRequirement) accessRequirementDao.get(accessRequirementId);
			if (requirement.getIsAnnualReviewRequired() /*TODO: && has approved submission*/) {
				return createRenewalFromRequest(current);
			}
			return current;
		} catch (NotFoundException e) {
			return new DataAccessRequest();
		}
	}

	public DataAccessRenewal createRenewalFromRequest(DataAccessRequestInterface current) {
		DataAccessRenewal renewal = new DataAccessRenewal();
		renewal.setId(current.getId());
		renewal.setAccessRequirementId(current.getAccessRequirementId());
		renewal.setResearchProjectId(current.getResearchProjectId());
		renewal.setCreatedBy(current.getCreatedBy());
		renewal.setCreatedOn(current.getCreatedOn());
		renewal.setModifiedBy(current.getModifiedBy());
		renewal.setModifiedOn(current.getModifiedOn());
		renewal.setAccessors(current.getAccessors());
		renewal.setAttachments(current.getAttachments());
		renewal.setDucFileHandleId(current.getDucFileHandleId());
		renewal.setIrbFileHandleId(current.getIrbFileHandleId());
		renewal.setEtag(current.getEtag());
		renewal.setConcreteType(DataAccessRenewal.class.getName());
		return renewal;
	}

	@WriteTransactionReadCommitted
	@Override
	public DataAccessRequestInterface update(UserInfo userInfo, DataAccessRequestInterface toUpdate)
			throws NotFoundException, UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		validateRequest(toUpdate);

		DataAccessRequestInterface original = dataAccessRequestDao.getForUpdate(toUpdate.getId());

		if (!original.getEtag().equals(toUpdate.getEtag())) {
			throw new ConflictingUpdateException("etag does not match.");
		}

		ValidateArgument.requirement(toUpdate.getCreatedBy().equals(original.getCreatedBy())
				&& toUpdate.getCreatedOn().equals(original.getCreatedOn())
				&& toUpdate.getAccessRequirementId().equals(original.getAccessRequirementId())
				&& toUpdate.getResearchProjectId().equals(original.getResearchProjectId()),
				"researchProjectId, accessRequirementId, createdOn and createdBy fields cannot be editted.");

		if (toUpdate instanceof DataAccessRenewal) {
			ACTAccessRequirement requirement = (ACTAccessRequirement) accessRequirementDao.get(toUpdate.getAccessRequirementId());
			ValidateArgument.requirement(requirement.getIsAnnualReviewRequired()
					/* && has approved submission */,
					"Can only update to a DataAccessRenewal after a submission is approved and the requirement requires renewal.");
		}

		if (!original.getCreatedBy().equals(userInfo.getId().toString())) {
				throw new UnauthorizedException("Only owner can perform this action.");
		}

		toUpdate = prepareUpdateFields(toUpdate, userInfo.getId().toString());
		return dataAccessRequestDao.update(toUpdate);
	}

	@Override
	public DataAccessRequestInterface createOrUpdate(UserInfo userInfo, DataAccessRequestInterface toCreateOrUpdate) {
		ValidateArgument.required(toCreateOrUpdate, "toCreateOrUpdate");
		if (toCreateOrUpdate.getId() == null) {
			ValidateArgument.requirement(toCreateOrUpdate instanceof DataAccessRequest, 
					"Cannot create a request of type "+toCreateOrUpdate.getClass().getSimpleName().toString());
			return create(userInfo, (DataAccessRequest) toCreateOrUpdate);
		} else {
			return update(userInfo, toCreateOrUpdate);
		}
	}

}
