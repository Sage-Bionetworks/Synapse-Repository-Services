package org.sagebionetworks.repo.manager.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.webhook.WebhookAuthorizationManager.WebhookPermissionCacheKey;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.webhook.SynapseEventType;
import org.sagebionetworks.repo.model.webhook.SynapseObjectType;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;

@ExtendWith(MockitoExtension.class)
public class WebhookAuthorizationManagerUnitTest {
	
	@Mock
	private UserManager mockUserManager;
	
	@Mock
	private EntityAuthorizationManager mockAuthorizationManager;
	
	@InjectMocks
	@Spy
	private WebhookAuthorizationManager manager;
	
	private UserInfo userInfo;
	
	private Webhook webhook;
	
	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, 123L);
		
		webhook = new Webhook()
			.setId("456")
			.setCreatedBy(userInfo.getId().toString())
			.setEventTypes(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE))
			.setObjectType(SynapseObjectType.ENTITY)
			.setObjectId("123")
			.setInvokeEndpoint("https://my.endpoint.org/events")
			.setIsEnabled(true)
			.setVerificationStatus(WebhookVerificationStatus.PENDING);
	}

	@Test
	public void testGetReadAuthorizationStatus() {
		
		when(mockAuthorizationManager.hasAccess(userInfo, "123", ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		
		// Call under test
		assertEquals(AuthorizationStatus.authorized(), manager.getReadAuthorizationStatus(userInfo, SynapseObjectType.ENTITY, "123"));
	}

	@Test
	public void testHasWebhookOwnerReadAccess() {
		when(mockUserManager.getUserInfo(userInfo.getId())).thenReturn(userInfo);
		when(mockAuthorizationManager.hasAccess(any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		// Call under test
		assertTrue(manager.hasWebhookOwnerReadAccess(webhook));
			
		// Invoking a second time should not trigger a new hasAccess check
		assertTrue(manager.hasWebhookOwnerReadAccess(webhook));
		
		verify(mockUserManager).getUserInfo(userInfo.getId());
		verify(mockAuthorizationManager).hasAccess(userInfo, webhook.getObjectId(), ACCESS_TYPE.READ);
		
		verifyNoMoreInteractions(mockAuthorizationManager);
	}
	
	@Test
	public void testGetReadAuthorizationStatusWithAdmin() {
		userInfo = new UserInfo(true, 1L);
		
		// Call under test
		assertEquals(AuthorizationStatus.authorized(), manager.getReadAuthorizationStatus(userInfo, SynapseObjectType.ENTITY, "123"));
		
		verifyZeroInteractions(mockAuthorizationManager);
	}
	
	@Test
	public void testLoadWebhookOwnerReadAccessValue() {
		
		when(mockUserManager.getUserInfo(userInfo.getId())).thenReturn(userInfo);
		doReturn(AuthorizationStatus.authorized()).when(manager).getReadAuthorizationStatus(userInfo, webhook.getObjectType(), webhook.getObjectId());
		
		// Call under test
		boolean result = manager.loadWebhookOwnerReadAccessValue(new WebhookPermissionCacheKey(webhook));
		
		assertTrue(result);
	}
	
	@Test
	public void testLoadWebhookOwnerReadAccessValueWithNoAccess() {
		
		when(mockUserManager.getUserInfo(userInfo.getId())).thenReturn(userInfo);
		doReturn(AuthorizationStatus.accessDenied("denied")).when(manager).getReadAuthorizationStatus(userInfo, webhook.getObjectType(), webhook.getObjectId());
		
		// Call under test
		boolean result = manager.loadWebhookOwnerReadAccessValue(new WebhookPermissionCacheKey(webhook));
		
		assertFalse(result);
	}
	
}
