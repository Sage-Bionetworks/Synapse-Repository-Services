package org.sagebionetworks.repo.manager.docusign;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.sagebionetworks.docusign.DocuSignClient;
import org.sagebionetworks.docusign.EnvelopeStatusResult;
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

		AccessRequirement ar = accessRequirementDao.get(request.getAccessRequirementId());
		if (!(ar instanceof ManagedACTAccessRequirement managedAr)) {
			throw new IllegalArgumentException("The access requirement is not a ManagedACTAccessRequirement.");
		}
		if (!Boolean.TRUE.equals(managedAr.getIsDUCRequired())) {
			throw new IllegalArgumentException("The access requirement does not require a DUC.");
		}

		String templateId = managedAr.getEDucTemplateId();
		if (StringUtils.isBlank(templateId)) {
			throw new IllegalArgumentException("The access requirement does not have an eDUC template ID configured.");
		}

		PrincipalInvestigator pi = request.getPrincipalInvestigator();
		ValidateArgument.required(pi, "principalInvestigator");
		ValidateArgument.required(pi.getUserId(), "principalInvestigator.userId");

		SigningOfficial so = request.getSigningOfficial();
		ValidateArgument.required(so, "signingOfficial");
		ValidateArgument.required(so.getInstitutionalEmail(), "signingOfficial.institutionalEmail");

		List<String> collaboratorUserIds = buildCollaboratorUserIds(request);
		Map<String, String> roleEmails = buildRoleEmails(request, collaboratorUserIds);
		Map<RoleLabelKey, String> tabValues = buildTabValues(request, collaboratorUserIds);

		String envelopeId = docuSignClient.createEnvelope(templateId, roleEmails, tabValues);

		request.setEDucSignatureEnvelopeId(envelopeId);
		requestDao.update(request);
		return request;
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
		// TODO PLFM-9657 will set the following to show whether
		// changes to the request have been applied to the routed document
		status.setIncludesRequestChanges(true);

		if (status.getSignerStatus() != null) {
			for (int i = 0; i < status.getSignerStatus().size(); i++) {
				EDucSignerStatus signerStatus = status.getSignerStatus().get(i);
				String email = signerEmails.get(i);
				PrincipalAlias principalAlias = principalAliasDao.findPrincipalWithAlias(email, AliasType.USER_EMAIL);
				if (principalAlias != null) {
					signerStatus.setUserId(principalAlias.getPrincipalId().toString());
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

	private Map<String, String> buildRoleEmails(RequestInterface request, List<String> collaboratorUserIds) {
		Map<String, String> roleEmails = new LinkedHashMap<>();

		PrincipalInvestigator pi = request.getPrincipalInvestigator();
		roleEmails.put("principal_investigator",
				notificationEmailDao.getNotificationEmailForPrincipal(Long.parseLong(pi.getUserId())));

		SigningOfficial so = request.getSigningOfficial();
		ValidateArgument.required(so, "signingOfficial");
		roleEmails.put("signing_official", so.getInstitutionalEmail());

		for (int i = 0; i < collaboratorUserIds.size(); i++) {
			String email = notificationEmailDao.getNotificationEmailForPrincipal(
					Long.parseLong(collaboratorUserIds.get(i)));
			roleEmails.put("collaborator_" + (i + 1), email);
		}

		return roleEmails;
	}

	private Map<RoleLabelKey, String> buildTabValues(RequestInterface request, List<String> collaboratorUserIds) {
		Map<RoleLabelKey, String> tabValues = new LinkedHashMap<>();

		SigningOfficial so = request.getSigningOfficial();
		addIfPresent(tabValues, "signing_official", "signing_official_name", so.getName());
		addIfPresent(tabValues, "signing_official", "signing_official_title", so.getTitle());
		addIfPresent(tabValues, "signing_official", "signing_official_email", so.getInstitutionalEmail());
		addIfPresent(tabValues, "signing_official", "signing_official_institution", request.getInstitution());

		PrincipalInvestigator pi = request.getPrincipalInvestigator();
		addIfPresent(tabValues, "principal_investigator", "principal_investigator_name", pi.getName());
		addIfPresent(tabValues, "principal_investigator", "principal_investigator_title", pi.getTitle());
		addIfPresent(tabValues, "principal_investigator", "principal_investigator_email", pi.getInstitutionalEmail());

		String piUserName = principalAliasDao.getUserName(Long.parseLong(pi.getUserId()));
		addIfPresent(tabValues, "principal_investigator", "principal_investigator_user_name", piUserName);

		for (int i = 0; i < collaboratorUserIds.size(); i++) {
			String role = "collaborator_" + (i + 1);
			String collabUserId = collaboratorUserIds.get(i);

			String userName = principalAliasDao.getUserName(Long.parseLong(collabUserId));
			addIfPresent(tabValues, role, role + "_user_name", userName);

			UserProfile profile = userProfileDao.get(collabUserId);
			String fullName = buildFullName(profile.getFirstName(), profile.getLastName());
			addIfPresent(tabValues, role, role + "_name", fullName);
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
