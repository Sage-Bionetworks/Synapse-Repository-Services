package org.sagebionetworks.repo.manager.subscription;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionListRequest;
import org.sagebionetworks.repo.model.subscription.SortByType;
import org.sagebionetworks.repo.model.subscription.SortDirection;
import org.sagebionetworks.repo.model.subscription.SubscriberCount;
import org.sagebionetworks.repo.model.subscription.SubscriberPagedResults;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class SubscriptionManagerImpl implements SubscriptionManager {
	public static final String ALL_OBJECT_IDS = "0";

	@Autowired
	private SubscriptionDAO subscriptionDao;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private AccessControlListDAO aclDao;

	@WriteTransaction
	@Override
	public Subscription create(UserInfo userInfo, Topic toSubscribe) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(toSubscribe, "toSubscribe");
		ValidateArgument.required(toSubscribe.getObjectId(), "Topic.objectId");
		ValidateArgument.required(toSubscribe.getObjectType(), "Topic.objectType");
		authorizationManager.canSubscribe(userInfo, toSubscribe.getObjectId(), toSubscribe.getObjectType()).checkAuthorizationOrElseThrow();
		return subscriptionDao.create(userInfo.getId().toString(), toSubscribe.getObjectId(), toSubscribe.getObjectType());
	}

	@WriteTransaction
	@Override
	public Subscription subscribeAll(UserInfo userInfo, SubscriptionObjectType toSubscribe) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(toSubscribe, "toSubscribe");
		authorizationManager.canSubscribe(userInfo, ALL_OBJECT_IDS, toSubscribe).checkAuthorizationOrElseThrow();
		return subscriptionDao.create(userInfo.getId().toString(), ALL_OBJECT_IDS, toSubscribe);
	}

	@Override
	public SubscriptionPagedResults getList(UserInfo userInfo, SubscriptionRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getObjectType(), "SubscriptionRequest.objectType");
		ValidateArgument.required(request.getIdList(), "SubscriptionRequest.idList");
		SubscriptionPagedResults result = new SubscriptionPagedResults();
		List<Subscription> page = subscriptionDao.listSubscriptions(new SubscriptionListRequest()
				.withSubscriberId(userInfo.getId().toString())
				.withObjectIds(request.getIdList())
				.withObjectType(request.getObjectType())
				.withSortByType(request.getSortByType())
				.withSortDirection(request.getSortDirection()));
		result.setResults(page);
		result.setTotalNumberOfResults((long) page.size());
		return result;
	}

	@Override
	public SubscriptionPagedResults getAll(UserInfo userInfo, Long limit,
			Long offset, SubscriptionObjectType objectType, SortByType sortByType, SortDirection sortDirection) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		ValidateArgument.required(objectType, "objectType");
		
		// Lookup the projects for this user and type if relevant.
		Set<Long> projectIds = getAllProjectsUserHasSubscriptions(userInfo, objectType);
		SubscriptionListRequest request = new SubscriptionListRequest()
		.withSubscriberId(userInfo.getId().toString())
		.withObjectType(objectType)
		.withSortByType(sortByType)
		.withSortDirection(sortDirection)
		.withLimit(limit)
		.withOffset(offset)
		.withProjectIds(projectIds);
		List<Subscription> page = subscriptionDao.listSubscriptions(request);
		Long count = subscriptionDao.listSubscriptionsCount(request);
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		results.setResults(page);
		results.setTotalNumberOfResults(count);
		return results;
	}
	
	/**
	 * Get the projects the user has subscriptions for based on the passed type.
	 * Projects the user cannot see are filtered out.
	 * @param userInfo
	 * @param objectType
	 * @return
	 */
	Set<Long> getAllProjectsUserHasSubscriptions(UserInfo userInfo, SubscriptionObjectType objectType) {
		Set<Long> projectIds = null;
		switch (objectType) {
		case FORUM:
			projectIds = subscriptionDao.getAllProjectsUserHasForumSubs(userInfo.getId().toString());
			break;
		case THREAD:
			projectIds = subscriptionDao.getAllProjectsUserHasThreadSubs(userInfo.getId().toString());
			break;
		default:
			// other types do not have projects
			projectIds = null;
		}
		if (projectIds != null) {
			// filter projects the user cannot see.
			projectIds = aclDao.getAccessibleBenefactors(userInfo.getGroups(), projectIds, ObjectType.ENTITY,
					ACCESS_TYPE.READ);
		}
		return projectIds;
	}

	@WriteTransaction
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

	@WriteTransaction
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
	public SubscriberPagedResults getSubscribers(UserInfo userInfo, Topic topic, String nextPageToken) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(topic, "topic");
		ValidateArgument.required(topic.getObjectId(), "Topic.objectId");
		ValidateArgument.required(topic.getObjectType(), "Topic.objectType");
		authorizationManager.canSubscribe(userInfo, topic.getObjectId(), topic.getObjectType()).checkAuthorizationOrElseThrow();
		NextPageToken token = new NextPageToken(nextPageToken);
		List<String> subscribers = subscriptionDao.getSubscribers(topic.getObjectId(), topic.getObjectType(), token.getLimitForQuery(), token.getOffset());
		SubscriberPagedResults results = new SubscriberPagedResults();
		results.setNextPageToken(token.getNextPageTokenForCurrentResults(subscribers));
		results.setSubscribers(subscribers);
		return results;
	}

	@Override
	public SubscriberCount getSubscriberCount(UserInfo userInfo, Topic topic) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(topic, "topic");
		ValidateArgument.required(topic.getObjectId(), "Topic.objectId");
		ValidateArgument.required(topic.getObjectType(), "Topic.objectType");
		authorizationManager.canSubscribe(userInfo, topic.getObjectId(), topic.getObjectType()).checkAuthorizationOrElseThrow();
		SubscriberCount count = new SubscriberCount();
		count.setCount(subscriptionDao.getSubscriberCount(topic.getObjectId(), topic.getObjectType()));
		return count;
	}
}
