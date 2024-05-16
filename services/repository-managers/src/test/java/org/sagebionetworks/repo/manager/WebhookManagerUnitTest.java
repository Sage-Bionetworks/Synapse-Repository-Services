package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
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
	String anotherObjectId = "syn2009";
	WebhookObjectType webhookObjectType = WebhookObjectType.ENTITY;
	ObjectType objectType = ObjectType.ENTITY;
	String validApiGatewayEndpoint = "https://abcd1234.execute-api.us-east-1.amazonaws.com/prod";
	String anotherValidApiGatewayEndpoint = "https://vxyz5678.execute-api.us-west-2.amazonaws.com/prod";
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
		String accessDeniedMessage = String.format("You do not have %s permission for %s : %s", ACCESS_TYPE.READ, objectType, objectId);
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
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
				.setObjectId(anotherObjectId)
				.setObjectType(webhookObjectType)
				.setInvokeEndpoint(anotherValidApiGatewayEndpoint)
				.setIsAuthenticationEnabled(true);
		
		Webhook expected = new Webhook()
				.setWebhookId(webhookId)
				.setObjectId(anotherObjectId)
				.setObjectType(webhookObjectType)
				.setInvokeEndpoint(anotherValidApiGatewayEndpoint)
				.setIsWebhookEnabled(true)
				.setCreatedBy(String.valueOf(userId));

		when(mockWebhookDao.getWebhookOwnerForUpdate(any())).thenReturn(Optional.of(String.valueOf(userId)));
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockWebhookDao.updateWebhook(any(), any(), any())).thenReturn(expected);
		
		// Call under test
		Webhook response = webhookManager.updateWebhook(userInfo, webhookId, request);
		
		assertEquals(expected, response);
		
		verify(mockWebhookDao).getWebhookOwnerForUpdate(webhookId);
		verify(mockAclDao).canAccess(userInfo, anotherObjectId, objectType, ACCESS_TYPE.READ);
		verify(mockWebhookVerificationDao).createWebhookVerification(userId, webhookId);
		verify(mockWebhookDao).updateWebhook(userId, webhookId, request);
	}
	
	@Test
	public void testUpdateWebhookWithNullUserInfo() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.updateWebhook(null, webhookId, new CreateOrUpdateWebhookRequest());
		}).getMessage();
		
		assertEquals("userInfo is required.", errorMessage);
	}
	
	@Test
	public void testUpdateWebhookWithNullWebhookId() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.updateWebhook(userInfo, null, new CreateOrUpdateWebhookRequest());
		}).getMessage();
		
		assertEquals("webhookId is required.", errorMessage);
	}
	
	@Test
	public void testUpdateWebhookWithNullRequest() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.updateWebhook(userInfo, webhookId, null);
		}).getMessage();
		
		assertEquals("createOrUpdateWebhookRequest is required.", errorMessage);
	}
	
	@Test
	public void testUpdateWebhookWithAdmin() {
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
				.setObjectId(anotherObjectId)
				.setObjectType(webhookObjectType)
				.setInvokeEndpoint(anotherValidApiGatewayEndpoint)
				.setIsAuthenticationEnabled(true);
		
		Webhook expected = new Webhook()
				.setWebhookId(webhookId)
				.setObjectId(anotherObjectId)
				.setObjectType(webhookObjectType)
				.setInvokeEndpoint(anotherValidApiGatewayEndpoint)
				.setIsWebhookEnabled(true)
				.setCreatedBy(String.valueOf(userId));

		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockWebhookDao.updateWebhook(any(), any(), any())).thenReturn(expected);
		
		// Call under test
		Webhook response = webhookManager.updateWebhook(adminUserInfo, webhookId, request);
		
		assertEquals(expected, response);
		
		verify(mockAclDao).canAccess(adminUserInfo, anotherObjectId, objectType, ACCESS_TYPE.READ);
		verify(mockWebhookVerificationDao).createWebhookVerification(userId, webhookId);
		verify(mockWebhookDao, never()).getWebhookOwnerForUpdate(any());
	}
	
	@Test
	public void testUpdateWebhookWithOnlyObjectId() {
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
				.setObjectId(anotherObjectId);

		when(mockWebhookDao.getWebhookOwnerForUpdate(any())).thenReturn(Optional.of(String.valueOf(userId)));

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.updateWebhook(userInfo, webhookId, request);
		}).getMessage();
		
		assertEquals(WebhookManagerImpl.MISSING_OBJECT_UPDATE_PARAMS, errorMessage);
		
		verify(mockWebhookDao).getWebhookOwnerForUpdate(webhookId);
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), any(), any(), any());
		verify(mockWebhookVerificationDao, never()).createWebhookVerification(any(), any());
		verify(mockWebhookDao, never()).updateWebhook(any(), any(), any());
	}
	
	@Test
	public void testUpdateWebhookWithOnlyObjectType() {
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
				.setObjectType(webhookObjectType);

		when(mockWebhookDao.getWebhookOwnerForUpdate(any())).thenReturn(Optional.of(String.valueOf(userId)));

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.updateWebhook(userInfo, webhookId, request);
		}).getMessage();
		
		assertEquals(WebhookManagerImpl.MISSING_OBJECT_UPDATE_PARAMS, errorMessage);
		
		verify(mockWebhookDao).getWebhookOwnerForUpdate(webhookId);
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), any(), any(), any());
		verify(mockWebhookVerificationDao, never()).createWebhookVerification(any(), any());
		verify(mockWebhookDao, never()).updateWebhook(any(), any(), any());
	}
	
	@Test
	public void testUpdateWebhookWithoutInvokeEndpoint() {
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
				.setObjectId(anotherObjectId)
				.setObjectType(webhookObjectType);
		
		Webhook expected = new Webhook()
				.setWebhookId(webhookId)
				.setObjectId(anotherObjectId)
				.setObjectType(webhookObjectType)
				.setInvokeEndpoint(validApiGatewayEndpoint)
				.setIsWebhookEnabled(true)
				.setCreatedBy(String.valueOf(userId));

		when(mockWebhookDao.getWebhookOwnerForUpdate(any())).thenReturn(Optional.of(String.valueOf(userId)));
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockWebhookDao.updateWebhook(any(), any(), any())).thenReturn(expected);
		
		// Call under test
		Webhook response = webhookManager.updateWebhook(userInfo, webhookId, request);
		
		assertEquals(expected, response);
		
		verify(mockWebhookDao).getWebhookOwnerForUpdate(webhookId);
		verify(mockAclDao).canAccess(userInfo, anotherObjectId, objectType, ACCESS_TYPE.READ);
		verify(mockWebhookVerificationDao, never()).createWebhookVerification(any(), any());
		verify(mockWebhookDao).updateWebhook(userId, webhookId, request);
	}
	
	@Test
	public void testUpdateWebhookWithInvalidInvokeEndpoint() {
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
				.setInvokeEndpoint(invalidInvokeEndpoint);
		
		when(mockWebhookDao.getWebhookOwnerForUpdate(any())).thenReturn(Optional.of(String.valueOf(userId)));
	
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.updateWebhook(userInfo, webhookId, request);
		}).getMessage();
		
		assertEquals(String.format(WebhookManagerImpl.INVALID_INVOKE_ENDPOINT_MESSAGE, invalidInvokeEndpoint), errorMessage);
		
		verify(mockWebhookDao).getWebhookOwnerForUpdate(webhookId);
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), any(), any(), any());
		verify(mockWebhookVerificationDao, never()).createWebhookVerification(any(), any());
		verify(mockWebhookDao, never()).updateWebhook(any(), any(), any());
	}
	
	@Test
	public void testDeleteWebhook() {
		when(mockWebhookDao.getWebhookOwnerForUpdate(any())).thenReturn(Optional.of(String.valueOf(userId)));
		
		// Call under test
		webhookManager.deleteWebhook(userInfo, webhookId);
		
		verify(mockWebhookDao).getWebhookOwnerForUpdate(webhookId);
		verify(mockWebhookDao).deleteWebhook(webhookId);
	}
	
	@Test
	public void testDeleteWebhookWithNullUserInfo() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.deleteWebhook(null, webhookId);
		}).getMessage();
		
		assertEquals("userInfo is required.", errorMessage);
	}
	
	@Test
	public void testDeleteWebhookWithNullWebhookId() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.deleteWebhook(userInfo, null);
		}).getMessage();
		
		assertEquals("webhookId is required.", errorMessage);
	}
	
	@Test
	public void testDeleteWebhookWithAdmin() {		
		// Call under test
		webhookManager.deleteWebhook(adminUserInfo, webhookId);
		
		verify(mockWebhookDao, never()).getWebhookOwnerForUpdate(any());
		verify(mockWebhookDao).deleteWebhook(webhookId);
	}
	
	@Test
	public void testValidateWebhook() {
		String validCode = "someCode";
		WebhookVerification verification = new WebhookVerification()
				.setWebhookId(webhookId)
				.setVerificationCode(validCode)
				.setExpiresOn(new GregorianCalendar(3000, Calendar.JANUARY, 1).getTime());
		
		VerifyWebhookRequest request = new VerifyWebhookRequest()
				.setVerificationCode(validCode);
		
		when(mockWebhookVerificationDao.getWebhookVerification(any())).thenReturn(verification);
		
		// Call under test
		VerifyWebhookResponse response = webhookManager.verifyWebhook(userInfo, webhookId, request);
		
		assertTrue(response.getIsValid());
		assertNull(response.getInvalidReason());
		
		verify(mockWebhookDao).setWebhookVerificationStatus(webhookId, true);
	}
	
	@Test
	public void testValidateWebhookWithInvalidCode() {
		String validCode = "someCode";
		WebhookVerification verification = new WebhookVerification()
				.setWebhookId(webhookId)
				.setVerificationCode(validCode)
				.setExpiresOn(new GregorianCalendar(3000, Calendar.JANUARY, 1).getTime());
		
		VerifyWebhookRequest request = new VerifyWebhookRequest()
				.setVerificationCode("thisIsNotTheValidCode");
		
		when(mockWebhookVerificationDao.getWebhookVerification(any())).thenReturn(verification);
		
		// Call under test
		VerifyWebhookResponse response = webhookManager.verifyWebhook(userInfo, webhookId, request);
		
		assertFalse(response.getIsValid());
		assertEquals(WebhookManagerImpl.INVALID_VERIFICATION_CODE_MESSAGE, response.getInvalidReason());
		
		verify(mockWebhookDao, never()).setWebhookVerificationStatus(webhookId, true);
	}
	
	@Test
	public void testValidateWebhookWithExpiredCode() {
		String validCode = "someCode";
		WebhookVerification verification = new WebhookVerification()
				.setWebhookId(webhookId)
				.setVerificationCode(validCode)
				.setExpiresOn(new GregorianCalendar(1000, Calendar.JANUARY, 1).getTime());
		
		VerifyWebhookRequest request = new VerifyWebhookRequest()
				.setVerificationCode(validCode);
		
		when(mockWebhookVerificationDao.getWebhookVerification(any())).thenReturn(verification);
		
		// Call under test
		VerifyWebhookResponse response = webhookManager.verifyWebhook(userInfo, webhookId, request);
		
		assertFalse(response.getIsValid());
		assertEquals(WebhookManagerImpl.EXPIRED_VERIFICATION_CODE_MESSAGE, response.getInvalidReason());
		
		verify(mockWebhookDao, never()).setWebhookVerificationStatus(webhookId, true);
	}
	
	@Test
	public void testValidateWebhookWithInvalidAndExpiredCode() {
		String validCode = "someCode";
		WebhookVerification verification = new WebhookVerification()
				.setWebhookId(webhookId)
				.setVerificationCode(validCode)
				.setExpiresOn(new GregorianCalendar(1000, Calendar.JANUARY, 1).getTime());
		
		VerifyWebhookRequest request = new VerifyWebhookRequest()
				.setVerificationCode("thisIsNotTheValidCode");
		
		when(mockWebhookVerificationDao.getWebhookVerification(any())).thenReturn(verification);
		
		// Call under test
		VerifyWebhookResponse response = webhookManager.verifyWebhook(userInfo, webhookId, request);
		
		assertFalse(response.getIsValid());
		assertEquals(WebhookManagerImpl.INVALID_VERIFICATION_CODE_MESSAGE, response.getInvalidReason());
		
		verify(mockWebhookDao, never()).setWebhookVerificationStatus(webhookId, true);
	}
	
	@Test
	public void testValidateWebhookWithNullUserInfo() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.verifyWebhook(null, webhookId, 
					new VerifyWebhookRequest().setVerificationCode("code"));
		}).getMessage();
		
		assertEquals("userInfo is required.", errorMessage);
	}
	
	@Test
	public void testValidateWebhookWithNullWebhookId() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.verifyWebhook(userInfo, null, 
					new VerifyWebhookRequest().setVerificationCode("code"));
		}).getMessage();
		
		assertEquals("webhookId is required.", errorMessage);
	}
	
	@Test
	public void testValidateWebhookWithNullRequest() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.verifyWebhook(userInfo, webhookId, null);
		}).getMessage();
		
		assertEquals("verifyWebhookRequest is required.", errorMessage);
	}
	
	@Test
	public void testValidateWebhookWithNullVerificationCode() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.verifyWebhook(userInfo, webhookId, 
					new VerifyWebhookRequest().setVerificationCode(null));
		}).getMessage();
		
		assertEquals("verifyWebhookRequest.verificationCode is required.", errorMessage);
	}
	
	@Test
	public void testListUserWebhooks() {
		ListUserWebhooksRequest request = new ListUserWebhooksRequest()
				.setUserId(String.valueOf(userId));
		
		List<Webhook> page = new ArrayList<Webhook>();
		for (int i = 0; i < NextPageToken.MAX_LIMIT + 1; i++) {
			page.add(new Webhook().setCreatedBy(String.valueOf(userId)));
		}
		ListUserWebhooksResponse expectedResponse = new ListUserWebhooksResponse()
				.setPage(page)
				.setNextPageToken("50a50");
		long expectedLimit = NextPageToken.DEFAULT_LIMIT + 1;
		long expectedOffset = NextPageToken.DEFAULT_OFFSET;
		
		when(mockWebhookDao.listUserWebhooks(any(), anyLong(), anyLong())).thenReturn(page);
		
		// Call under test
		ListUserWebhooksResponse response = webhookManager.listUserWebhooks(userInfo, request);
		
		assertEquals(expectedResponse, response);
		
		verify(mockWebhookDao).listUserWebhooks(userId, expectedLimit, expectedOffset);
	}
	
	@Test
	public void testListUserWebhooksWithNullUserInfo() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.listUserWebhooks(null, new ListUserWebhooksRequest());
		}).getMessage();
		
		assertEquals("userInfo is required.", errorMessage);
	}
	
	@Test
	public void testListUserWebhooksWithNullRequest() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.listUserWebhooks(userInfo, null);
		}).getMessage();
		
		assertEquals("listUserWebhooksRequest is required.", errorMessage);
	}
	
	@Test
	public void testListUserWebhooksWithLessThanOnePage() {
		ListUserWebhooksRequest request = new ListUserWebhooksRequest()
				.setUserId(String.valueOf(userId));
		
		List<Webhook> page = new ArrayList<Webhook>();
		for (int i = 0; i < Math.max(Math.ceil(NextPageToken.MAX_LIMIT/3), 3); i++) {
			page.add(new Webhook().setCreatedBy(String.valueOf(userId)));
		}
		ListUserWebhooksResponse expectedResponse = new ListUserWebhooksResponse()
				.setPage(page)
				.setNextPageToken(null);
		long expectedLimit = NextPageToken.DEFAULT_LIMIT + 1;
		long expectedOffset = NextPageToken.DEFAULT_OFFSET;
		
		when(mockWebhookDao.listUserWebhooks(any(), anyLong(), anyLong())).thenReturn(page);
		
		// Call under test
		ListUserWebhooksResponse response = webhookManager.listUserWebhooks(userInfo, request);
		
		assertEquals(expectedResponse, response);
		
		verify(mockWebhookDao).listUserWebhooks(userId, expectedLimit, expectedOffset);
	}
	
	@Test
	public void testListSendableWebhooksForObjectId() {
		List<Webhook> webhooks = createDefaultWebhooksForUsers(List.of(userInfo), 5);

        when(mockWebhookDao.listVerifiedAndEnabledWebhooksForObjectId(objectId)).thenReturn(webhooks);
        when(mockUserManager.getUserInfo(anyLong())).thenReturn(userInfo);
        
        // Call under test
        List<Webhook> result = webhookManager.listSendableWebhooksForObjectId(objectId);
        
        assertEquals(webhooks, result);
        
        verify(mockWebhookDao).listVerifiedAndEnabledWebhooksForObjectId(objectId);
        verify(mockUserManager, times(5)).getUserInfo(userId);
	}
	
	@Test 
	public void testListSendableWebhooksForObjectIdWithZeroWebhooks() {
		List<Webhook> webhooks = new ArrayList<>();
		
		when(mockWebhookDao.listVerifiedAndEnabledWebhooksForObjectId(objectId)).thenReturn(webhooks);
		 
		// Call under test
	    List<Webhook> result = webhookManager.listSendableWebhooksForObjectId(objectId);
	        
	    assertEquals(webhooks, result);
	        
	    verify(mockWebhookDao).listVerifiedAndEnabledWebhooksForObjectId(objectId);
	    verify(mockUserManager, never()).getUserInfo(any());
	}

	@Test
	public void testListSendableWebhooksForObjectIdWithManyUsers() {
		List<Webhook> webhooks = createDefaultWebhooksForUsers(createUsers(5), 1);

        when(mockWebhookDao.listVerifiedAndEnabledWebhooksForObjectId(objectId)).thenReturn(webhooks);
        for (Webhook webhook : webhooks) {
        	Long currUserId = Long.parseLong(webhook.getUserId());
        	when(mockUserManager.getUserInfo(currUserId)).thenReturn(new UserInfo(false, currUserId));
        }
        
        // Call under test
        List<Webhook> result = webhookManager.listSendableWebhooksForObjectId(objectId);
        
        assertEquals(webhooks, result);
        
        verify(mockWebhookDao).listVerifiedAndEnabledWebhooksForObjectId(objectId);
        for (Webhook webhook : webhooks) {
        	verify(mockUserManager).getUserInfo(Long.parseLong(webhook.getUserId()));
        }
	}
	
	@Test
	public void testListSendableWebhooksForObjectIdWithSomeSendable() {
		List<UserInfo> users = createUsers(5);
		List<Webhook> webhooks = Stream.concat(
			    createDefaultWebhooksForUsers(users, 1).stream(),
			    createWebhooksForUsers(users, 1, anotherObjectId, objectType, false).stream()
		).collect(Collectors.toList());

        when(mockWebhookDao.listVerifiedAndEnabledWebhooksForObjectId(objectId)).thenReturn(webhooks);
        
        // Call under test
        List<Webhook> result = webhookManager.listSendableWebhooksForObjectId(objectId);
        
        assertEquals(5, result.size());
        
        verify(mockWebhookDao).listVerifiedAndEnabledWebhooksForObjectId(objectId);
        for (Webhook webhook : webhooks) {
        	verify(mockUserManager, times(2)).getUserInfo(Long.parseLong(webhook.getUserId()));
        }
	}
	
	@Test
	public void testListSendableWebhooksForObjectIdWithZeroSendable() {
		List<UserInfo> users = createUsers(5);
		List<Webhook> webhooks = createWebhooksForUsers(users, 1, anotherObjectId, objectType, false);

        when(mockWebhookDao.listVerifiedAndEnabledWebhooksForObjectId(objectId)).thenReturn(webhooks);
        
        // Call under test
        List<Webhook> result = webhookManager.listSendableWebhooksForObjectId(objectId);
        
        assertEquals(0, result.size());
        
        verify(mockWebhookDao).listVerifiedAndEnabledWebhooksForObjectId(objectId);
        for (Webhook webhook : webhooks) {
        	verify(mockUserManager).getUserInfo(Long.parseLong(webhook.getUserId()));
        }
	}
	
	private List<UserInfo> createUsers(int numberOfUsers) {
		List<UserInfo> users = new ArrayList<>();
		for (long i = 0; i < numberOfUsers; i++) {
			UserInfo userInfo = new UserInfo(false, i);
			when(mockUserManager.getUserInfo(userInfo.getId())).thenReturn(userInfo);
			users.add(userInfo);
		}
		return users;
	}
	
	private List<Webhook> createDefaultWebhooksForUsers(List<UserInfo> users, int numberOfWebhooksPerUser) {
		return createWebhooksForUsers(users, numberOfWebhooksPerUser, objectId, objectType, true);
	}
	
	private List<Webhook> createWebhooksForUsers(List<UserInfo> users, int numberOfWebhooksPerUser, String objectId, ObjectType objectType, boolean hasReadPermission) {
		List<Webhook> webhooks = new ArrayList<>();
		for (UserInfo userInfo : users) {
			for (int i = 0; i < numberOfWebhooksPerUser; i++) {
				Webhook webhook = new Webhook()
	                    .setWebhookId(String.format("webhookId%s-user%s", i, userInfo.getId()))
	                    .setUserId(String.valueOf(userInfo.getId()))
	                    .setObjectId(objectId)
	                    .setObjectType(webhookObjectType)
	                    .setIsWebhookEnabled(true)
	            		.setIsVerified(true);
				
				if (hasReadPermission) {
					when(mockAclDao.canAccess(userInfo, objectId, objectType, ACCESS_TYPE.READ))
							.thenReturn(AuthorizationStatus.authorized());
				} else {
					String accessDeniedMessage = String.format("You do not have %s permission for %s : %s", ACCESS_TYPE.READ, webhook.getObjectType(), webhook.getObjectId());
	        		when(mockAclDao.canAccess(userInfo, objectId, objectType, ACCESS_TYPE.READ))
	        				.thenReturn(AuthorizationStatus.accessDenied(accessDeniedMessage));
				}
				
	            webhooks.add(webhook);
	        }
		}
        return webhooks;
	}

}
