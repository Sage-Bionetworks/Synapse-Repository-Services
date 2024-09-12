package org.sagebionetworks.repo.manager.webhook;

import java.time.Duration;
import java.util.Objects;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.webhook.SynapseObjectType;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Service
public class WebhookAuthorizationManager {
	
	static final Duration PERMISSIONS_CACHE_EXPIRATION = Duration.ofMinutes(5);
	private static final int PERMISSIONS_CACHE_MAX_SIZE = 2_000;

	private LoadingCache<WebhookPermissionCacheKey, Boolean> webhookReadPermissions;

	private UserManager userManager;
	private EntityAuthorizationManager entityAuthorizationManager;

	public WebhookAuthorizationManager(UserManager userManager, EntityAuthorizationManager entityAuthorizationManager) {
		this.userManager = userManager;
		this.entityAuthorizationManager = entityAuthorizationManager;
	}
	
	@Autowired
	public void configure(Clock clock) {
		
		Ticker cacheTicker = new Ticker() {
			@Override
			public long read() {
				return clock.nanoTime();
			}
		};
		
		this.webhookReadPermissions = CacheBuilder.newBuilder()
			.ticker(cacheTicker)
			.expireAfterWrite(PERMISSIONS_CACHE_EXPIRATION)
			.maximumSize(PERMISSIONS_CACHE_MAX_SIZE)
			.build(CacheLoader.from(this::loadWebhookOwnerReadAccessValue));
	}

	public boolean hasWebhookOwnerReadAccess(Webhook webhook) {
		return webhookReadPermissions.getUnchecked(new WebhookPermissionCacheKey(webhook));
	}

	public AuthorizationStatus getReadAuthorizationStatus(UserInfo userInfo, SynapseObjectType objectType, String objectId) {

		if (userInfo.isAdmin()) {
			return AuthorizationStatus.authorized();
		}

		switch (objectType) {
		case ENTITY:
			return entityAuthorizationManager.hasAccess(userInfo, objectId, ACCESS_TYPE.READ)
					.isAuthorizedOrElseGet(() -> "You lack READ access to the synapse object specified by the webhook.");
		default:
			throw new IllegalArgumentException("Unsupported object type " + objectType);
		}
	}
	
	boolean loadWebhookOwnerReadAccessValue(WebhookPermissionCacheKey webhookKey) {
		UserInfo userInfo = userManager.getUserInfo(webhookKey.getUserId());

		return getReadAuthorizationStatus(userInfo, webhookKey.getObjectType(), webhookKey.getObjectId()).isAuthorized();
	}
	
	static class WebhookTokenCacheKey {
		private String webhookId;
		private String webhookOwnerId;
		
		WebhookTokenCacheKey(String webhookId, String webhookOwnerId) {
			this.webhookId = webhookId;
			this.webhookOwnerId = webhookOwnerId;
		}
		
		String getWebhookId() {
			return webhookId;
		}
		
		String getWebhookOwnerId() {
			return webhookOwnerId;
		}

		@Override
		public int hashCode() {
			return Objects.hash(webhookId, webhookOwnerId);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof WebhookTokenCacheKey)) {
				return false;
			}
			WebhookTokenCacheKey other = (WebhookTokenCacheKey) obj;
			return Objects.equals(webhookId, other.webhookId) && Objects.equals(webhookOwnerId, other.webhookOwnerId);
		}
		
	}

	static class WebhookPermissionCacheKey {

		private Long userId;
		private SynapseObjectType objectType;
		private String objectId;

		WebhookPermissionCacheKey(Webhook webhook) {
			this.userId = Long.valueOf(webhook.getCreatedBy());
			this.objectType = webhook.getObjectType();
			this.objectId = webhook.getObjectId();
		}

		Long getUserId() {
			return userId;
		}

		SynapseObjectType getObjectType() {
			return objectType;
		}

		String getObjectId() {
			return objectId;
		}

		@Override
		public int hashCode() {
			return Objects.hash(objectId, objectType, userId);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof WebhookPermissionCacheKey)) {
				return false;
			}
			WebhookPermissionCacheKey other = (WebhookPermissionCacheKey) obj;
			return Objects.equals(objectId, other.objectId) && objectType == other.objectType && Objects.equals(userId, other.userId);
		}

	}
}
