package org.sagebionetworks.repo.manager;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.dao.webhook.WebhookDao;
import org.sagebionetworks.repo.model.dbo.dao.webhook.WebhookVerificationDao;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksResponse;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookResponse;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookObjectType;
import org.sagebionetworks.repo.model.webhook.WebhookVerification;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.regions.Regions;

public class WebhookManagerImpl implements WebhookManager {

	@Autowired
	private WebhookDao webhookDao;

	@Autowired
	private WebhookVerificationDao webhookVerificationDao;

	@Autowired
	private AccessControlListDAO aclDao;

	@Autowired
	private UserManager userManager;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private Clock clock;

	public static final String UNAUTHORIZED_ACCESS_MESSAGE = "You do not have permission to access the provided webhook.";
	public static final String EXPIRED_VERIFICATION_CODE_MESSAGE = "The verification code provided has expired.";
	public static final String INVALID_VERIFICATION_CODE_MESSAGE = "The verification code provided is invalid.";
	public static final String INVALID_WEBHOOK_ID = "The provided webhookId is invalid.";
	public static final String INVALID_INVOKE_ENDPOINT_MESSAGE = "The invokeEndpoint: %s is invalid.";
	public static final String CONFLICTING_UPDATE_MESSAGE = "Webhook: %s was updated since you last fetched it, retrieve it again and re-apply the update";
	public static final String EXCEEDED_MAXIMUM_ATTEMPTS = "You have exceeded the maximum number of attempts to verify your Webhook. Your Webhook has been deleted.";

	private static final SecureRandom secureRandom = new SecureRandom();
	public static final String VERIFICATION_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	public static final long VERIFICATION_CODE_TTL = 10 * 60 * 1000; // 10 minutes
	public static final int VERIFICATION_CODE_LENGTH = 6;
	public static final long MAXIMUM_VERIFICATION_ATTEMPTS = 10;

	private static final String AWS_REGION_REGEX = Stream.of(Regions.values()).map(Regions::getName)
			.collect(Collectors.joining("|"));
	private static final String AWS_API_GATEWAY_REGEX = "^https://[a-zA-Z0-9-]+\\.execute-api\\.(" + AWS_REGION_REGEX
			+ ")\\.amazonaws\\.com(/.*)?$";
	private static final Pattern AWS_API_GATEWAY_PATTERN = Pattern.compile(AWS_API_GATEWAY_REGEX);
	public static final Set<Pattern> ALLOWED_INVOKE_DOMAINS = Set.of(AWS_API_GATEWAY_PATTERN);

	@WriteTransaction
	@Override
	public Webhook createWebhook(UserInfo userInfo, Webhook toCreate) {
		validateCreateOrUpdateArguments(userInfo, toCreate);

		aclDao.canAccess(userInfo, toCreate.getObjectId(), ObjectType.valueOf(toCreate.getObjectType().name()),
				ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();

		String userIdAsString = userInfo.getId().toString();
		Date currentDate = clock.now();
		toCreate.setWebhookId(idGenerator.generateNewId(IdType.WEBHOOK_ID).toString())
				.setObjectId(toCreate.getObjectId()).setObjectType(toCreate.getObjectType()).setUserId(userIdAsString)
				.setInvokeEndpoint(toCreate.getInvokeEndpoint()).setIsVerified(false)
				.setIsWebhookEnabled(toCreate.getIsWebhookEnabled() == null ? true : toCreate.getIsWebhookEnabled())
				.setIsAuthenticationEnabled(
						toCreate.getIsAuthenticationEnabled() == null ? true : toCreate.getIsAuthenticationEnabled())
				.setEtag(UUID.randomUUID().toString()).setCreatedBy(userIdAsString).setModifiedBy(userIdAsString)
				.setCreatedOn(currentDate).setModifiedOn(currentDate);

		Webhook createdWebhook = webhookDao.createWebhook(toCreate);
		generateAndSendWebhookVerification(createdWebhook);
		return createdWebhook;
	}

	@Override
	public Webhook getWebhook(UserInfo userInfo, String webhookId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(webhookId, "webhookId");

		Webhook webhook = webhookDao.getWebhook(webhookId);
		validateUserIsAdminOrWebhookOwner(userInfo, webhook);
		return webhook;
	}

	@WriteTransaction
	@Override
	public Webhook updateWebhook(UserInfo userInfo, Webhook updateWith) {
		validateCreateOrUpdateArguments(userInfo, updateWith);
		ValidateArgument.required(updateWith.getEtag(), "updateWith.etag");

		aclDao.canAccess(userInfo, updateWith.getObjectId(), ObjectType.valueOf(updateWith.getObjectType().name()),
				ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();

		Webhook originalWebhook = lockAndValidateOwner(userInfo, updateWith.getWebhookId());
		if (!updateWith.getEtag().equals(originalWebhook.getEtag())) {
			throw new ConflictingUpdateException(
					String.format(CONFLICTING_UPDATE_MESSAGE, originalWebhook.getWebhookId()));
		}

		originalWebhook.setObjectId(updateWith.getObjectId()).setObjectType(updateWith.getObjectType())
				.setInvokeEndpoint(updateWith.getInvokeEndpoint())
				.setIsWebhookEnabled(updateWith.getIsWebhookEnabled() == null ? originalWebhook.getIsWebhookEnabled()
						: updateWith.getIsWebhookEnabled())
				.setIsAuthenticationEnabled(
						updateWith.getIsAuthenticationEnabled() == null ? originalWebhook.getIsAuthenticationEnabled()
								: updateWith.getIsAuthenticationEnabled())
				.setEtag(UUID.randomUUID().toString()).setModifiedBy(userInfo.getId().toString())
				.setModifiedOn(clock.now());

		Webhook updatedWebhook = webhookDao.updateWebhook(originalWebhook);
		generateAndSendWebhookVerification(updatedWebhook);
		return updatedWebhook;
	}

	@WriteTransaction
	@Override
	public void deleteWebhook(UserInfo userInfo, String webhookId) {
		getWebhook(userInfo, webhookId); // performs validation
		webhookDao.deleteWebhook(webhookId);
	}

	void deleteWebhookAsAdmin(String webhookId) {
		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		deleteWebhook(adminUserInfo, webhookId);
	}

	@Override
	public VerifyWebhookResponse verifyWebhook(UserInfo userInfo, VerifyWebhookRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "verifyWebhookRequest");
		ValidateArgument.required(request.getWebhookId(), "verifyWebhookRequest.webhookId");
		ValidateArgument.required(request.getVerificationCode(), "verifyWebhookRequest.verificationCode");

		// We need to lock the Webhook before getting the WebhookVerification to ensure
		// the WebhookVerification is not stale
		Webhook webhook = lockAndValidateOwner(userInfo, request.getWebhookId());
		WebhookVerification webhookVerification = webhookVerificationDao.getWebhookVerification(webhook.getWebhookId());

		VerifyWebhookResponse response = new VerifyWebhookResponse().setWebhookId(webhook.getWebhookId());

		if (webhookVerificationDao.incrementAttempts(webhook.getWebhookId()) > MAXIMUM_VERIFICATION_ATTEMPTS) {
			deleteWebhookAsAdmin(webhook.getWebhookId());
			return response.setIsValid(false).setInvalidReason(EXCEEDED_MAXIMUM_ATTEMPTS);
		}

		if (request.getVerificationCode().equals(webhookVerification.getVerificationCode())) {
			if (webhookVerification.getExpiresOn().after(clock.now())) {
				webhook.setIsVerified(true);
				webhookDao.updateWebhook(webhook);
				return response.setIsValid(true);
			} else {
				return response.setIsValid(false).setInvalidReason(EXPIRED_VERIFICATION_CODE_MESSAGE);
			}
		}

		return response.setIsValid(false).setInvalidReason(INVALID_VERIFICATION_CODE_MESSAGE);
	}

	@Override
	public ListUserWebhooksResponse listUserWebhooks(UserInfo userInfo, ListUserWebhooksRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "listUserWebhooksRequest");

		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());
		List<Webhook> page = webhookDao.listUserWebhooks(userInfo.getId(), nextPageToken.getLimitForQuery(),
				nextPageToken.getOffset());

		return new ListUserWebhooksResponse().setPage(page)
				.setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(page));
	}

	@Override
	public List<Webhook> listSendableWebhooksForObjectId(String objectId, WebhookObjectType webhookObjectType) {
		return webhookDao
				.listVerifiedAndEnabledWebhooksForObjectId(objectId, ObjectType.valueOf(webhookObjectType.name()))
				.stream().filter(webhook -> {
					try {
						UserInfo userInfo = userManager.getUserInfo(Long.parseLong(webhook.getUserId()));
						return aclDao
								.canAccess(userInfo, webhook.getObjectId(),
										ObjectType.valueOf(webhook.getObjectType().name()), ACCESS_TYPE.READ)
								.isAuthorized();
					} catch (NumberFormatException | NotFoundException e) {
						deleteWebhookAsAdmin(webhook.getWebhookId());
						return false;
					}
				}).collect(Collectors.toList());
	}

	@Override
	public void generateAndSendWebhookVerification(UserInfo userInfo, String webhookId) {
		Webhook webhook = getWebhook(userInfo, webhookId); // performs validation
		generateAndSendWebhookVerification(webhook);
	}

	void generateAndSendWebhookVerification(Webhook webhook) {
		ValidateArgument.required(webhook, "webhook");
		ValidateArgument.required(webhook.getWebhookId(), "webhook.webhookId");
		ValidateArgument.required(webhook.getUserId(), "webhook.userId");

		Date currentDate = clock.now();
		Date expirationDate = new Date(currentDate.getTime() + VERIFICATION_CODE_TTL);

		WebhookVerification toCreate = new WebhookVerification().setWebhookId(webhook.getWebhookId())
				.setVerificationCode(generateVerificationCode()).setExpiresOn(expirationDate).setAttempts(0L)
				.setCreatedBy(webhook.getUserId()).setCreatedOn(currentDate);

		webhookVerificationDao.createWebhookVerification(toCreate);

		// TODO
		// re-using some of the distribution worker logic, send the newly created
		// WebhookVerification to the user's invokeEndpoint
	}

	Webhook lockAndValidateOwner(UserInfo userInfo, String webhookId) {
		Webhook webhook = webhookDao.getWebhookForUpdate(webhookId);
		validateUserIsAdminOrWebhookOwner(userInfo, webhook);
		return webhook;
	}

	void validateUserIsAdminOrWebhookOwner(UserInfo userInfo, Webhook webhook) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(webhook, "webhook");
		if (!userInfo.isAdmin() && !userInfo.getId().toString().equals(webhook.getUserId())) {
			throw new UnauthorizedException(UNAUTHORIZED_ACCESS_MESSAGE);
		}
	}

	void validateCreateOrUpdateArguments(UserInfo userInfo, Webhook webhook) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(webhook, "webhook");
		ValidateArgument.required(webhook.getObjectId(), "webhook.objectId");
		ValidateArgument.required(webhook.getObjectType(), "webhook.objectType");
		ValidateArgument.required(webhook.getInvokeEndpoint(), "webhook.invokeEndpoint");

		AuthorizationUtils.disallowAnonymous(userInfo);
		validateInvokeEndpoint(webhook.getInvokeEndpoint());
	}

	void validateInvokeEndpoint(String invokeEndpoint) {
		ValidateArgument.required(invokeEndpoint, "invokeEndpoint");

		for (Pattern pattern : ALLOWED_INVOKE_DOMAINS) {
			if (pattern.matcher(invokeEndpoint).matches()) {
				return;
			}
		}

		throw new IllegalArgumentException(String.format(INVALID_INVOKE_ENDPOINT_MESSAGE, invokeEndpoint));
	}

	String generateVerificationCode() {
		StringBuilder sb = new StringBuilder(VERIFICATION_CODE_LENGTH);
		for (int i = 0; i < VERIFICATION_CODE_LENGTH; i++) {
			int randomIndex = secureRandom.nextInt(VERIFICATION_CODE_CHARACTERS.length());
			sb.append(VERIFICATION_CODE_CHARACTERS.charAt(randomIndex));
		}
		return sb.toString();
	}
}
