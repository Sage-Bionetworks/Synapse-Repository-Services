package org.sagebionetworks.repo.manager.subscription;

import java.util.List;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class SubscriptionManagerImpl implements SubscriptionManager {

	@Autowired
	private SubscriptionDAO subscriptionDao;
	@Autowired
	private AuthorizationManager authorizationManager;

	@Override
	public Subscription create(UserInfo userInfo, Topic toSubscribe) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(toSubscribe, "toSubscribe");
		ValidateArgument.required(toSubscribe.getObjectId(), "Topic.objectId");
		ValidateArgument.required(toSubscribe.getObjectType(), "Topic.objectType");
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canSubscribe(userInfo, toSubscribe.getObjectId(), toSubscribe.getObjectType()));
		return subscriptionDao.create(userInfo.getId().toString(), toSubscribe.getObjectId(), toSubscribe.getObjectType());
	}

	@Override
	public SubscriptionPagedResults getList(UserInfo userInfo, SubscriptionObjectType objectType, List<Long> objectIds) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(objectIds, "objectIds");
		return subscriptionDao.getSubscriptionList(userInfo.getId().toString(), objectType, objectIds);
	}

	@Override
	public SubscriptionPagedResults getAll(UserInfo userInfo, Long limit,
			Long offset, SubscriptionObjectType objectType) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		return subscriptionDao.getAll(userInfo.getId().toString(), limit, offset, objectType);
	}

	@Override
	public void delete(UserInfo userInfo, String subscriptionId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(subscriptionId, "subscriptionId");
		Long id = Long.parseLong(subscriptionId);
		Subscription sub = subscriptionDao.get(id);
		if (!sub.getSubscriberId().equals(userInfo.getId().toString())) {
			throw new UnauthorizedException("Only the user who created this subscription can perform this action.");
		}
		subscriptionDao.delete(id);
	}
}
