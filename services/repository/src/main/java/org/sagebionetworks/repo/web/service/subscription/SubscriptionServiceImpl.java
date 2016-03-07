package org.sagebionetworks.repo.web.service.subscription;

import java.util.List;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.subscription.SubscriptionManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectId;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
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
	public SubscriptionPagedResults getList(Long userId, SubscriptionObjectType objectType, List<SubscriptionObjectId> objectIds) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.getList(userInfo, objectType, objectIds);
	}

	@Override
	public void delete(Long userId, String subscriptionId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		subscriptionManager.delete(userInfo, subscriptionId);
	}

}
