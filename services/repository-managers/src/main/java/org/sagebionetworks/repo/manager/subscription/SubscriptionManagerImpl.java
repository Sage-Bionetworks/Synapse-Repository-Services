package org.sagebionetworks.repo.manager.subscription;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.subscription.Etag;
import org.sagebionetworks.repo.model.subscription.SubscriberPagedResults;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class SubscriptionManagerImpl implements SubscriptionManager {

	@Autowired
	private SubscriptionDAO subscriptionDao;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private DBOChangeDAO changeDao;
	@Autowired
	private AccessControlListDAO aclDao;

	@WriteTransactionReadCommitted
	@Override
	public Subscription create(UserInfo userInfo, Topic toSubscribe) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(toSubscribe, "toSubscribe");
		ValidateArgument.required(toSubscribe.getObjectId(), "Topic.objectId");
		ValidateArgument.required(toSubscribe.getObjectType(), "Topic.objectType");
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canSubscribe(userInfo, toSubscribe.getObjectId(), toSubscribe.getObjectType()));
		Subscription sub = subscriptionDao.create(userInfo.getId().toString(), toSubscribe.getObjectId(), toSubscribe.getObjectType());
		return sub;
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
		ValidateArgument.required(objectType, "objectType");
		Set<Long> projectIds = subscriptionDao.getAllProjects(userInfo.getId().toString(), objectType);
		projectIds = aclDao.getAccessibleBenefactors(userInfo.getGroups(), projectIds, ObjectType.ENTITY, ACCESS_TYPE.READ);
		return subscriptionDao.getAll(userInfo.getId().toString(), limit, offset, objectType, projectIds);
	}

	@WriteTransactionReadCommitted
	@Override
	public void delete(UserInfo userInfo, String subscriptionId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(subscriptionId, "subscriptionId");
		Long id = Long.parseLong(subscriptionId);
		try {
			Subscription sub = subscriptionDao.get(id);
			if (!sub.getSubscriberId().equals(userInfo.getId().toString())) {
				throw new UnauthorizedException("Only the owner of this subscription can perform this action.");
			}
			subscriptionDao.delete(id);
		} catch (NotFoundException e) {
			// subscription does not exist - do nothing
		}
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

	@Override
	public Etag getEtag(String objectId, ObjectType objectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		Long objectIdLong = null;
		if (objectType == ObjectType.ENTITY) {
			objectIdLong = KeyFactory.stringToKey(objectId);
		} else {
			objectIdLong = Long.parseLong(objectId);
		}
		Etag etag = new Etag();
		etag.setEtag(changeDao.getEtag(objectIdLong, objectType));
		return etag;
	}

	@Override
	public SubscriberPagedResults getSubscribers(UserInfo userInfo, Topic topic, String nextPageToken) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(topic, "topic");
		ValidateArgument.required(topic.getObjectId(), "Topic.objectId");
		ValidateArgument.required(topic.getObjectType(), "Topic.objectType");
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canSubscribe(userInfo, topic.getObjectId(), topic.getObjectType()));
		NextPageToken token = new NextPageToken(nextPageToken);
		List<String> subscribers = subscriptionDao.getSubscribers(topic.getObjectId(), topic.getObjectType(), token.getLimitForQuery(), token.getOffset());
		SubscriberPagedResults results = new SubscriberPagedResults();
		results.setNextPageToken(token.getNextPageTokenForCurrentResults(subscribers));
		results.setSubscribers(subscribers);
		return results;
	}
}
