package org.sagebionetworks.repo.manager.dataaccess;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sagebionetworks.docusign.DocuSignClient;
import org.sagebionetworks.docusign.EnvelopeStatusResult;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationManager;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.dataaccess.AccessRequestList;
import org.sagebionetworks.repo.model.dataaccess.AccessRequestListRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessRequestStatusEnum;
import org.sagebionetworks.repo.model.dataaccess.AccessRequestSummary;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.PrincipalInvestigator;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.SigningOfficial;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestUserInfo;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;

import com.docusign.esign.model.Envelope;
import com.docusign.esign.model.Signer;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RequestManagerImpl implements RequestManager{
	public static final int MAX_ACCESSORS = 500;

	private final AccessRequirementDAO accessRequirementDao;
	private final RequestDAO requestDao;
	private final SubmissionDAO submissionDao;
	private final FileHandleAuthorizationManager fileHandleAuthorizationManager;
	private final DocuSignClient docuSignClient;

	@Autowired
	public RequestManagerImpl(AccessRequirementDAO accessRequirementDao, RequestDAO requestDao,
			SubmissionDAO submissionDao, FileHandleAuthorizationManager fileHandleAuthorizationManager,
			DocuSignClient docuSignClient) {
		super();
		this.accessRequirementDao = accessRequirementDao;
		this.requestDao = requestDao;
		this.submissionDao = submissionDao;
		this.fileHandleAuthorizationManager = fileHandleAuthorizationManager;
		this.docuSignClient = docuSignClient;
	}

	Request create(UserInfo userInfo, Request toCreate) {
		ValidateArgument.required(userInfo, "userInfo");
		validateRequest(toCreate);
		validateEnvelopeCompletion(toCreate);
		validateFileHandleAccess(userInfo, toCreate);
		AccessRequirement ar = accessRequirementDao.get(toCreate.getAccessRequirementId());
		ValidateArgument.requirement(ar instanceof ManagedACTAccessRequirement,
				"A Request can only associate with an ManagedACTAccessRequirement.");
		toCreate = prepareCreationFields(toCreate, userInfo.getId().toString());
		Request result = requestDao.create(toCreate);
		return result;
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
		PrincipalInvestigator pi = toUpdate.getPrincipalInvestigator();
		if (pi != null && pi.getInstitutionalEmail() != null) {
			AliasEnum.USER_EMAIL.validateAlias(pi.getInstitutionalEmail());
		}
		SigningOfficial so = toUpdate.getSigningOfficial();
		if (so != null && so.getInstitutionalEmail() != null) {
			AliasEnum.USER_EMAIL.validateAlias(so.getInstitutionalEmail());
		}
	}


	/*
	 * If there is an associated eDUC envelope then a signed DUC document if the envelope is done being
	 * routed.  If there is no associated eDUC envelope then the request is using a 'traditional' (non-eDUC)
	 * flow and it's OK to attach the signed document.
	 */
	void validateEnvelopeCompletion(RequestInterface request) {
		if (request.getDucFileHandleId() != null && request.getEDucSignatureEnvelopeId() != null) {
			EnvelopeStatusResult envelopeResult = docuSignClient.getEnvelopeStatus(request.getEDucSignatureEnvelopeId());
			ValidateArgument.requirement(EDucStatusEnum.completed.equals(envelopeResult.status().getDucStatus()),
					"Cannot set ducFileHandleId: the eDUC envelope has not been completed.");
		}
	}

	/*
	 * Can only attach documents uploaded by the same user who created the request.
	 */
	void validateFileHandleAccess(UserInfo userInfo, RequestInterface request) {
		if (request.getDucFileHandleId() != null) {
			fileHandleAuthorizationManager.canAccessRawFileHandleById(userInfo, request.getDucFileHandleId())
					.checkAuthorizationOrElseThrow();
		}
		if (request.getIrbFileHandleId() != null) {
			fileHandleAuthorizationManager.canAccessRawFileHandleById(userInfo, request.getIrbFileHandleId())
					.checkAuthorizationOrElseThrow();
		}
	}

	@Override
	public RequestInterface getRequestForUpdate(UserInfo userInfo, String accessRequirementId)
			throws NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		// Verify the access requirement exists; a missing one must be a 404 rather than falling
		// through to a blank new-request stub.
		accessRequirementDao.get(accessRequirementId);
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

	RequestInterface update(UserInfo userInfo, RequestInterface toUpdate)
			throws NotFoundException, UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		validateRequest(toUpdate);
		validateFileHandleAccess(userInfo, toUpdate);

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

		// The eDUC signature envelope id is managed by the server (set when routing for signature
		// and cleared when cancelling). Preserve the persisted value so a client editing the
		// request cannot resurrect or change it from a stale copy. This must happen before
		// validateEnvelopeCompletion so the envelope-completion check runs against the authoritative
		// envelope id rather than whatever the client sent.
		toUpdate.setEDucSignatureEnvelopeId(original.getEDucSignatureEnvelopeId());

		validateEnvelopeCompletion(toUpdate);

		toUpdate = prepareUpdateFields(toUpdate, userInfo.getId().toString());
		RequestInterface result = requestDao.update(toUpdate);
		return result;
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

	@Override
	public AccessRequestList listUserRequests(UserInfo userInfo, AccessRequestListRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");

		NextPageToken token = new NextPageToken(request.getNextPageToken());
		List<RequestUserInfo> page = requestDao.getUserRequests(
				userInfo.getId(), token.getLimitForQuery(), token.getOffset(),
				request.getSortBy(), request.getSortDirection());

		List<String> envelopeIds = page.stream()
				.map(RequestUserInfo::getEnvelopeId)
				.filter(id -> id != null)
				.collect(Collectors.toList());

		Map<String, Envelope> envelopeMap = new HashMap<>();
		if (!envelopeIds.isEmpty()) {
			List<Envelope> envelopes = docuSignClient.listEnvelopeStatuses(envelopeIds);
			for (Envelope env : envelopes) {
				envelopeMap.put(env.getEnvelopeId(), env);
			}
		}

		List<AccessRequestSummary> results = new ArrayList<>();
		for (RequestUserInfo info : page) {
			AccessRequestSummary summary = new AccessRequestSummary();
			summary.setRequestId(info.getRequestId());
			summary.setAccessRequirementId(info.getAccessRequirementId());
			summary.setAccessRequirementName(info.getAccessRequirementName());
			summary.setIsEDuc(info.getEnvelopeId() != null);
			summary.setSubmittedOn(info.getSubmittedOn());
			summary.setModifiedOn(info.getModifiedOn());
			summary.setExpiresOn(info.getExpiresOn());

			if (info.getSubmissionStatus() != null) {
				summary.setStatus(toAccessRequestStatus(info.getSubmissionStatus()));
			} else if (info.getEnvelopeId() != null) {
				Envelope env = envelopeMap.get(info.getEnvelopeId());
				if (env != null) {
					summary.setStatus(toAccessRequestStatusFromEnvelope(env.getStatus()));
					if (env.getRecipients() != null && env.getRecipients().getSigners() != null) {
						List<Signer> signers = env.getRecipients().getSigners();
						summary.setSignaturesRequested((long) signers.size());
						long completed = signers.stream()
								.filter(s -> "completed".equalsIgnoreCase(s.getStatus())
										|| "signed".equalsIgnoreCase(s.getStatus()))
								.count();
						summary.setSignaturesAcquired(completed);
					}
				} else {
					summary.setStatus(AccessRequestStatusEnum.created);
				}
			} else {
				summary.setStatus(AccessRequestStatusEnum.created);
			}

			results.add(summary);
		}

		AccessRequestList result = new AccessRequestList();
		result.setResults(results);
		result.setNextPageToken(token.getNextPageTokenForCurrentResults(results));
		return result;
	}

	static AccessRequestStatusEnum toAccessRequestStatus(SubmissionState submissionState) {
		switch (submissionState) {
			case SUBMITTED:
				return AccessRequestStatusEnum.submitted;
			case APPROVED:
				return AccessRequestStatusEnum.approved;
			case REJECTED:
				return AccessRequestStatusEnum.rejected;
			case CANCELLED:
				return AccessRequestStatusEnum.cancelled;
			default:
				throw new IllegalArgumentException("Unexpected submission state: " + submissionState);
		}
	}

	static AccessRequestStatusEnum toAccessRequestStatusFromEnvelope(String envelopeStatus) {
		EDucStatusEnum ducStatus = DocuSignClient.toEDucStatusEnum(envelopeStatus);
		return AccessRequestStatusEnum.valueOf(ducStatus.name());
	}

	@Override
	public void truncateAll() {
		requestDao.truncateAll();
	}

}
