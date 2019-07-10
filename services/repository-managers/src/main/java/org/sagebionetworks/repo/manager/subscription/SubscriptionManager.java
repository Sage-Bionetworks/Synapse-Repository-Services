package org.sagebionetworks.repo.manager.subscription;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.subscription.SortByType;
import org.sagebionetworks.repo.model.subscription.SortDirection;
import org.sagebionetworks.repo.model.subscription.SubscriberCount;
import org.sagebionetworks.repo.model.subscription.SubscriberPagedResults;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
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
	 * @param request
	 * @return
	 */
	public SubscriptionPagedResults getList(UserInfo userInfo, SubscriptionRequest request);

	/**
	 * Get all subscriptions for a user
	 * 
	 * @param userInfo
	 * @param limit
	 * @param offset
	 * @param objectType
	 * @param sortDirection 
	 * @param sortByType 
	 * @return
	 */
	public SubscriptionPagedResults getAll(UserInfo userInfo, Long limit, Long offset, SubscriptionObjectType objectType, SortByType sortByType, SortDirection sortDirection);

	/**
	 * Delete a subscription when user unsubscribe to a topic
	 * 
	 * @param userInfo
	 * @param subscriptionId
	 */
	public void delete(UserInfo userInfo, String subscriptionId);

	/**
	 * Delete all subscriptions one has
	 * 
	 * @param userInfo
	 */
	public void deleteAll(UserInfo userInfo);

	/**
	 * Retrieve a subscription given its ID
	 * 
	 * @param userInfo
	 * @param id
	 * @return
	 */
	public Subscription get(UserInfo userInfo, String id);

	/**
	 * Retrieve a list of subscribers for a given topic
	 * 
	 * @param userInfo
	 * @param topic
	 * @param nextPageToken
	 * @return
	 */
	public SubscriberPagedResults getSubscribers(UserInfo userInfo, Topic topic, String nextPageToken);

	/**
	 * Retrieve number of subscribers for a given topic
	 * 
	 * @param userInfo
	 * @param topic
	 * @return
	 */
	public SubscriberCount getSubscriberCount(UserInfo userInfo, Topic topic);

	/**
	 * Create a new subscription all all topics with toSubscribe SubscriptionObjectType.
	 * 
	 * @param userInfo
	 * @param toSubscribe
	 * @return
	 */
	public Subscription subscribeAll(UserInfo userInfo, SubscriptionObjectType toSubscribe);

}
