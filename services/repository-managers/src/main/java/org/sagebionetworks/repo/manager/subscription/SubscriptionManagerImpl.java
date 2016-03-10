package org.sagebionetworks.repo.manager.subscription;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class SubscriptionManagerImpl implements SubscriptionManager {

	@Autowired
	private SubscriptionDAO subscriptionDao;
	@Autowired
	private AuthorizationManager authorizationManager;

	@WriteTransactionReadCommitted
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
	public SubscriptionPagedResults getList(UserInfo userInfo, SubscriptionRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getObjectType(), "SubscriptionRequest.objectType");
		ValidateArgument.required(request.getIdList(), "SubscriptionRequest.idList");
		return subscriptionDao.getSubscriptionList(userInfo.getId().toString(), request.getObjectType(), request.getIdList());
	}

	@Override
	public SubscriptionPagedResults getAll(UserInfo userInfo, Long limit,
			Long offset, SubscriptionObjectType objectType) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		return subscriptionDao.getAll(userInfo.getId().toString(), limit, offset, objectType);
	}

	@WriteTransactionReadCommitted
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

	@WriteTransactionReadCommitted
	@Override
	public void deleteAll(UserInfo userInfo) {
		ValidateArgument.required(userInfo, "userInfo");
		subscriptionDao.deleteAll(userInfo.getId());
	}

	@Override
	public Subscription get(UserInfo userInfo, String subscriptionId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(subscriptionId, "subscriptionId");
		Long id = Long.parseLong(subscriptionId);
		Subscription sub = subscriptionDao.get(id);
		if (!sub.getSubscriberId().equals(userInfo.getId().toString())) {
			throw new UnauthorizedException("Only the user who created this subscription can perform this action.");
		}
		return sub;
	}
}
