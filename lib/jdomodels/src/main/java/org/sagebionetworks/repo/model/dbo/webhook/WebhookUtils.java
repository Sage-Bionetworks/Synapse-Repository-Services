package org.sagebionetworks.repo.model.dbo.webhook;

import java.sql.Timestamp;

import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookVerification;

public class WebhookUtils {

	public static DBOWebhook translateWebhookToDBOWebhook(Webhook dto) {
		DBOWebhook dbo = new DBOWebhook();
		dbo.setId(translateId(dto.getWebhookId()));
		dbo.setObjectId(translateId(dto.getObjectId()));
		dbo.setObjectType(dto.getObjectType().name());
		dbo.setUserId(translateId(dto.getUserId()));
		dbo.setInvokeEndpoint(dto.getInvokeEndpoint());
		dbo.setIsWebhookEnabled(dto.getIsWebhookEnabled());
		dbo.setIsAuthenticationEnabled(dto.getIsAuthenticationEnabled());
		if (dto.getCreatedOn() != null) {
			dbo.setCreatedOn(new Timestamp(dto.getCreatedOn().getTime()));
		}
		dbo.setCreatedBy(translateId(dto.getCreatedBy()));
		dbo.setModifiedBy(translateId(dto.getModifiedBy()));
		return dbo;
	}

	public static DBOWebhookVerification translateWebhookVerificationToDBOWebhookVerification(WebhookVerification dto) {
		DBOWebhookVerification dbo = new DBOWebhookVerification();
		dbo.setWebhookId(translateId(dto.getWebhookId()));
		dbo.setVerificationCode(dto.getVerificationCode());
		dbo.setExpiresOn(new Timestamp(dto.getExpiresOn().getTime()));
		dbo.setAttempts(dto.getAttempts());
		if (dto.getCreatedOn() != null) {
			dbo.setCreatedOn(new Timestamp(dto.getCreatedOn().getTime()));
		}
		if (dto.getModifiedOn() != null) {
			dbo.setModifiedOn(new Timestamp(dto.getModifiedOn().getTime()));
		}
		dbo.setCreatedBy(translateId(dto.getCreatedBy()));
		dbo.setModifiedBy(translateId(dto.getModifiedBy()));
		return dbo;
	}

	public static Long translateId(String id) {
		if (id == null) {
			return null;
		}

		return KeyFactory.stringToKey(id);
	}
}
