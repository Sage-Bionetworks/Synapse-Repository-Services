package org.sagebionetworks.repo.model.dbo.persistence.subscription;

import java.util.Date;

import org.sagebionetworks.repo.model.subscription.SubscriptionObjectId;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.util.ValidateArgument;

public class SubscriptionUtils {

	public static DBOSubscription createDBO(long subscriptionId, String subscriberId,
			SubscriptionObjectId objectId, SubscriptionObjectType objectType, Date createdOn) {
		ValidateArgument.required(subscriberId, "subscriberId");
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(createdOn, "createdOn");
		DBOSubscription dbo = new DBOSubscription();
		dbo.setId(subscriptionId);
		dbo.setSubscriberId(Long.parseLong(subscriberId));
		dbo.setObjectId(Long.parseLong(objectId.getId()));
		dbo.setObjectType(objectType.name());
		dbo.setCreatedOn(createdOn.getTime());
		return dbo;
	}
}
