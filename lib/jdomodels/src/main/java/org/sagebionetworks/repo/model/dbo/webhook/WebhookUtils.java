package org.sagebionetworks.repo.model.dbo.webhook;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookEventType;
import org.sagebionetworks.repo.model.webhook.WebhookObjectType;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;

public class WebhookUtils {

	public static Webhook translateDboToDto(DBOWebhook dbo) {
		return new Webhook()
			.setId(dbo.getId().toString())
			.setCreatedBy(dbo.getCreatedBy().toString())
			.setCreatedOn(new Date(dbo.getCreatedOn().getTime()))
			.setModifiedOn(new Date(dbo.getModifiedOn().getTime()))
			.setObjectId(dbo.getObjectId().toString())
			.setObjectType(WebhookObjectType.valueOf(dbo.getObjectType()))
			.setEventTypes(eventsFromJson(dbo.getEventTypes()))
			.setInvokeEndpoint(dbo.getInvokeEndpoint())
			.setIsEnabled(dbo.getIsEnabled())
			.setVerificationStatus(WebhookVerificationStatus.valueOf(dbo.getVerificationStatus()))
			.setVerificationMsg(dbo.getVerificationMessage());
	}
	
	public static String eventsToJson(Set<WebhookEventType> events) {
		return new JSONArray(events).toString();
	}
	
	public static Set<WebhookEventType> eventsFromJson(String json) {
		JSONArray jsonArray = new JSONArray(json);
		
		Set<WebhookEventType> events = new TreeSet<>();
		
		jsonArray.forEach( element -> {
			events.add(WebhookEventType.valueOf((String) element));
		});
		
		return events;
	}

}
