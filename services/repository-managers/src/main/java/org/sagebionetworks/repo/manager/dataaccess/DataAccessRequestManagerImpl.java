package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Date;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRenewal;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessRequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessSubmissionDAO;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessRequestManagerImpl implements DataAccessRequestManager{

	@Autowired
	private AccessRequirementDAO accessRequirementDao;
	@Autowired
	private DataAccessRequestDAO dataAccessRequestDao;
	@Autowired
	private DataAccessSubmissionDAO dataAccessSubmissionDao;

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
		toCreate.setCreatedBy(createdBy);
		toCreate.setCreatedOn(new Date());
		toCreate = (DataAccessRequest) prepareUpdateFields(toCreate, createdBy);
		return toCreate;
	}

	public DataAccessRequestInterface prepareUpdateFields(DataAccessRequestInterface toUpdate, String modifiedBy) {
		toUpdate.setModifiedBy(modifiedBy);
		toUpdate.setModifiedOn(new Date());
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
			if (requirement.getIsAnnualReviewRequired()
					&& dataAccessSubmissionDao.hasSubmissionWithState(
					userInfo.getId().toString(), accessRequirementId,
					DataAccessSubmissionState.APPROVED)) {
				return createRenewalFromRequest(current);
			}
			return current;
		} catch (NotFoundException e) {
			return createNewDataAccessRequest(accessRequirementId);
		}
	}

	private DataAccessRequestInterface createNewDataAccessRequest(String accessRequirementId) {
		DataAccessRequest request = new DataAccessRequest();
		request.setAccessRequirementId(accessRequirementId);
		return request;
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

		if (!original.getCreatedBy().equals(userInfo.getId().toString())) {
				throw new UnauthorizedException("Only owner can perform this action.");
		}

		ValidateArgument.requirement(!dataAccessSubmissionDao.hasSubmissionWithState(
				userInfo.getId().toString(), toUpdate.getAccessRequirementId(),
				DataAccessSubmissionState.SUBMITTED),
				"A submission has been created. User needs to cancel the created submission or wait for an ACT member to review it before create another submission.");

		ACTAccessRequirement requirement = (ACTAccessRequirement) accessRequirementDao.get(toUpdate.getAccessRequirementId());
		if (requirement.getIsAnnualReviewRequired()) {
			boolean hasApprovedSubmission = dataAccessSubmissionDao.hasSubmissionWithState(
					userInfo.getId().toString(), toUpdate.getAccessRequirementId(),
					DataAccessSubmissionState.APPROVED);
			if (toUpdate instanceof DataAccessRenewal) {
				ValidateArgument.requirement(hasApprovedSubmission,
						"Can only create/update a renewal request after a submission is approved.");
			} else {
				ValidateArgument.requirement(!hasApprovedSubmission,
						"The AccessRequirement requires renewal.");
			}
		} else {
			ValidateArgument.requirement(toUpdate instanceof DataAccessRequest,
					"AccessRequirement does not require renewal.");
		}

		toUpdate = prepareUpdateFields(toUpdate, userInfo.getId().toString());
		return dataAccessRequestDao.update(toUpdate);
	}

	@WriteTransactionReadCommitted
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
