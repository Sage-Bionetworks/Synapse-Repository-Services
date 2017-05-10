package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Date;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class RequestManagerImpl implements RequestManager{

	@Autowired
	private AccessRequirementDAO accessRequirementDao;
	@Autowired
	private RequestDAO requestDao;
	@Autowired
	private SubmissionDAO submissionDao;

	@WriteTransactionReadCommitted
	@Override
	public Request create(UserInfo userInfo, Request toCreate) {
		ValidateArgument.required(userInfo, "userInfo");
		validateRequest(toCreate);
		AccessRequirement ar = accessRequirementDao.get(toCreate.getAccessRequirementId());
		ValidateArgument.requirement(ar instanceof ACTAccessRequirement,
				"A Request can only associate with an ACTAccessRequirement.");
		ACTAccessRequirement actAR = (ACTAccessRequirement) ar;
		ValidateArgument.requirement(actAR.getAcceptRequest() != null
				&& actAR.getAcceptRequest(),
				"This Access Requirement doesn't accept Data Access Request.");
		toCreate = prepareCreationFields(toCreate, userInfo.getId().toString());
		return requestDao.create(toCreate);
	}

	public Request prepareCreationFields(Request toCreate, String createdBy) {
		toCreate.setCreatedBy(createdBy);
		toCreate.setCreatedOn(new Date());
		toCreate = (Request) prepareUpdateFields(toCreate, createdBy);
		return toCreate;
	}

	public RequestInterface prepareUpdateFields(RequestInterface toUpdate, String modifiedBy) {
		toUpdate.setModifiedBy(modifiedBy);
		toUpdate.setModifiedOn(new Date());
		return toUpdate;
	}

	public void validateRequest(RequestInterface toUpdate) {
		ValidateArgument.required(toUpdate, "toCreate");
		ValidateArgument.required(toUpdate.getAccessRequirementId(), "Request.accessRequirementId");
		ValidateArgument.required(toUpdate.getResearchProjectId(), "Request.researchProjectId");
	}

	@Override
	public RequestInterface getUserOwnCurrentRequest(UserInfo userInfo, String accessRequirementId)
			throws NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		return requestDao.getUserOwnCurrentRequest(accessRequirementId, userInfo.getId().toString());
	}

	@Override
	public RequestInterface getRequestForUpdate(UserInfo userInfo, String accessRequirementId)
			throws NotFoundException {
		try {
			RequestInterface current = getUserOwnCurrentRequest(userInfo, accessRequirementId);
			if (current instanceof Renewal) {
				return current;
			}
			ACTAccessRequirement requirement = (ACTAccessRequirement) accessRequirementDao.get(accessRequirementId);
			if (requirement.getIsAnnualReviewRequired()
					&& submissionDao.hasSubmissionWithState(
					userInfo.getId().toString(), accessRequirementId,
					SubmissionState.APPROVED)) {
				return createRenewalFromRequest(current);
			}
			return current;
		} catch (NotFoundException e) {
			return createNewRequest(accessRequirementId);
		}
	}

	private RequestInterface createNewRequest(String accessRequirementId) {
		Request request = new Request();
		request.setAccessRequirementId(accessRequirementId);
		return request;
	}

	public Renewal createRenewalFromRequest(RequestInterface current) {
		Renewal renewal = new Renewal();
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
	public RequestInterface update(UserInfo userInfo, RequestInterface toUpdate)
			throws NotFoundException, UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		validateRequest(toUpdate);

		RequestInterface original = requestDao.getForUpdate(toUpdate.getId());

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

		ValidateArgument.requirement(!submissionDao.hasSubmissionWithState(
				userInfo.getId().toString(), toUpdate.getAccessRequirementId(),
				SubmissionState.SUBMITTED),
				"A submission has been created. User needs to cancel the created submission or wait for an ACT member to review it before create another submission.");

		ACTAccessRequirement requirement = (ACTAccessRequirement) accessRequirementDao.get(toUpdate.getAccessRequirementId());
		if (requirement.getIsAnnualReviewRequired()) {
			boolean hasApprovedSubmission = submissionDao.hasSubmissionWithState(
					userInfo.getId().toString(), toUpdate.getAccessRequirementId(),
					SubmissionState.APPROVED);
			if (toUpdate instanceof Renewal) {
				ValidateArgument.requirement(hasApprovedSubmission,
						"Can only create/update a renewal request after a submission is approved.");
			} else {
				ValidateArgument.requirement(!hasApprovedSubmission,
						"The AccessRequirement requires renewal.");
			}
		} else {
			ValidateArgument.requirement(toUpdate instanceof Request,
					"AccessRequirement does not require renewal.");
		}

		toUpdate = prepareUpdateFields(toUpdate, userInfo.getId().toString());
		return requestDao.update(toUpdate);
	}

	@WriteTransactionReadCommitted
	@Override
	public RequestInterface createOrUpdate(UserInfo userInfo, RequestInterface toCreateOrUpdate) {
		ValidateArgument.required(toCreateOrUpdate, "toCreateOrUpdate");
		if (toCreateOrUpdate.getId() == null) {
			ValidateArgument.requirement(toCreateOrUpdate instanceof Request, 
					"Cannot create a request of type "+toCreateOrUpdate.getClass().getSimpleName().toString());
			return create(userInfo, (Request) toCreateOrUpdate);
		} else {
			return update(userInfo, toCreateOrUpdate);
		}
	}

}
