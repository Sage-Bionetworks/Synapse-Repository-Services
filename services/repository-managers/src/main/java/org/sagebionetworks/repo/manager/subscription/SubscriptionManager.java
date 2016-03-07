package org.sagebionetworks.repo.manager.subscription;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.Topic;

public interface SubscriptionManager {

	/**
	 * Create a new subscription
	 * 
	 * @param userInfo
	 * @param toSubscribe
	 * @return
	 */
	public Subscription create(UserInfo userInfo, Topic toSubscribe);

	/**
	 * Get subscriptions for a user based on a list of topics
	 * 
	 * @param userInfo
	 * @param objectType
	 * @param objectIds
	 * @return
	 */
	public SubscriptionPagedResults getList(UserInfo userInfo,
			SubscriptionObjectType objectType, List<Long> objectIds);

	/**
	 * Get all subscriptions for a user
	 * 
	 * @param userInfo
	 * @param limit
	 * @param offset
	 * @param objectType
	 * @return
	 */
	public SubscriptionPagedResults getAll(UserInfo userInfo, Long limit, Long offset, SubscriptionObjectType objectType);

	/**
	 * Delete a subscription when user unsubscribe to a topic
	 * 
	 * @param userInfo
	 * @param subscriptionId
	 */
	public void delete(UserInfo userInfo, String subscriptionId);

}
