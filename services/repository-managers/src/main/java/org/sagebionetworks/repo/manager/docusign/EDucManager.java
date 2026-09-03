package org.sagebionetworks.repo.manager.docusign;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.http.entity.ContentType;
import org.sagebionetworks.docusign.DocuSignClient;
import org.sagebionetworks.docusign.EnvelopeRecipient;
import org.sagebionetworks.docusign.EnvelopeStatusResult;
import org.sagebionetworks.docusign.RecipientInfo;
import org.sagebionetworks.docusign.RoleLabelKey;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.PrincipalInvestigator;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.SigningOfficial;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucQuotaDao;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.educ.EDucFileHandleId;
import org.sagebionetworks.repo.model.educ.EDucSignatureStatus;
import org.sagebionetworks.repo.model.educ.EDucSignerStatus;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;
import org.sagebionetworks.repo.model.educ.EDucTemplateListRequest;
import org.sagebionetworks.repo.model.educ.EDucTemplatePage;
import org.sagebionetworks.repo.model.educ.EDucTemplateValidationResult;
import org.sagebionetworks.repo.model.educ.EDucSignatureQuota;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class EDucManager {

	static final int MAX_ENVELOPES_PER_MONTH = 10;
	static final long THIRTY_DAYS_IN_MS = 30L * 24 * 60 * 60 * 1000;
	static final int MAX_GLOBAL_ENVELOPES_PER_DAY = 100;
	// Prefix of the numbered DocuSign roles the template defines for the request's accessors.
	static final String COLLABORATOR_ROLE_PREFIX = "collaborator_";
	static final long ONE_DAY_IN_MS = 24L * 60 * 60 * 1000;

	private final DocuSignClient docuSignClient;
	private final RequestDAO requestDao;
	private final AccessRequirementDAO accessRequirementDao;
	private final PrincipalAliasDAO principalAliasDao;
	private final NotificationEmailDAO notificationEmailDao;
	private final UserProfileDAO userProfileDao;
	private final EDucQuotaDao eDucQuotaDao;
	private final Clock clock;
	private final FileHandleManager fileHandleManager;

	public EDucManager(DocuSignClient docuSignClient, RequestDAO requestDao,
			AccessRequirementDAO accessRequirementDao, PrincipalAliasDAO principalAliasDao,
			NotificationEmailDAO notificationEmailDao, UserProfileDAO userProfileDao,
			EDucQuotaDao eDucQuotaDao, Clock clock, FileHandleManager fileHandleManager) {
		this.docuSignClient = docuSignClient;
		this.requestDao = requestDao;
		this.accessRequirementDao = accessRequirementDao;
		this.principalAliasDao = principalAliasDao;
		this.notificationEmailDao = notificationEmailDao;
		this.userProfileDao = userProfileDao;
		this.eDucQuotaDao = eDucQuotaDao;
		this.clock = clock;
		this.fileHandleManager = fileHandleManager;
	}

	public EDucTemplatePage listTemplates(UserInfo userInfo, EDucTemplateListRequest request) throws Exception {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		if (!AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		NextPageToken token = new NextPageToken(request.getNextPageToken());
		int startPosition = (int) token.getOffset();
		int count = (int) token.getLimitForQuery();
		EDucTemplatePage page = docuSignClient.listTemplates(startPosition, count);
		page.setNextPageToken(token.getNextPageTokenForCurrentResults(page.getResults()));
		return page;
	}

	public EDucTemplateValidationResult validateTemplate(UserInfo userInfo, String templateId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(templateId, "templateId");
		if (!AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		EDucTemplateValidationResult result = new EDucTemplateValidationResult();
		try {
			docuSignClient.validateTemplate(templateId);
			result.setIsValid(true);
		} catch (IllegalArgumentException e) {
			result.setIsValid(false);
			result.setReason(e.getMessage());
		}
		return result;
	}

	public EDucSignatureQuota routeForSignature(UserInfo userInfo, String requestId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(requestId, "requestId");

		RequestInterface request = requestDao.get(requestId);

		if (!AuthorizationUtils.isUserCreatorOrAdmin(userInfo, request.getCreatedBy())) {
			throw new UnauthorizedException("Only the request creator or an administrator can route for signature.");
		}

		Long userId = userInfo.getId();
		Long arId = Long.parseLong(request.getAccessRequirementId());

		long nowMs = clock.currentTimeMillis();
		long thirtyDaysAgoMs = nowMs - THIRTY_DAYS_IN_MS;

		long count = eDucQuotaDao.getCount(userId, arId, thirtyDaysAgoMs, nowMs);
		if (count >= MAX_ENVELOPES_PER_MONTH) {
			throw new IllegalArgumentException(
					"User has exceeded their eDUC routing quota for the requested access requirement.");
		}

		long oneDayAgoMs = nowMs - ONE_DAY_IN_MS;
		long globalCount = eDucQuotaDao.getGlobalCount(oneDayAgoMs, nowMs);
		if (globalCount >= MAX_GLOBAL_ENVELOPES_PER_DAY) {
			throw new IllegalArgumentException(
					"The global daily eDUC routing limit has been reached. Please try again later.");
		}

		request = createDraftEDuc(request);
		String envelopeId = request.getEDucSignatureEnvelopeId();

		docuSignClient.sendEnvelope(envelopeId);

		eDucQuotaDao.create(userId, arId, envelopeId);
		requestDao.setEDucContentHash(requestId, computeEDucContentHash(request));

		EDucSignatureQuota result = new EDucSignatureQuota();
		result.setQuota((long) MAX_ENVELOPES_PER_MONTH);
		result.setRemaining((long) (MAX_ENVELOPES_PER_MONTH - count - 1));
		return result;
	}

	public EDucSignatureQuota getSignatureQuota(UserInfo userInfo, String requestId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(requestId, "requestId");

		RequestInterface request = requestDao.get(requestId);

		if (!AuthorizationUtils.isUserCreatorOrAdmin(userInfo, request.getCreatedBy())) {
			throw new UnauthorizedException("Only the request creator or an administrator can view the signature quota.");
		}

		Long userId = Long.parseLong(request.getCreatedBy());
		Long arId = Long.parseLong(request.getAccessRequirementId());

		long nowMs = clock.currentTimeMillis();
		long thirtyDaysAgoMs = nowMs - THIRTY_DAYS_IN_MS;

		long count = eDucQuotaDao.getCount(userId, arId, thirtyDaysAgoMs, nowMs);

		EDucSignatureQuota result = new EDucSignatureQuota();
		result.setQuota((long) MAX_ENVELOPES_PER_MONTH);
		result.setRemaining(Math.max(0L, MAX_ENVELOPES_PER_MONTH - count));
		return result;
	}

	public EDucSignatureQuota resetQuota(UserInfo userInfo, String accessRequirementId, Long userId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(userId, "userId");

		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Only an administrator can reset an eDUC quota.");
		}

		// Validate the access requirement exists (throws NotFoundException otherwise).
		accessRequirementDao.get(accessRequirementId);

		Long arId = Long.parseLong(accessRequirementId);

		eDucQuotaDao.deleteByUserAndAccessRequirement(userId, arId);

		EDucSignatureQuota result = new EDucSignatureQuota();
		result.setQuota((long) MAX_ENVELOPES_PER_MONTH);
		result.setRemaining((long) MAX_ENVELOPES_PER_MONTH);
		return result;
	}

	public EDucFileHandleId previewEDuc(UserInfo userInfo, String requestId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(requestId, "requestId");

		RequestInterface request = requestDao.get(requestId);

		if (!AuthorizationUtils.isUserCreatorOrAdmin(userInfo, request.getCreatedBy())) {
			throw new UnauthorizedException("Only the request creator or an administrator can preview the eDUC.");
		}

		request = createDraftEDuc(request);
		String envelopeId = request.getEDucSignatureEnvelopeId();

		byte[] pdfBytes = docuSignClient.getDocument(envelopeId);

		try {
			S3FileHandle fileHandle = fileHandleManager.createFileFromByteArray(
					userInfo.getId().toString(), new Date(), pdfBytes,
					"eDUC_preview_" + requestId + ".pdf", ContentType.create("application/pdf"), null);
			EDucFileHandleId result = new EDucFileHandleId();
			result.setFileHandleId(fileHandle.getId());
			return result;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to upload preview document.", e);
		}
	}

	RequestInterface createDraftEDuc(RequestInterface request) {
		// if an envelope already exists then there's nothing more to do
		if (request.getEDucSignatureEnvelopeId() != null) {
			return request;
		}

		ManagedACTAccessRequirement managedAr = validateEDucRequest(request);
		String templateId = managedAr.getEDucTemplateId();
		// No envelope exists yet, so the collaborator roles are assigned from scratch.
		EDucContent content = buildEDucContent(request, List.of());

		String envelopeId = docuSignClient.createEnvelope(templateId, content.recipients(), content.tabValues());

		request.setEDucSignatureEnvelopeId(envelopeId);
		requestDao.update(request);
		return request;
	}

	/**
	 * Validates that the request's access requirement supports an eDUC and that the request has
	 * the principal investigator and signing official needed to build the envelope.
	 *
	 * @return the ManagedACTAccessRequirement (which carries the eDUC template ID)
	 */
	private ManagedACTAccessRequirement validateEDucRequest(RequestInterface request) {
		AccessRequirement ar = accessRequirementDao.get(request.getAccessRequirementId());
		if (!(ar instanceof ManagedACTAccessRequirement managedAr)) {
			throw new IllegalArgumentException("The access requirement is not a ManagedACTAccessRequirement.");
		}
		if (!Boolean.TRUE.equals(managedAr.getIsDUCRequired())) {
			throw new IllegalArgumentException("The access requirement does not require a DUC.");
		}
		if (StringUtils.isBlank(managedAr.getEDucTemplateId())) {
			throw new IllegalArgumentException("The access requirement does not have an eDUC template ID configured.");
		}

		PrincipalInvestigator pi = request.getPrincipalInvestigator();
		ValidateArgument.required(pi, "principalInvestigator");
		ValidateArgument.required(pi.getUserId(), "principalInvestigator.userId");
		ValidateArgument.requiredNotBlank(pi.getName(), "principalInvestigator.name");
		ValidateArgument.requiredNotBlank(pi.getInstitutionalEmail(), "principalInvestigator.institutionalEmail");

		SigningOfficial so = request.getSigningOfficial();
		ValidateArgument.required(so, "signingOfficial");
		ValidateArgument.required(so.getInstitutionalEmail(), "signingOfficial.institutionalEmail");
		ValidateArgument.requiredNotBlank(so.getName(), "signingOfficial.name");

		return managedAr;
	}

	// The recipient identities (email + name per role) and tab values derived from a request that
	// define the eDUC envelope content, used both when creating an envelope and when correcting one.
	private record EDucContent(Map<String, RecipientInfo> recipients,
			Map<RoleLabelKey, String> tabValues) {}

	/**
	 * Derives the envelope content from the request.
	 *
	 * @param existingRecipients the signers already on the envelope, so that a collaborator keeps
	 *        the role they were routed with; empty when the envelope has yet to be created
	 */
	private EDucContent buildEDucContent(RequestInterface request, List<EnvelopeRecipient> existingRecipients) {
		List<String> collaboratorUserIds = buildCollaboratorUserIds(request);
		// Look up each collaborator's profile and user name once and reuse for the recipient
		// identities, the role emails and the tab values.
		List<CollaboratorInfo> collaborators = gatherCollaboratorInfos(collaboratorUserIds);
		Map<String, CollaboratorInfo> collaboratorsByRole = assignCollaboratorRoles(collaborators, existingRecipients);
		Map<String, RecipientInfo> recipients = buildRecipients(request, collaboratorsByRole);
		Map<RoleLabelKey, String> tabValues = buildTabValues(request, collaboratorsByRole);

		return new EDucContent(recipients, tabValues);
	}

	/**
	 * Apply the current content of the request (signers and tab values) to the already-routed
	 * eDUC envelope. The envelope is corrected in place (no new envelope is created, so there is
	 * no quota impact). If the request has not been routed for signature, or the envelope's status
	 * does not allow an update, an {@link IllegalArgumentException} is thrown (HTTP 400) with a
	 * reason.
	 *
	 * @return the signature status of the envelope after the update
	 */
	public EDucSignatureStatus updateRoutedEnvelope(UserInfo userInfo, String requestId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(requestId, "requestId");

		RequestInterface request = requestDao.get(requestId);

		if (!AuthorizationUtils.isUserCreatorOrAdmin(userInfo, request.getCreatedBy())) {
			throw new UnauthorizedException("Only the request creator or an administrator can update the signature.");
		}

		String envelopeId = request.getEDucSignatureEnvelopeId();
		EnvelopeStatusResult statusResult = envelopeId == null ? null : docuSignClient.getEnvelopeStatus(envelopeId);

		Optional<String> reason = reasonUpdateNotPossible(envelopeId, statusResult);
		if (reason.isPresent()) {
			throw new IllegalArgumentException(reason.get());
		}

		ManagedACTAccessRequirement managedAr = validateEDucRequest(request);
		EDucContent content = buildEDucContent(request, docuSignClient.getRecipients(envelopeId));

		docuSignClient.correctEnvelope(envelopeId, managedAr.getEDucTemplateId(), content.recipients(),
				content.tabValues());
		requestDao.setEDucContentHash(requestId, computeEDucContentHash(request));

		return getSignatureStatus(userInfo, requestId);
	}

	/**
	 * Determine whether the current content of the request could be applied to its routed eDUC
	 * envelope (i.e. whether {@link #updateRoutedEnvelope} would succeed rather than return a 400).
	 *
	 * @return true if an update is possible, false otherwise
	 */
	public boolean canUpdateRoutedEnvelopePrecheck(UserInfo userInfo, String requestId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(requestId, "requestId");

		RequestInterface request = requestDao.get(requestId);

		if (!AuthorizationUtils.isUserCreatorOrAdmin(userInfo, request.getCreatedBy())) {
			throw new UnauthorizedException("Only the request creator or an administrator can check update status.");
		}

		String envelopeId = request.getEDucSignatureEnvelopeId();
		EnvelopeStatusResult statusResult = envelopeId == null ? null : docuSignClient.getEnvelopeStatus(envelopeId);

		return reasonUpdateNotPossible(envelopeId, statusResult).isEmpty();
	}

	/**
	 * Shared updatability logic used by both {@link #updateRoutedEnvelope} and
	 * {@link #canUpdateRoutedEnvelopePrecheck}.
	 *
	 * @return {@link Optional#empty()} if an update is possible, otherwise a human-readable reason
	 *         why the update is not possible.
	 */
	Optional<String> reasonUpdateNotPossible(String envelopeId, EnvelopeStatusResult statusResult) {
		if (envelopeId == null) {
			return Optional.of("This request has not been routed for signature.");
		}
		EDucStatusEnum status = statusResult.status().getDucStatus();
		if (EDucStatusEnum.draft.equals(status)) {
			return Optional.of("This request has not been routed for signature.");
		}
		if (EDucStatusEnum.sent.equals(status) || EDucStatusEnum.delivered.equals(status)) {
			return Optional.empty();
		}
		return Optional.of(switch (status) {
			case completed -> "The eDUC cannot be updated because it has already been completed.";
			case declined -> "The eDUC cannot be updated because a signer declined to sign.";
			case voided -> "The eDUC cannot be updated because it has been cancelled.";
			case correct -> "The eDUC cannot be updated because it is currently being corrected.";
			default -> "The eDUC cannot be updated in its current status: " + status + ".";
		});
	}

	/**
	 * Computes a stable hash of the request fields that determine the eDUC envelope content. This
	 * is recorded whenever the envelope is routed or corrected, so that a later change to any of
	 * these fields can be detected (see {@link #getSignatureStatus} setting includesRequestChanges).
	 */
	static String computeEDucContentHash(RequestInterface request) {
		StringBuilder builder = new StringBuilder();
		builder.append("createdBy=").append(request.getCreatedBy()).append('\n');
		builder.append("institution=").append(request.getInstitution()).append('\n');

		PrincipalInvestigator pi = request.getPrincipalInvestigator();
		if (pi != null) {
			builder.append("pi.userId=").append(pi.getUserId()).append('\n');
			builder.append("pi.name=").append(pi.getName()).append('\n');
			builder.append("pi.email=").append(pi.getInstitutionalEmail()).append('\n');
		}

		SigningOfficial so = request.getSigningOfficial();
		if (so != null) {
			builder.append("so.name=").append(so.getName()).append('\n');
			builder.append("so.email=").append(so.getInstitutionalEmail()).append('\n');
		}

		// Collaborators drive the envelope's recipient set, keyed by userId + change type. The
		// list order is significant because it determines the collaborator_N role assignment.
		List<AccessorChange> accessorChanges = request.getAccessorChanges();
		if (accessorChanges != null) {
			for (AccessorChange change : accessorChanges) {
				if (AccessType.GAIN_ACCESS.equals(change.getType())
						|| AccessType.RENEW_ACCESS.equals(change.getType())) {
					builder.append("accessor=").append(change.getUserId())
							.append(':').append(change.getType()).append('\n');
				}
			}
		}

		// Note, this string encoding will be 64 bytes long
		return DigestUtils.sha256Hex(builder.toString());
	}

	public EDucSignatureStatus getSignatureStatus(UserInfo userInfo, String requestId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(requestId, "requestId");

		RequestInterface request = requestDao.get(requestId);

		if (!AuthorizationUtils.isUserCreatorOrAdmin(userInfo, request.getCreatedBy())) {
			throw new UnauthorizedException("Only the request creator or an administrator can view signature status.");
		}

		String envelopeId = request.getEDucSignatureEnvelopeId();
		if (envelopeId == null) {
			throw new IllegalArgumentException("This request does not have a routed DUC.");
		}

		EnvelopeStatusResult result = docuSignClient.getEnvelopeStatus(envelopeId);
		EDucSignatureStatus status = result.status();
		List<String> signerEmails = result.signerEmails();

		status.setDataAccessRequestId(requestId);
		// The routed document reflects the current request content only if the content hash
		// recorded at the last route/correct still matches the request's current content. A user
		// editing their request changes the content (but not the server-managed hash), which flips
		// this to false until the envelope is corrected.
		String storedHash = requestDao.getEDucContentHash(requestId);
		status.setIncludesRequestChanges(storedHash != null && storedHash.equals(computeEDucContentHash(request)));

		if (status.getSignerStatus() != null) {
			PrincipalInvestigator pi = request.getPrincipalInvestigator();
			for (int i = 0; i < status.getSignerStatus().size(); i++) {
				EDucSignerStatus signerStatus = status.getSignerStatus().get(i);
				String email = signerEmails.get(i);
				// The principal investigator is addressed at their institutional email, which need
				// not be a Synapse email alias, so take their user ID straight from the request.
				if (pi != null && Strings.CI.equals(email, pi.getInstitutionalEmail())) {
					signerStatus.setUserId(pi.getUserId());
				} else {
					PrincipalAlias principalAlias = principalAliasDao.findPrincipalWithAlias(email, AliasType.USER_EMAIL);
					if (principalAlias != null) {
						signerStatus.setUserId(principalAlias.getPrincipalId().toString());
					}
				}
			}
		}

		return status;
	}

	public void cancelSignature(UserInfo userInfo, String requestId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(requestId, "requestId");

		RequestInterface request = requestDao.get(requestId);

		if (!AuthorizationUtils.isUserCreatorOrAdmin(userInfo, request.getCreatedBy())) {
			throw new UnauthorizedException("Only the request creator or an administrator can cancel a signature.");
		}

		String envelopeId = request.getEDucSignatureEnvelopeId();
		if (envelopeId == null) {
			throw new IllegalArgumentException("This request does not have a routed DUC.");
		}

		docuSignClient.voidEnvelope(envelopeId, "Cancelled by user.");
		request.setEDucSignatureEnvelopeId(null);
		requestDao.update(request);
	}

	public EDucFileHandleId getSignedDocumentFileHandle(UserInfo userInfo, String requestId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(requestId, "requestId");

		RequestInterface request = requestDao.get(requestId);

		if (!AuthorizationUtils.isUserCreatorOrAdmin(userInfo, request.getCreatedBy())) {
			throw new UnauthorizedException("Only the request creator or an administrator can retrieve the signed document.");
		}

		String envelopeId = request.getEDucSignatureEnvelopeId();
		if (envelopeId == null) {
			throw new IllegalArgumentException("This request does not have a routed DUC.");
		}

		byte[] pdfBytes = docuSignClient.getSignedDocument(envelopeId);

		try {
			S3FileHandle fileHandle = fileHandleManager.createFileFromByteArray(
					userInfo.getId().toString(), new Date(), pdfBytes,
					"eDUC_" + requestId + ".pdf", ContentType.create("application/pdf"), null);
			EDucFileHandleId result = new EDucFileHandleId();
			result.setFileHandleId(fileHandle.getId());
			return result;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to upload signed document.", e);
		}
	}

	List<String> buildCollaboratorUserIds(RequestInterface request) {
		PrincipalInvestigator pi = request.getPrincipalInvestigator();
		ValidateArgument.required(pi, "principalInvestigator");
		String piUserId = pi.getUserId();

		LinkedHashSet<String> collaboratorIds = new LinkedHashSet<>();

		collaboratorIds.add(request.getCreatedBy());

		List<AccessorChange> accessorChanges = request.getAccessorChanges();
		if (accessorChanges != null) {
			for (AccessorChange change : accessorChanges) {
				if (AccessType.GAIN_ACCESS.equals(change.getType())
						|| AccessType.RENEW_ACCESS.equals(change.getType())) {
					collaboratorIds.add(change.getUserId());
				}
			}
		}

		collaboratorIds.remove(piUserId);

		return new ArrayList<>(collaboratorIds);
	}

	// A collaborator's looked-up data, gathered once and reused for both the recipient identity
	// and the tab values. email and userName are always present; fullName may be null when the
	// user's profile has no first or last name.
	private record CollaboratorInfo(String userId, String email, String userName, String fullName) {}

	/**
	 * Assigns each collaborator a numbered collaborator role.
	 * <p>
	 * A collaborator already on the envelope keeps the role they were routed with, and the role of
	 * someone who has already signed is never given to anyone else. Without this, roles were assigned
	 * by position in the accessor list, so removing or reordering an accessor shifted everyone after
	 * them onto a different role: a signature could end up attributed to the wrong person, and a
	 * collaborator whose role was occupied by a completed signer would never be asked to sign at all.
	 * Remaining collaborators take the lowest roles still free, so routing a request for the first
	 * time (no existing recipients) assigns them in order from collaborator_1.
	 * 
	 * @return a map whose key is the role name (the key to a signer in an envelope) and whose value is
	 * the info about the collaborator assigned to that role (Synapse ID, email, etc.)
	 */
	private Map<String, CollaboratorInfo> assignCollaboratorRoles(List<CollaboratorInfo> collaborators,
			List<EnvelopeRecipient> existingRecipients) {
		Map<String, CollaboratorInfo> unassigned = new LinkedHashMap<>();
		for (CollaboratorInfo collaborator : collaborators) {
			unassigned.put(collaborator.userId(), collaborator);
		}

		Map<String, CollaboratorInfo> byRole = new LinkedHashMap<>();
		Set<String> unavailableRoles = new HashSet<>();
		for (EnvelopeRecipient recipient : existingRecipients) {
			if (!recipient.roleName().startsWith(COLLABORATOR_ROLE_PREFIX)) {
				continue;
			}
			// An unrecognized address (the user may have changed it since the envelope was routed)
			// leaves the role free: whoever ends up with it is sent a new invitation, which is
			// preferable to attributing the role to the wrong person.
			CollaboratorInfo occupant = unassigned.remove(resolveUserId(recipient.email()));
			if (occupant != null) {
				byRole.put(recipient.roleName(), occupant);
				unavailableRoles.add(recipient.roleName());
			} else if (recipient.completed()) {
				// Signed by someone who is no longer a collaborator. The signature stands (DocuSign
				// does not allow removing it) so the role is retired rather than reused.
				unavailableRoles.add(recipient.roleName());
			}
		}

		int nextIndex = 1;
		for (CollaboratorInfo collaborator : unassigned.values()) {
			while (unavailableRoles.contains(collaboratorRole(nextIndex))) {
				nextIndex++;
			}
			String role = collaboratorRole(nextIndex);
			byRole.put(role, collaborator);
			unavailableRoles.add(role);
		}
		return byRole;
	}

	// The Synapse user an envelope recipient's address belongs to, or null if it belongs to none.
	private String resolveUserId(String email) {
		if (StringUtils.isBlank(email)) {
			return null;
		}
		PrincipalAlias alias = principalAliasDao.findPrincipalWithAlias(email, AliasType.USER_EMAIL);
		return alias == null ? null : alias.getPrincipalId().toString();
	}

	private static String collaboratorRole(int index) {
		return COLLABORATOR_ROLE_PREFIX + index;
	}

	private List<CollaboratorInfo> gatherCollaboratorInfos(List<String> collaboratorUserIds) {
		List<CollaboratorInfo> collaborators = new ArrayList<>(collaboratorUserIds.size());
		for (String collabUserId : collaboratorUserIds) {
			long principalId = Long.parseLong(collabUserId);
			String email = notificationEmailDao.getNotificationEmailForPrincipal(principalId);
			String userName = principalAliasDao.getUserName(principalId);
			UserProfile profile = userProfileDao.get(collabUserId);
			String fullName = buildFullName(profile.getFirstName(), profile.getLastName());
			collaborators.add(new CollaboratorInfo(collabUserId, email, userName, fullName));
		}
		return collaborators;
	}

	/**
	 * Builds the map from DocuSign role name to the recipient's email and name. Every role must
	 * have both before the envelope can be sent. The signing official's and principal
	 * investigator's institutional emails and names come from the request (their presence is
	 * validated in {@link #createDraftEDuc}). A collaborator is addressed at their Synapse
	 * notification email, and their name comes from their user profile (first and/or last name)
	 * when available, otherwise their Synapse user name, which every user is guaranteed to have.
	 */
	private Map<String, RecipientInfo> buildRecipients(RequestInterface request,
			Map<String, CollaboratorInfo> collaboratorsByRole) {
		Map<String, RecipientInfo> recipients = new LinkedHashMap<>();

		PrincipalInvestigator pi = request.getPrincipalInvestigator();
		recipients.put("principal_investigator", new RecipientInfo(pi.getInstitutionalEmail(), pi.getName()));

		SigningOfficial so = request.getSigningOfficial();
		recipients.put("signing_official", new RecipientInfo(so.getInstitutionalEmail(), so.getName()));

		for (Map.Entry<String, CollaboratorInfo> entry : collaboratorsByRole.entrySet()) {
			CollaboratorInfo collaborator = entry.getValue();
			String name = StringUtils.isBlank(collaborator.fullName())
					? collaborator.userName() : collaborator.fullName();
			recipients.put(entry.getKey(), new RecipientInfo(collaborator.email(), name));
		}

		return recipients;
	}

	private Map<RoleLabelKey, String> buildTabValues(RequestInterface request,
			Map<String, CollaboratorInfo> collaboratorsByRole) {
		Map<RoleLabelKey, String> tabValues = new LinkedHashMap<>();

		SigningOfficial so = request.getSigningOfficial();
		addIfPresent(tabValues, "signing_official", "signing_official_name", so.getName());
		addIfPresent(tabValues, "signing_official", "signing_official_email", so.getInstitutionalEmail());
		addIfPresent(tabValues, "signing_official", "signing_official_institution", request.getInstitution());

		PrincipalInvestigator pi = request.getPrincipalInvestigator();
		addIfPresent(tabValues, "principal_investigator", "principal_investigator_name", pi.getName());
		addIfPresent(tabValues, "principal_investigator", "principal_investigator_email", pi.getInstitutionalEmail());

		String piUserName = principalAliasDao.getUserName(Long.parseLong(pi.getUserId()));
		addIfPresent(tabValues, "principal_investigator", "principal_investigator_user_name", piUserName);

		for (Map.Entry<String, CollaboratorInfo> entry : collaboratorsByRole.entrySet()) {
			String role = entry.getKey();
			CollaboratorInfo collaborator = entry.getValue();
			addIfPresent(tabValues, role, role + "_user_name", collaborator.userName());
			addIfPresent(tabValues, role, role + "_name", collaborator.fullName());
		}

		return tabValues;
	}

	private static void addIfPresent(Map<RoleLabelKey, String> tabValues, String roleName,
			String tabLabel, String value) {
		if (StringUtils.isNotEmpty(value)) {
			tabValues.put(new RoleLabelKey(roleName, tabLabel), value);
		}
	}

	static String buildFullName(String firstName, String lastName) {
		if (firstName == null && lastName == null) {
			return null;
		}
		if (firstName == null) {
			return lastName;
		}
		if (lastName == null) {
			return firstName;
		}
		return firstName + " " + lastName;
	}
}
