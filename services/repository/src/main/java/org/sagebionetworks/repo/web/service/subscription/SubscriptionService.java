package org.sagebionetworks.repo.web.service.subscription;

import java.util.List;

import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectId;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.Topic;

public interface SubscriptionService {

	/**
	 * Subscribe to a topic
	 * 
	 * @param userId
	 * @param topic
	 * @return
	 */
	public Subscription create(Long userId, Topic topic);

	/**
	 * retrieve all subscriptions one has
	 * 
	 * @param userId
	 * @param limit
	 * @param offset
	 * @param objectType
	 * @return
	 */
	public SubscriptionPagedResults getAll(Long userId, Long limit, Long offset, SubscriptionObjectType objectType);

	/**
	 * retrieve subscriptions one has based on a list of provided topics
	 * 
	 * @param userId
	 * @param objectType
	 * @param objectIds
	 * @return
	 */
	public SubscriptionPagedResults getList(Long userId,
			SubscriptionObjectType objectType, List<SubscriptionObjectId> objectIds);

	/**
	 * unsubscribe to a topic
	 * 
	 * @param userId
	 * @param subscriptionId
	 */
	public void delete(Long userId, String subscriptionId);
}
