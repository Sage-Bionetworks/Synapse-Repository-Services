package org.sagebionetworks.repo.manager;

import java.sql.Timestamp;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.webhook.WebhookDao;
import org.sagebionetworks.repo.model.dbo.dao.webhook.WebhookVerificationDao;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksResponse;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookResponse;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookObjectType;
import org.sagebionetworks.repo.model.webhook.WebhookVerification;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class WebhookManagerImpl implements WebhookManager {
	
	@Autowired
	private WebhookDao webhookDao;
	
	@Autowired
	private WebhookVerificationDao webhookVerificationDao;
	
	@Autowired
	private AccessControlListDAO aclDao;
	
	@Autowired
	private UserManager userManager;
	

	public static final String UNAUTHORIZED_ACCESS_MESSAGE = "You do not have permission to access the given webhook.";
	public static final String EXPIRED_VERIFICATION_CODE_MESSAGE = "The verification code provided has expired.";
	public static final String INVALID_VERIFICATION_CODE_MESSAGE = "The verificaiton code provided is invalid.";
	public static final String INVALID_WEBHOOK_ID = "The provided webhookId is invalid.";
	public static final String MISSING_OBJECT_UPDATE_PARAMS = "If you would like to update the object, both the ObjectId and the ObjectType are required.";
	public static final String INVALID_INVOKE_ENDPOINT_MESSAGE = "The invokeEndpoint: %s is invalid.";
	
	private static final String AWS_API_GATEWAY_REGEX = "^https://[a-zA-Z0-9-]+\\.execute-api\\.[a-zA-Z0-9-]+\\.amazonaws\\.com(/.*)?$";
    private static final Pattern AWS_API_GATEWAY_PATTERN = Pattern.compile(AWS_API_GATEWAY_REGEX);
	
	@WriteTransaction
	@Override
	public Webhook createWebhook(UserInfo userInfo, CreateOrUpdateWebhookRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "createOrUpdateWebhookRequest");
		AuthorizationUtils.disallowAnonymous(userInfo);
		
		validateUserCanReadObject(userInfo, request.getObjectId(), request.getObjectType());
		validateInvokeEndpoint(request.getInvokeEndpoint());
		
		if (request.getIsWebhookEnabled() == null) {
			request.setIsWebhookEnabled(true);
		}
		
		Webhook webhook = webhookDao.createWebhook(userInfo.getId(), request); 
		generateAndSendVerificationCode(userInfo, webhook.getWebhookId());
		return webhook;
	}

	@Override
	public Webhook getWebhook(UserInfo userInfo, String webhookId) {
		validateUserIsAdminOrWebhookOwner(userInfo, webhookId);
		return webhookDao.getWebhook(webhookId);
	}

	@WriteTransaction
	@Override
	public Webhook updateWebhook(UserInfo userInfo, String webhookId, CreateOrUpdateWebhookRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(webhookId, "webhookId");
		ValidateArgument.required(request, "createOrUpdateWebhookRequest");
		validateUserIsAdminOrWebhookOwner(userInfo, webhookId);
		
		boolean includesObjectId = request.getObjectId() != null;
		boolean includesObjectType = request.getObjectType() != null;
		boolean includesInvokeEndpoint = request.getInvokeEndpoint() != null;
		
		// If updating the object, verify the user has read permission on the new object
		if (includesObjectId || includesObjectType) {
			ValidateArgument.requirement(includesObjectId && includesObjectType, MISSING_OBJECT_UPDATE_PARAMS);
			validateUserCanReadObject(userInfo, request.getObjectId(), request.getObjectType());
		}
		
		if (includesInvokeEndpoint) {
			validateInvokeEndpoint(request.getInvokeEndpoint());
		}
		
		Webhook updatedWebhook = webhookDao.updateWebhook(userInfo.getId(), webhookId, request);
		
		if (includesInvokeEndpoint) {
			generateAndSendVerificationCode(userInfo, updatedWebhook.getWebhookId());
		}
		
		return updatedWebhook;
	}

	@WriteTransaction
	@Override
	public void deleteWebhook(UserInfo userInfo, String webhookId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(webhookId, "webhookId");
		validateUserIsAdminOrWebhookOwner(userInfo, webhookId);
		webhookDao.deleteWebhook(webhookId);
	}

	@Override
	public VerifyWebhookResponse validateWebhook(UserInfo userInfo, String webhookId, VerifyWebhookRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(webhookId, "webhookId");
		ValidateArgument.required(request, "validateWebhookRequest");
		ValidateArgument.required(request.getVerificationCode(), "validateWebhookRequest.verificaionCode");
		
		WebhookVerification webhookVerification = webhookVerificationDao.getWebhookVerification(webhookId);
		VerifyWebhookResponse response = new VerifyWebhookResponse().setWebhookId(webhookId);
		
		if (request.getVerificationCode().equals(webhookVerification.getVerificationCode())) {
			if (webhookVerification.getExpiresOn().after(new Timestamp(System.currentTimeMillis()))) {
				webhookDao.setWebhookVerificationStatus(webhookId, true);
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
		List<Webhook> page = webhookDao.listUserWebhooks(userInfo.getId(), nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
		
		return new ListUserWebhooksResponse()
				.setPage(page)
				.setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(page));
	}
	
	@Override
	public List<Webhook> listSendableWebhooksForObjectId(String objectId) {
		return webhookDao.listVerifiedAndEnabledWebhooksForObjectId(objectId).stream()
				.filter(webhook -> {
					UserInfo userInfo = userManager.getUserInfo(Long.parseLong(webhook.getCreatedBy()));
					try {
						validateUserCanReadObject(userInfo, webhook.getObjectId(), webhook.getObjectType());
                        return true; 
                    } catch (RuntimeException e) {
                        return false; 
                    }
				}).collect(Collectors.toList());
	}
	
	private void validateUserIsAdminOrWebhookOwner(UserInfo userInfo, String webhookId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(webhookId, "webhookId");
		
		if (userInfo.isAdmin()) {
			return;
		}
		
		String webhookOwner = webhookDao.getWebhookOwnerForUpdate(webhookId)
				.orElseThrow(() -> new IllegalArgumentException(INVALID_WEBHOOK_ID));

		if (userInfo.getId().toString().equals(webhookOwner)) {
			return;
		}
		
		throw new UnauthorizedException(UNAUTHORIZED_ACCESS_MESSAGE);
	}
	
	private void validateUserCanReadObject(UserInfo userInfo, String objectId, WebhookObjectType webhookObjectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(webhookObjectType, "webhookObjectType");
		
		ObjectType objectType = ObjectType.valueOf(webhookObjectType.name().toUpperCase());
		aclDao.canAccess(userInfo, objectId, objectType, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();;
	}
	
	private void validateInvokeEndpoint(String invokeEndpoint) {
		ValidateArgument.required(invokeEndpoint, "invokeEndpoint");
		
		if (AWS_API_GATEWAY_PATTERN.matcher(invokeEndpoint).matches()) {
			return;
		}
		
		throw new IllegalArgumentException(String.format(INVALID_INVOKE_ENDPOINT_MESSAGE, invokeEndpoint));
	}
	
	private void generateAndSendVerificationCode(UserInfo userInfo, String webhookId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(webhookId, "webhookId");
		
		WebhookVerification webhookVerification = webhookVerificationDao.createWebhookVerification(userInfo.getId(), webhookId);
	}

}

