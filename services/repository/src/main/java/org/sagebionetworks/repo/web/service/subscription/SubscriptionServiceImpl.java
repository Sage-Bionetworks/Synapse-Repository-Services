package org.sagebionetworks.repo.web.service.subscription;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.subscription.SubscriptionManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.subscription.Etag;
import org.sagebionetworks.repo.model.subscription.SubscriberCount;
import org.sagebionetworks.repo.model.subscription.SubscriberPagedResults;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.springframework.beans.factory.annotation.Autowired;

public class SubscriptionServiceImpl implements SubscriptionService {
	@Autowired
	private UserManager userManager;
	@Autowired
	private SubscriptionManager subscriptionManager;

	@Override
	public Subscription create(Long userId, Topic topic) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.create(userInfo, topic);
	}

	@Override
	public SubscriptionPagedResults getAll(Long userId, Long limit, Long offset, SubscriptionObjectType objectType) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.getAll(userInfo, limit, offset, objectType);
	}

	@Override
	public SubscriptionPagedResults getList(Long userId, SubscriptionRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.getList(userInfo, request);
	}

	@Override
	public void delete(Long userId, String subscriptionId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		subscriptionManager.delete(userInfo, subscriptionId);
	}

	@Override
	public void deleteAll(Long userId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		subscriptionManager.deleteAll(userInfo);
	}

	@Override
	public Subscription get(Long userId, String id) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.get(userInfo, id);
	}

	@Override
	public Etag getEtag(String objectId, ObjectType objectType) {
		return subscriptionManager.getEtag(objectId, objectType);
	}

	@Override
	public SubscriberPagedResults getSubscribers(Long userId, Topic topic, String nextPageToken) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.getSubscribers(userInfo, topic, nextPageToken);
	}

	@Override
	public SubscriberCount getSubscriberCount(Long userId, Topic topic) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.getSubscriberCount(userInfo, topic);
	}

	@Override
	public Subscription subscribeAll(Long userId, SubscriptionObjectType objectType) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.subscribeAll(userInfo, objectType);
	}

}
