package org.sagebionetworks.webhook.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.manager.webhook.WebhookManager.MSG_ATTR_WEBHOOK_ID;
import static org.sagebionetworks.repo.manager.webhook.WebhookManager.MSG_ATTR_WEBHOOK_MESSAGE_TYPE;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.AwsClientFactory;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.manager.webhook.WebhookManager;
import org.sagebionetworks.repo.manager.webhook.WebhookMessageType;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.webhook.WebhookDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.FileHandleObjectHelper;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.SynapseEventType;
import org.sagebionetworks.repo.model.webhook.SynapseObjectType;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookMessage;
import org.sagebionetworks.repo.model.webhook.WebhookSynapseEventMessage;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationMessage;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.apigatewayv2.AmazonApiGatewayV2;
import com.amazonaws.services.apigatewayv2.model.Api;
import com.amazonaws.services.apigatewayv2.model.GetApisRequest;
import com.amazonaws.services.apigatewayv2.model.GetApisResult;
import com.amazonaws.services.apigatewayv2.model.UpdateApiRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class WebhookWorkerIntegrationTest {
	
	private static final int TIMEOUT = 60_000;
	
	private static AmazonApiGatewayV2 apiGatewayClient;
	private static AmazonSQS sqsClient;
	
	private static Api testApi;
	private static String apiTestQueueUrl;
	private static String deadLetterQueueUrl;

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private TrashManager trashManager;
	
	@Autowired
	private AccessControlListObjectHelper aclHelper;
	
	@Autowired
	private FileHandleObjectHelper fileHelper;
	
	@Autowired
	private WebhookManager webhookManager;
		
	@Autowired
	private WebhookDao webhookDao;
		

	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private Project project;
	
	@BeforeAll
	public static void beforeAll() throws Exception {
		
		sqsClient = AwsClientFactory.createAmazonSQSClient();
		apiGatewayClient = AwsClientFactory.createAmazonApiGatewayClient();
		 
		StackConfiguration config = StackConfigurationSingleton.singleton();
		String apiName = config.getStack() + config.getStackInstance() + "WebhookTestApi";
		GetApisRequest apiRequest = new GetApisRequest();
		GetApisResult page;
		
		// Lookup the testing api gateway endpoint for the stack
		do {
			page = apiGatewayClient.getApis(apiRequest);
			
			testApi = page.getItems().stream()
				.filter( api -> apiName.equals(api.getName()))
				.findFirst()
				.orElse(null);
			
			if (testApi != null) {
				break;
			}
			
			apiRequest.setNextToken(page.getNextToken());
		} while (page.getNextToken() != null);
		
		if (testApi == null) {
			throw new IllegalStateException("Could not find endpoint for API: " + apiName);
		}
				
		// Make sure the default API endpoint is enabled
		apiGatewayClient.updateApi(
			new UpdateApiRequest()
				.withApiId(testApi.getApiId())
				.withDisableExecuteApiEndpoint(false)
		);
		
		waitForEndpointStatus(true);
		
		// This is the queue that the testing api gateway endpoint forwards the messages to
		apiTestQueueUrl = sqsClient.getQueueUrl(new GetQueueUrlRequest()
			.withQueueName(config.getQueueName("WEBHOOK_TEST"))
		).getQueueUrl();
		
		// This is a dead letter queue that collects failed attempts
		deadLetterQueueUrl = sqsClient.getQueueUrl(new GetQueueUrlRequest()
			.withQueueName(config.getQueueName("WEBHOOK_MESSAGE-dead-letter"))
		).getQueueUrl(); 
	}
	
	@AfterAll
	public static void afterAll() throws Exception {
		// Make sure the default API endpoint is disabled
		apiGatewayClient.updateApi(
			new UpdateApiRequest()
				.withApiId(testApi.getApiId())
				.withDisableExecuteApiEndpoint(true)
		);
		
		waitForEndpointStatus(false);
	}
		
	@BeforeEach
	public void before() throws Exception {
		aclHelper.truncateAll();
		entityManager.truncateAll();
		fileHelper.truncateAll();
		webhookDao.truncateAll();
		
		webhookDao.addAllowedDomainPattern("^.+\\.sagebase\\.org$");
		
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		userInfo = userManager.createOrGetTestUser(adminUserInfo, new NewUser().setUserName(UUID.randomUUID().toString()).setEmail(UUID.randomUUID().toString() + "@foo.org"), true);
		
		project = entityManager.getEntity(adminUserInfo, entityManager
			.createEntity(adminUserInfo, new Project().setName(UUID.randomUUID().toString()), null), Project.class
		);
			
		aclHelper.update(project.getId(), ObjectType.ENTITY, a -> {
			a.getResourceAccess().add(createResourceAccess(userInfo.getId(), ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE, ACCESS_TYPE.CREATE, ACCESS_TYPE.DELETE));
		});
	}
	
	@Test
	public void testWebhookEndpointValidationFailure() throws Exception {
		Webhook webhook = webhookManager.createWebhook(userInfo, new CreateOrUpdateWebhookRequest()
			.setEventTypes(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE, SynapseEventType.DELETE))
			.setIsEnabled(true)
			.setObjectId(project.getId())
			.setObjectType(SynapseObjectType.ENTITY)
			.setInvokeEndpoint(testApi.getApiEndpoint() + "/events/invalid")
		);
		
		assertEquals(WebhookVerificationStatus.PENDING, webhook.getVerificationStatus());
		
		String webhookId = webhook.getId();
		
		// Wait for the verification to fail due to the 404
		webhook = TimeUtils.waitFor(TIMEOUT, 1000, () -> {
			Webhook updatedWebhook = webhookManager.getWebhook(userInfo, webhookId);
			
			return Pair.create(WebhookVerificationStatus.FAILED.equals(updatedWebhook.getVerificationStatus()), updatedWebhook);
		});
		
		assertEquals("The request to the webhook endpoint failed with status 404.", webhook.getVerificationMsg());
		
		// Now update the webhook to a poison endpoint that we know returns a 503
		webhook = webhookManager.updateWebhook(userInfo, webhookId, new CreateOrUpdateWebhookRequest()
			.setEventTypes(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE, SynapseEventType.DELETE))
			.setIsEnabled(true)
			.setObjectId(project.getId())
			.setObjectType(SynapseObjectType.ENTITY)
			.setInvokeEndpoint(testApi.getApiEndpoint() + "/failing")
		);
		
		assertEquals(WebhookVerificationStatus.PENDING, webhook.getVerificationStatus());
		
		// Wait for the message to end up in the DLQ due to the 503
		pollWebhookMessages(deadLetterQueueUrl, webhook.getId(), WebhookVerificationMessage.class, null);
		
		webhook = webhookManager.getWebhook(userInfo, webhookId);
		
		assertEquals(WebhookVerificationStatus.FAILED, webhook.getVerificationStatus());
		assertEquals("The request to the webhook endpoint failed with status 503.", webhook.getVerificationMsg());
		
		// Now update the webhook to a non existing endpoint
		webhook = webhookManager.updateWebhook(userInfo, webhookId, new CreateOrUpdateWebhookRequest()
			.setEventTypes(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE, SynapseEventType.DELETE))
			.setIsEnabled(true)
			.setObjectId(project.getId())
			.setObjectType(SynapseObjectType.ENTITY)
			.setInvokeEndpoint("https://webhook.sagebase.org/invalid")
		);
		
		assertEquals(WebhookVerificationStatus.PENDING, webhook.getVerificationStatus());
		
		// Wait for the message to end up in the DLQ due a connection timeout
		pollWebhookMessages(deadLetterQueueUrl, webhook.getId(), WebhookVerificationMessage.class, null);
		
		webhook = webhookManager.getWebhook(userInfo, webhookId);
		
		assertEquals(WebhookVerificationStatus.FAILED, webhook.getVerificationStatus());
		assertTrue(
			"The request to the webhook endpoint failed (Reason: connection timeout).".equals(webhook.getVerificationMsg())
			|| 
			"The request to the webhook endpoint failed (Reason: request timeout).".equals(webhook.getVerificationMsg()),
			"Unexpected verification message: " + webhook.getVerificationMsg()
		);
	}	
	
	@Test
	public void testWebhook() throws Exception {
		
		// The entity create message goes through a topic, that might be slower than the direct sqs message of the webhook verification
		Thread.sleep(3000);
		
		Webhook webhook = webhookManager.createWebhook(userInfo, new CreateOrUpdateWebhookRequest()
			.setEventTypes(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE, SynapseEventType.DELETE))
			.setIsEnabled(true)
			.setObjectId(project.getId())
			.setObjectType(SynapseObjectType.ENTITY)
			.setInvokeEndpoint(testApi.getApiEndpoint() + "/events")
		);
		
		// Try to validate before the message is sent
		String result = assertThrows(IllegalArgumentException.class, () -> {
			webhookManager.verifyWebhook(userInfo, webhook.getId(), new VerifyWebhookRequest().setVerificationCode("invalidCode"));
		}).getMessage();
		
		assertEquals("Cannot verify the webhook at this time.", result);
		
		// Wait for the code to be sent
		TimeUtils.waitFor(TIMEOUT, 1000, () -> {
			WebhookVerificationStatus status = webhookManager.getWebhook(userInfo, webhook.getId()).getVerificationStatus();
			
			return Pair.create(WebhookVerificationStatus.CODE_SENT.equals(status), null);
		});
		
		// Try to verify with an invalid code
		assertFalse(webhookManager.verifyWebhook(userInfo, webhook.getId(), new VerifyWebhookRequest().setVerificationCode("invalidCode")).getIsValid());
		
		// Extracts the code from the test queue
		WebhookVerificationMessage verificationMessage = pollWebhookMessages(apiTestQueueUrl, webhook.getId(), WebhookVerificationMessage.class, null).iterator().next();
		
		// Verify the webhook
		assertTrue(webhookManager.verifyWebhook(userInfo, webhook.getId(), new VerifyWebhookRequest().setVerificationCode(verificationMessage.getVerificationCode())).getIsValid());
		
		// Updated the entity as the admin
		project.setName(UUID.randomUUID().toString());
		entityManager.updateEntity(adminUserInfo, project, false, null);
		
		// Checks for the UPDATE project message
		pollWebhookMessages(apiTestQueueUrl, webhook.getId(), WebhookSynapseEventMessage.class, entityAndEventTypeFilter(project, SynapseEventType.UPDATE));
		
		// Create a folder in the project
		Folder folder = entityManager.getEntity(adminUserInfo, entityManager
			.createEntity(adminUserInfo, new Folder().setName("folder").setParentId(project.getId()), null), Folder.class
		);
		
		// Checks for the CREATE folder message
		pollWebhookMessages(apiTestQueueUrl, webhook.getId(), WebhookSynapseEventMessage.class, entityAndEventTypeFilter(folder, SynapseEventType.CREATE));
		
		// Create a file in the folder
		S3FileHandle fileHandle = fileHelper.create(f -> {});
		
		FileEntity file = entityManager.getEntity(adminUserInfo, entityManager
			.createEntity(adminUserInfo, new FileEntity()
				.setName("file")
				.setParentId(folder.getId())
				.setDataFileHandleId(fileHandle.getId()), null
			), FileEntity.class
		);
		
		// Checks for the CREATE file message
		pollWebhookMessages(apiTestQueueUrl, webhook.getId(), WebhookSynapseEventMessage.class, entityAndEventTypeFilter(file, SynapseEventType.CREATE));
		
		// Moves the folder to the trashcan
		trashManager.moveToTrash(adminUserInfo, folder.getId(), false);
		
		// Checks that the DELETE for both the file and folder messages are received
		pollWebhookMessages(apiTestQueueUrl, webhook.getId(), WebhookSynapseEventMessage.class, 
			entityAndEventTypeFilter(folder, SynapseEventType.DELETE).and(
			entityAndEventTypeFilter(file, SynapseEventType.DELETE))
		);
		
		// Restore the folder
		trashManager.restoreFromTrash(adminUserInfo, folder.getId(), null);
		
		// Checks that the CREATE for both the file and folder messages are received
		pollWebhookMessages(apiTestQueueUrl, webhook.getId(), WebhookSynapseEventMessage.class, 
			entityAndEventTypeFilter(folder, SynapseEventType.CREATE).and(
			entityAndEventTypeFilter(file, SynapseEventType.CREATE))
		);
		
	}
	
	@Test
	public void testWebhookDeliveryFailure() throws Exception {
		
		// The entity create message goes through a topic, that might be slower than the direct sqs message of the webhook verification
		Thread.sleep(3000);
		
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
				.setEventTypes(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE, SynapseEventType.DELETE))
				.setIsEnabled(true)
				.setObjectId(project.getId())
				.setObjectType(SynapseObjectType.ENTITY)
				.setInvokeEndpoint(testApi.getApiEndpoint() + "/events");
		
		Webhook webhook = webhookManager.createWebhook(userInfo, request);
		
		// Wait for the code to be sent
		TimeUtils.waitFor(TIMEOUT, 1000, () -> {
			WebhookVerificationStatus status = webhookManager.getWebhook(userInfo, webhook.getId()).getVerificationStatus();
			
			return Pair.create(WebhookVerificationStatus.CODE_SENT.equals(status), null);
		});
		
		// Extracts the code from the test queue
		WebhookVerificationMessage verificationMessage = pollWebhookMessages(apiTestQueueUrl, webhook.getId(), WebhookVerificationMessage.class, null).iterator().next();
		
		// Verify the webhook
		assertTrue(webhookManager.verifyWebhook(userInfo, webhook.getId(), new VerifyWebhookRequest().setVerificationCode(verificationMessage.getVerificationCode())).getIsValid());
				
		// Create a folder in the project
		Folder folder = entityManager.getEntity(adminUserInfo, entityManager
			.createEntity(adminUserInfo, new Folder().setName("folder").setParentId(project.getId()), null), Folder.class
		);
		
		// Checks for the CREATE folder message
		pollWebhookMessages(apiTestQueueUrl, webhook.getId(), WebhookSynapseEventMessage.class, entityAndEventTypeFilter(folder, SynapseEventType.CREATE));
		
		// Now emulates a failure in the webhook service by overriding its endpoint
		webhookDao.updateWebhook(webhook.getId(), request.setInvokeEndpoint(testApi.getApiEndpoint() + "/failing"));
		
		// Updates the folder in the project
		entityManager.updateEntity(adminUserInfo, folder.setName("folder updated"), false, null);
		
		// Checks for the UPDATE folder message in the DLQ
		pollWebhookMessages(deadLetterQueueUrl, webhook.getId(), WebhookSynapseEventMessage.class, entityAndEventTypeFilter(folder, SynapseEventType.UPDATE));
	}

	private static void waitForEndpointStatus(boolean enabled) throws Exception {
		
		HttpClient httpClient = HttpClient.newBuilder()
				.followRedirects(Redirect.NEVER)
				.connectTimeout(Duration.ofSeconds(2))
				.build();
		
		TimeUtils.waitFor(TIMEOUT, 1000, () -> {
			HttpRequest request = HttpRequest.newBuilder(URI.create(testApi.getApiEndpoint() + "/failing"))
				.POST(BodyPublishers.ofString("{ \"message\": \"ping\" }"))
				.build();
			
			HttpResponse<Void> response = httpClient.send(request, BodyHandlers.discarding());
			
			// The /failing API returns a 503 from the lambda, if the endpoint is disabled we receive a 404
			return Pair.create((enabled ? 503 : 404) == response.statusCode(), null);
		});
	}
	
	private static Predicate<List<WebhookSynapseEventMessage>> entityAndEventTypeFilter(Entity entity, SynapseEventType eventType) {
		return messages -> messages.stream()
			.filter(message -> message.getObjectId().equals(entity.getId()) && message.getEventType().equals(eventType))
			.findFirst()
			.isPresent();
	}
			
	private static <T extends WebhookMessage> List<T> pollWebhookMessages(String queueUrl, String webhookId, Class<T> messageClass, Predicate<List<T>> filter) throws Exception  {
		List<T> polledMessages = new ArrayList<>();
		WebhookMessageType messageType = WebhookMessageType.forClass(messageClass);
		
		TimeUtils.waitFor(TIMEOUT, 1000, () -> {
			ReceiveMessageRequest request = new ReceiveMessageRequest(queueUrl)
				.withMaxNumberOfMessages(1)
				// With long polling SQS queries all the servers and avoid empty receive messages on almost empty queues
				.withWaitTimeSeconds(10)
				.withMessageAttributeNames(MSG_ATTR_WEBHOOK_MESSAGE_TYPE, MSG_ATTR_WEBHOOK_ID);
			
			List<Message> messages = sqsClient.receiveMessage(request).getMessages();
			
			if (messages.isEmpty()) {
				return Pair.create(false, null);
			}
			
			// Only one message by default
			Message sqsMessage = messages.iterator().next();
			
			// The test webhook API gateway is configured to forward the special request headers as message attributes
			// X-Syn-Webhook-Id -> WebhookId
			// X-Syn-Webhook-Message-Type -> WebhookMessageType
			Map<String, MessageAttributeValue> sqsMessageAttributes = sqsMessage.getMessageAttributes();
			
			// MessageType does not match
			if (!messageType.name().equals(sqsMessageAttributes.get(MSG_ATTR_WEBHOOK_MESSAGE_TYPE).getStringValue())) {
				return Pair.create(false, null);
			}
			
			// Webhook id does not match
			if (!webhookId.equals(sqsMessageAttributes.get(MSG_ATTR_WEBHOOK_ID).getStringValue())) {
				return Pair.create(false, null);
			}
			
			T message;
			
			try {
				 message = EntityFactory.createEntityFromJSONString(sqsMessage.getBody(), messageClass);
			} catch (JSONObjectAdapterException e) {
				e.printStackTrace();
				return Pair.create(false, null);
			}
			
			polledMessages.add(message);
			
			sqsClient.deleteMessage(queueUrl, sqsMessage.getReceiptHandle());
			
			return Pair.create(filter == null || filter.test(polledMessages), null);
			
		});
		
		return polledMessages;
	}

}
