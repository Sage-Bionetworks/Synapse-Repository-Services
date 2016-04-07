package org.sagebionetworks.repo.manager.subscription;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.subscription.Etag;
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
	private DiscussionThreadDAO threadDao;
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
		if (toSubscribe.getObjectType() == SubscriptionObjectType.FORUM) {
			subscribeToAllExistingThreads(userInfo.getId().toString(), toSubscribe.getObjectId());
		}
		return sub;
	}

	private void subscribeToAllExistingThreads(String userId, String forumId) {
		List<String> threadIdList = threadDao.getAllThreadIdForForum(forumId);
		subscriptionDao.subscribeAll(userId, threadIdList, SubscriptionObjectType.THREAD);
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
		Subscription sub = subscriptionDao.get(id);
		if (!sub.getSubscriberId().equals(userInfo.getId().toString())) {
			throw new UnauthorizedException("Only the user who created this subscription can perform this action.");
		}
		if (sub.getObjectType() == SubscriptionObjectType.FORUM) {
			unsubscribeToAllExistingThreads(userInfo.getId().toString(), sub.getObjectId());
		}
		subscriptionDao.delete(id);
	}

	private void unsubscribeToAllExistingThreads(String userId, String forumId) {
		List<String> threadIdList = threadDao.getAllThreadIdForForum(forumId);
		subscriptionDao.deleteList(userId, threadIdList, SubscriptionObjectType.THREAD);
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
}
