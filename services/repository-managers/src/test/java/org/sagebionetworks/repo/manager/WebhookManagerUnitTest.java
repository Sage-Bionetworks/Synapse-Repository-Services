package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.dao.webhook.WebhookDao;
import org.sagebionetworks.repo.model.dbo.dao.webhook.WebhookVerificationDao;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookObjectType;

@ExtendWith(MockitoExtension.class)
public class WebhookManagerUnitTest {

	@Mock
	WebhookDao mockWebhookDao;
	
	@Mock
	WebhookVerificationDao mockWebhookVerificationDao;
	
	@Mock
	AccessControlListDAO mockAclDao;
	
	@Mock
	UserManager mockUserManager;
	
	@InjectMocks
	WebhookManagerImpl webhookManager;
	
	
	UserInfo userInfo;
	UserInfo adminUserInfo;
	UserInfo anonymousUserInfo;
	UserInfo unauthorizedUserInfo;
	long userId = 123L;
	long adminUserId = 456L;
	long unauthorizedUserId = 789L;
	String webhookId = "someWebhookId";
	Webhook webhook;
	String objectId = "syn2024";
	WebhookObjectType webhookObjectType = WebhookObjectType.entity;
	ObjectType objectType = ObjectType.ENTITY;
	String validApiGatewayEndpoint = "https://abcd1234.execute-api.us-east-1.amazonaws.com/prod";
	String invalidInvokeEndpoint = "https://invalidEndpoint.com";
	
	
	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, userId);
		adminUserInfo = new UserInfo(true, adminUserId);
		anonymousUserInfo = new UserInfo(true, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		unauthorizedUserInfo = new UserInfo(false, unauthorizedUserId);
		
		webhook = new Webhook()
				.setWebhookId(webhookId)
				.setObjectId(objectId)
				.setObjectType(webhookObjectType)
				.setInvokeEndpoint(validApiGatewayEndpoint)
				.setIsAuthenticationEnabled(true)
				.setIsWebhookEnabled(true)
				.setCreatedBy(String.valueOf(userId));
	}
	
	
	@Test
	public void testCreateWebhook() {
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
				.setObjectId(objectId)
				.setObjectType(webhookObjectType)
				.setInvokeEndpoint(validApiGatewayEndpoint)
				.setIsWebhookEnabled(true)
				.setIsAuthenticationEnabled(true);
		
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockWebhookDao.createWebhook(any(), any())).thenReturn(webhook);
		
		// Call under test
		Webhook result = webhookManager.createWebhook(userInfo, request);
		
		assertEquals(webhook, result);
		
		verify(mockAclDao).canAccess(userInfo, objectId, objectType, ACCESS_TYPE.READ);
		verify(mockWebhookVerificationDao).createWebhookVerification(userId, webhookId);
	}
	
	@Test
	public void testCreateWebhookWithNullUserInfo() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.createWebhook(null, new CreateOrUpdateWebhookRequest());
		}).getMessage();
		
		assertEquals("userInfo is required.", errorMessage);
	}
	
	@Test
	public void testCreateWebhookWithNullRequest() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.createWebhook(userInfo, null);
		}).getMessage();
		
		assertEquals("createOrUpdateWebhookRequest is required.", errorMessage);
	}
	
	@Test
	public void testCreateWebhookWithAnonymousUser() {
		String errorMessage = assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			webhookManager.createWebhook(anonymousUserInfo, new CreateOrUpdateWebhookRequest());
		}).getMessage();
		
		assertEquals("Must login to perform this action", errorMessage);
		
		verify(mockWebhookDao, never()).createWebhook(any(), any());
	}
	
	@Test
	public void testCreateWebhookWithoutReadPermissionOnObject() {
		String accessDeniedMessage = String.format("You do not have %s permission for %s : %s", ACCESS_TYPE.READ, objectType, webhookId);
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
				.setObjectId(objectId)
				.setObjectType(webhookObjectType);
		
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any()))
				.thenReturn(AuthorizationStatus.accessDenied(accessDeniedMessage));
		
		String errorMessage = assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			webhookManager.createWebhook(userInfo, request);
		}).getMessage();
		
		assertEquals(accessDeniedMessage, errorMessage);
		
		verify(mockWebhookDao, never()).createWebhook(any(), any());
	}
	
	@Test
	public void testCreateWebhookWithInvalidInvokeEndpoint() {
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
				.setObjectId(objectId)
				.setObjectType(webhookObjectType)
				.setInvokeEndpoint(invalidInvokeEndpoint);
		
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
	
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.createWebhook(userInfo, request);
		}).getMessage();
		
		assertEquals(String.format(WebhookManagerImpl.INVALID_INVOKE_ENDPOINT_MESSAGE, invalidInvokeEndpoint), errorMessage);
		
		verify(mockWebhookDao, never()).createWebhook(any(), any());
	}
	
	@Test
	public void testCreateWebhookWithNullIsWebhookEnabled() {
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
				.setObjectId(objectId)
				.setObjectType(webhookObjectType)
				.setInvokeEndpoint(validApiGatewayEndpoint)
				.setIsWebhookEnabled(null)
				.setIsAuthenticationEnabled(true);
		
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockWebhookDao.createWebhook(any(), any())).thenReturn(webhook);
		
		// Call under test
		Webhook result = webhookManager.createWebhook(userInfo, request);
		
		assertEquals(webhook, result);
		
		verify(mockAclDao).canAccess(userInfo, objectId, objectType, ACCESS_TYPE.READ);
		verify(mockWebhookVerificationDao).createWebhookVerification(userId, webhookId);
	}
	
	@Test
	public void testCreateWebhookWithFalseIsWebhookEnabled() {
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
				.setObjectId(objectId)
				.setObjectType(webhookObjectType)
				.setInvokeEndpoint(validApiGatewayEndpoint)
				.setIsWebhookEnabled(false)
				.setIsAuthenticationEnabled(true);
		
		Webhook disabledWebhook = new Webhook()
				.setWebhookId("someDisabledWebhookId")
				.setObjectId(objectId)
				.setObjectType(webhookObjectType)
				.setInvokeEndpoint(validApiGatewayEndpoint)
				.setIsWebhookEnabled(false)
				.setCreatedBy(String.valueOf(userId));
		
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockWebhookDao.createWebhook(any(), any())).thenReturn(disabledWebhook);
		
		// Call under test
		Webhook result = webhookManager.createWebhook(userInfo, request);
		
		assertEquals(disabledWebhook, result);
		
		verify(mockAclDao).canAccess(userInfo, objectId, objectType, ACCESS_TYPE.READ);
		verify(mockWebhookVerificationDao).createWebhookVerification(userId, "someDisabledWebhookId");
	}
	
	@Test
	public void testGetWebhook() {
		when(mockWebhookDao.getWebhook(any())).thenReturn(webhook);
		when(mockWebhookDao.getWebhookOwnerForUpdate(any())).thenReturn(Optional.of(String.valueOf(userId)));
		
		// Call under test
		Webhook result = webhookManager.getWebhook(userInfo, webhookId);
		
		assertEquals(webhook, result);
		
		verify(mockWebhookDao).getWebhookOwnerForUpdate(webhookId);
	}
	
	@Test
	public void testGetWebhookWithAdmin() {
		when(mockWebhookDao.getWebhook(any())).thenReturn(webhook);
		
		// Call under test
		Webhook result = webhookManager.getWebhook(adminUserInfo, webhookId);
				
		assertEquals(webhook, result);
				
		verify(mockWebhookDao, never()).getWebhookOwnerForUpdate(webhookId);
	}
	
	@Test
	public void testGetWebhookWithUnauthorized() {
		when(mockWebhookDao.getWebhookOwnerForUpdate(any())).thenReturn(Optional.of(String.valueOf(userId)));
		
		String errorMessage = assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			webhookManager.getWebhook(unauthorizedUserInfo, webhookId);
		}).getMessage();
		
		assertEquals(WebhookManagerImpl.UNAUTHORIZED_ACCESS_MESSAGE, errorMessage);
	}
	
	@Test
	public void testGetWebhookWithNullUserInfo() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.getWebhook(null, webhookId);
		}).getMessage();
		
		assertEquals("userInfo is required.", errorMessage);
	}
	
	@Test
	public void testGetWebhookWithNullWebhookId() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.getWebhook(userInfo, null);
		}).getMessage();
		
		assertEquals("webhookId is required.", errorMessage);
	}
	
	@Test
	public void testUpdateWebhook() {
		
	}
}
