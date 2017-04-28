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
	 * @return
	 */
	public SubscriptionPagedResults getAllSubscriptions(String subscriberId, Long limit,
			Long offset, SubscriptionObjectType objectType);

	/**
	 * Get all thread subscriptions for a given subscriber.
	 * 
	 * @param subscriberId
	 * @param limit
	 * @param offset
	 * @param projectIds
	 * @return
	 */
	public SubscriptionPagedResults getAllThreadSubscriptions(String subscriberId, Long limit, Long offset,
			Set<Long> projectIds);

	/**
	 * Get all forum subscriptions for a given subscriber.
	 * 
	 * @param subscriberId
	 * @param limit
	 * @param offset
	 * @param projectIds
	 * @return
	 */
	public SubscriptionPagedResults getAllForumSubscriptions(String subscriberId, Long limit, Long offset,
			Set<Long> projectIds);

	/**
	 * Get subscriptions for a subscriber limited by a given list of topic.
	 * 
	 * @param subscriberId
	 * @param objectType
	 * @param objectIds
	 * @return
	 */
	public SubscriptionPagedResults listSubscriptions(String subscriberId,
			SubscriptionObjectType objectType, List<String> objectIds);

	/**
	 *  Get subscriptions for a subscriber limited by a given list of forumIds.
	 * 
	 * @param subscriberId
	 * @param forumIds
	 * @return
	 */
	public SubscriptionPagedResults listSubscriptionForForum(String subscriberId, List<String> forumIds);

	/**
	 *  Get subscriptions for a subscriber limited by a given list of threadIds.
	 * 
	 * @param subscriberId
	 * @param threadIds
	 * @return
	 */
	public SubscriptionPagedResults listSubscriptionForThread(String subscriberId, List<String> threadIds);

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
	 * Retrieve all projects that a user has thread subscriptions to
	 * 
	 * @param userId
	 * @return
	 */
	public Set<Long> getAllProjectsUserHasThreadSubs(String subscriberId);

	/**
	 * Retrieve all projects that a user has forum subscriptions to
	 * 
	 * @param userId
	 * @return
	 */
	public Set<Long> getAllProjectsUserHasForumSubs(String subscriberId);

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
