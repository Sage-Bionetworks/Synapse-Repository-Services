package org.sagebionetworks.repo.model.dao.subscription;

import java.util.List;

import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.Topic;

public interface SubscriptionDAO {

	/**
	 * Create a new Subscription
	 * 
	 * @param subscriberId
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	public Subscription create(String subscriberId, String objectId, SubscriptionObjectType objectType);

	/**
	 * Get a subscription given its ID
	 * 
	 * @param subscriptionId
	 * @return
	 */
	public Subscription get(long subscriptionId);

	/**
	 * Get all subscriptions for a given subscriber.
	 * 
	 * @param subscriberId
	 * @param limit
	 * @param offset
	 * @param objectType - optional
	 * @return
	 */
	public SubscriptionPagedResults getAll(String subscriberId, Long limit,
			Long offset, SubscriptionObjectType objectType);

	/**
	 * Get the number of subscription for a given subscriber.
	 * 
	 * @param subscriberId
	 * @param objectType - optional
	 * @return
	 */
	public long getSubscriptionCount(String subscriberId, SubscriptionObjectType objectType);

	/**
	 * Get subscriptions for a subscriber limited by a given list of topic.
	 * 
	 * @param subscriberId
	 * @param listOfTopic
	 * @return
	 */
	public SubscriptionPagedResults getSubscriptionList(String subscriberId, List<Topic> listOfTopic);

	/**
	 * Delete a subscription
	 * 
	 * @param subscriptionId
	 */
	public void delete(long subscriptionId);
}
