package org.sagebionetworks.repo.web.service.subscription;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.subscription.Etag;
import org.sagebionetworks.repo.model.subscription.SortByType;
import org.sagebionetworks.repo.model.subscription.SortDirection;
import org.sagebionetworks.repo.model.subscription.SubscriberCount;
import org.sagebionetworks.repo.model.subscription.SubscriberPagedResults;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
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
	 * @param sortDirection 
	 * @param sortByType 
	 * @param sortDirection 
	 * @param sortByType 
	 * @return
	 */
	public SubscriptionPagedResults getAll(Long userId, Long limit, Long offset, SubscriptionObjectType objectType, SortByType sortByType, SortDirection sortDirection);

	/**
	 * retrieve subscriptions one has based on a list of provided topics
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	public SubscriptionPagedResults getList(Long userId, SubscriptionRequest request);

	/**
	 * unsubscribe to a topic
	 * 
	 * @param userId
	 * @param subscriptionId
	 */
	public void delete(Long userId, String subscriptionId);

	/**
	 * unsubscribe to all topic
	 * 
	 * @param userId
	 */
	public void deleteAll(Long userId);

	/**
	 * retrieve a subscription given its ID
	 * 
	 * @param userId
	 * @param id
	 * @return
	 */
	public Subscription get(Long userId, String id);

	/**
	 * retrieve the current etag of the given topic's object
	 * 
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	public Etag getEtag(String objectId, ObjectType objectType);

	/**
	 * retrieve a page of subscribers for a given topic
	 * 
	 * @param userId
	 * @param topic
	 * @param nextPageToken
	 * @return
	 */
	public SubscriberPagedResults getSubscribers(Long userId, Topic topic, String nextPageToken);

	/**
	 * retrieve number of subscribers for a given topic
	 * 
	 * @param userId
	 * @param topic
	 * @return
	 */
	public SubscriberCount getSubscriberCount(Long userId, Topic topic);

	/**
	 * Subscribe to all topic of the same SubscriptionObjectType
	 * 
	 * @param userId
	 * @param objectType
	 * @return
	 */
	public Subscription subscribeAll(Long userId, SubscriptionObjectType objectType);

}
