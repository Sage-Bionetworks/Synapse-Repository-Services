package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksResponse;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookResponse;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.service.webhook.WebhookService;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Provides APIs to manage webhooks that are triggered by synapse events.
 * <p>
 * A webhook can specify the id of a synapse entity to receive events for. For entities that represent a container (folder and project), 
 * matching events are published to the webhook endpoint for any entity in their hierarchy.
 * <p>
 * Before events are published to the endpoint, the webhook needs to be verified. A special request is sent to the webhook endpoint 
 * containing a verification code that can be submitted in the body of the <a href="${POST.webhook.webhookId.verify}">POST /webhook/{webhookId}/verify</a> request.
 * <p>
 * There are two types of requests that can be sent to an endpoint:
 * <ul>
 * <li><a href="${org.sagebionetworks.repo.model.webhook.WebhookVerificationMessage}">WebhookVerificationMessage</a>: Sent when a webhook is created or when its endpoint is updated that contains the verification code used to verify the webhook.</li>
 * <li><a href="${org.sagebionetworks.repo.model.webhook.WebhookSynapseEventMessage}">WebhookSynapseEventMessage</a>: Sent when a synapse event that match the webhook entity is generated in the backend.</li>
 * </ul>
 * Each HTTP request will contain the following headers:
 * <ul>
 * <li><b>X-Syn-Webhook-Id:</b> The id of the webhook</li>
 * <li><b>X-Syn-Webhook-Message-Type:</b> The type of message body, either Verification or SynapseEvent</li>
 * <li><b>X-Syn-Webhook-Message-Id:</b> A unique id for the message</li>
 * <li><b>X-Syn-Webhook-Owner-Id:</b> The id of the user that created the webhook</li>
 * </ul>
 * <p>
 * A request to an endpoint is sent at least once and retried for a maximum of 3 times. The endpoint needs to provide a response within 2 seconds in order for the request to be considered successful, 
 * otherwise the request is retried. Valid HTTP response codes are: 
 * <ul>
 * <li>200 (OK)</li> 
 * <li>201 (CREATED)</li>
 * <li>202 (ACCEPTED)</li>
 * <li>204 (NO CONTENT)</li> 
 * </ul>
 * The following response codes are considered for retrying: 
 * <ul>
 * <li>429 (TOO MANY REQUESTS)</li>
 * <li>500 (INTERNAL SERVER ERROR)</li>
 * <li>502 (BAD GATEWAY)</li>
 * <li>503 (SERVICE UNAVAILABLE)</li>
 * <li>504 (GATEWAY TIMEOUT)</li> 
 * </ul>
 * Any other response code is considered a failed delivery and won't be retried. If a webhook endpoint consistently fails to respond to requests, it will eventually be disabled 
 * and the <a href="${org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus}">verification status</a> of the webhook will be set to REVOKED and will need to be re-verified.
 * <p>
 * Each user is limited to a maximum of 25 webhooks.
 */
@ControllerInfo(displayName = "Webhook Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class WebhookController {
	
	private final WebhookService service;
	
	public WebhookController(WebhookService service) {
		this.service = service;
	}
	
	/**
	 * Create a new webhook. A user can create a maximum of 25 webhooks. Once created the webhook needs to be verified 
	 * using the <a href="${POST.webhook.webhookId.verify}">POST /webhook/{webhookId}/verify</a> request with the 
	 * verification code received by the webhook endpoint.
	 * <p>
	 * The webhook endpoint is checked against a white list of allowed domain patterns, currently only the AWS Api Gateway <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/how-to-call-api.html">execute-api</a> is allowed.
	 * For adding new domain exception you can submit a request to the <a href="https://sagebionetworks.jira.com/servicedesk/customer/portal/9">Synapse Service Desk</a>.
	 * <p>
	 * The caller must have READ permissions on the entity specified in the request, this permission needs to be maintained in order to receive events. 
	 *  
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.WEBHOOK }, method = RequestMethod.POST)
	public @ResponseBody Webhook createWebhook(
		@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
		@RequestBody CreateOrUpdateWebhookRequest request) {
		return service.create(userId, request);
	}
	
	/**
	 * Get the webhook with the provided id, the caller must be the creator of the webhook.
	 * 
	 * @param userId
	 * @param webhookId
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.WEBHOOK_ID }, method = RequestMethod.GET)
	public @ResponseBody Webhook getWebhook(
		@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
		@PathVariable(value = "webhookId") String webhookId) {
		return service.get(userId, webhookId);
	}
	
	/**
	 * Get the list of webhooks for the caller.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.WEBHOOK_LIST }, method = RequestMethod.POST)
	public @ResponseBody ListUserWebhooksResponse listUserWebhooks(
		@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
		@RequestBody ListUserWebhooksRequest request) {
		return service.list(userId, request);
	}
	
	/**
	 * Update the webhook with the given id, the caller must be the creator of the webhook. 
	 * <p>
	 * If the endpoint is modified the webhook <a href="${org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus}">verification status</a> 
	 * will be set to PENDING and a new verification code will be sent to the endpoint that can be used with 
	 * the <a href="${POST.webhook.webhookId.verify}">POST /webhook/{webhookId}/verify</a> request.
	 * <p>
	 * The caller must have READ permissions on the entity specified in the request, this permission needs to be maintained in order to receive events. 
	 * 
	 * @param userId
	 * @param webhookId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.WEBHOOK_ID }, method = RequestMethod.PUT)
	public @ResponseBody Webhook updateWebhook(
		@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
		@PathVariable(value = "webhookId") String webhookId,
		@RequestBody CreateOrUpdateWebhookRequest request) {
		return service.update(userId, webhookId, request);
	}	

	/**
	 * Verifies a webhook with the verificationCode received by the endpoint in a <a href="${org.sagebionetworks.repo.model.webhook.WebhookVerificationMessage}">WebhookVerificationMessage</a> request. 
	 * The caller must be the creator of the webhook.
	 * <p>
	 * In order to perform this call the webhook <a href="${org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus}">verification status</a> must be CODE_SENT.
	 * @param userId
	 * @param webhookId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.WEBHOOK_VERIFY }, method = RequestMethod.POST)
	public @ResponseBody VerifyWebhookResponse verifyWebhook(
		@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
		@PathVariable(value = "webhookId") String webhookId,
		@RequestBody VerifyWebhookRequest request) {
		return service.verify(userId, webhookId, request);
	}
	
	/**
	 * When the verification fails or if a verification code is lost or wasn't received, this API allows to sent a new verification code to the webhook endpoint. The caller must be the creator of the webhook.
	 * 
	 * @param userId
	 * @param webhookId
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.WEBHOOK_VERIFICATION_CODE }, method = RequestMethod.PUT)
	public @ResponseBody Webhook generateWebhookVerificationCode(
		@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
		@PathVariable(value = "webhookId") String webhookId) {
		return service.generateVerificationCode(userId, webhookId);
	}
	
	/**
	 * Deletes the webhook with the given id, the caller must be the creator of the webhook.
	 * @param userId
	 * @param webhookId
	 */
	@RequiredScope({ modify })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.WEBHOOK_ID }, method = RequestMethod.DELETE)
	public void deleteWebhook(
		@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
		@PathVariable(value = "webhookId") String webhookId) {
		service.delete(userId, webhookId);
	}
	

}
