package org.sagebionetworks.repo.model.dao.subscription;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;

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
	 * @param objectType
	 * @param projectIds
	 * @return
	 */
	public SubscriptionPagedResults getAll(String subscriberId, Long limit,
			Long offset, SubscriptionObjectType objectType, Set<Long> projectIds);

	/**
	 * Get subscriptions for a subscriber limited by a given list of topic.
	 * 
	 * @param subscriberId
	 * @param objectType
	 * @param objectIds
	 * @return
	 */
	public SubscriptionPagedResults getSubscriptionList(String subscriberId,
			SubscriptionObjectType objectType, List<String> objectIds);

	/**
	 * Delete a subscription
	 * 
	 * @param subscriptionId
	 */
	public void delete(long subscriptionId);

	/**
	 * Delete all subscriptions this user has
	 * 
	 * @param userId
	 */
	public void deleteAll(Long userId);

	/**
	 * List all subscribers for a given topic
	 * 
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	public List<String> getAllSubscribers(String objectId,
			SubscriptionObjectType objectType);
	/**
	 * Get all email subscribers that want to receive email notifications.
	 * 
	 * @param objectId
	 * @param objectType
	 * 
	 * @return
	 */
	public List<Subscriber> getAllEmailSubscribers(String objectId,
			SubscriptionObjectType objectType);

	/**
	 * Retrieve all projects that a user has subscriptions to
	 * 
	 * @param userId
	 * @param objectType
	 * @return
	 */
	public Set<Long> getAllProjects(String userId, SubscriptionObjectType objectType);

	/**
	 * Create a batch of new Subscriptions to a single topic
	 * 
	 * @param subscriberIds
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	public void subscribeAllUsers(Set<String> subscriberIds, String objectId, SubscriptionObjectType objectType);

	/**
	 * Retrieve a list of subscribers for a given topic
	 * 
	 * @param objectId
	 * @param objectType
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<String> getSubscribers(String objectId, SubscriptionObjectType objectType, long limit, long offset);

	/**
	 * Retrieve the number of subscribers for a given topic
	 * 
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	public long getSubscriberCount(String objectId, SubscriptionObjectType objectType);

}
