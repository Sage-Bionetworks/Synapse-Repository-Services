package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class RequestManagerImpl implements RequestManager{
	public static final int MAX_ACCESSORS = 500;

	@Autowired
	private AccessRequirementDAO accessRequirementDao;
	@Autowired
	private RequestDAO requestDao;
	@Autowired
	private SubmissionDAO submissionDao;

	@WriteTransaction
	@Override
	public Request create(UserInfo userInfo, Request toCreate) {
		ValidateArgument.required(userInfo, "userInfo");
		validateRequest(toCreate);
		AccessRequirement ar = accessRequirementDao.get(toCreate.getAccessRequirementId());
		ValidateArgument.requirement(ar instanceof ManagedACTAccessRequirement,
				"A Request can only associate with an ManagedACTAccessRequirement.");
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
		ValidateArgument.requirement(toUpdate.getAccessorChanges() == null
				|| toUpdate.getAccessorChanges().isEmpty()
				|| toUpdate.getAccessorChanges().size() <= MAX_ACCESSORS,
				"A request cannot have more than "+MAX_ACCESSORS+" changes.");
	}


	@Override
	public RequestInterface getRequestForUpdate(UserInfo userInfo, String accessRequirementId)
			throws NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		try {
			return requestDao.getUserOwnCurrentRequest(accessRequirementId, userInfo.getId().toString());
		} catch (NotFoundException e) {
			return createNewRequest(accessRequirementId);
		}
	}

	private RequestInterface createNewRequest(String accessRequirementId) {
		Request request = new Request();
		request.setAccessRequirementId(accessRequirementId);
		return request;
	}

	/**
	 * Given a request/renewal that was approved, create a renewal that includes
	 * all accessors that still have access to {@link AccessType.RENEW_ACCESS}
	 * and excludes all accssors that were revoked.
	 * All other fields from the original request/renewal are copied into the new
	 * renewal.
	 * 
	 * @param current
	 * @return
	 */
	public static Renewal createRenewalFromApprovedRequest(RequestInterface current) {
		Renewal renewal = new Renewal();
		renewal.setId(current.getId());
		renewal.setAccessRequirementId(current.getAccessRequirementId());
		renewal.setResearchProjectId(current.getResearchProjectId());
		renewal.setCreatedBy(current.getCreatedBy());
		renewal.setCreatedOn(current.getCreatedOn());
		renewal.setModifiedBy(current.getModifiedBy());
		renewal.setModifiedOn(current.getModifiedOn());
		// All current users should be renewed
		if(current.getAccessorChanges() != null){
			List<AccessorChange> list = new LinkedList<>();
			for(AccessorChange oldChange: current.getAccessorChanges()){
				if(AccessType.REVOKE_ACCESS.equals(oldChange.getType())){
					// users that were revoked can be ignored this time.
					continue;
				}
				// All other users should be renewed.
				AccessorChange newChagne = new AccessorChange();
				newChagne.setUserId(oldChange.getUserId());
				newChagne.setType(AccessType.RENEW_ACCESS);
				list.add(newChagne);
			}
			renewal.setAccessorChanges(list);
		}
		renewal.setAttachments(current.getAttachments());
		renewal.setDucFileHandleId(current.getDucFileHandleId());
		renewal.setIrbFileHandleId(current.getIrbFileHandleId());
		renewal.setEtag(current.getEtag());
		return renewal;
	}

	@WriteTransaction
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
				"researchProjectId, accessRequirementId, createdOn and createdBy fields cannot be edited.");

		if (!original.getCreatedBy().equals(userInfo.getId().toString())) {
				throw new UnauthorizedException("Only owner can perform this action.");
		}

		ValidateArgument.requirement(!submissionDao.hasSubmissionWithState(
				userInfo.getId().toString(), toUpdate.getAccessRequirementId(),
				SubmissionState.SUBMITTED),
				"A submission has been created. User needs to cancel the created submission or wait for an ACT member to review it before create another submission.");

		toUpdate = prepareUpdateFields(toUpdate, userInfo.getId().toString());
		return requestDao.update(toUpdate);
	}

	@WriteTransaction
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

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.dataaccess.RequestManager#updateApprovedRequest(java.lang.String)
	 */
	@WriteTransaction
	@Override
	public void updateApprovedRequest(String requestId) {
		ValidateArgument.required(requestId, "requestId");
		RequestInterface original = requestDao.getForUpdate(requestId);
		original = createRenewalFromApprovedRequest(original);
		/*
		 * Note: Since this method is called when a submission is approved by
		 * ACT, modifiedOn and modifiedBy are not changed. The dao.update() will
		 * change the etag.
		 */
		requestDao.update(original);
	}

	/*
	 * 
	 */
	@Override
	public RequestInterface getRequestForSubmission(String requestId) {
		ValidateArgument.required(requestId, "requestId");
		return requestDao.get(requestId);
	}

}
