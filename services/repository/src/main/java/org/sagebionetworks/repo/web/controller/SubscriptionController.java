package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.subscription.Etag;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <p>While working in Synapse, users may want to subscribe to different topics
 * to receive notifications about changes in those topics.</p>
 * <br>
 * <p>These services provide the APIs for Synapse users to manage their subscriptions.</p>
 * <br>
 */
@ControllerInfo(displayName = "Subscription Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class SubscriptionController extends BaseController{

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * This API is used to subscribe to a topic.
	 * <br/>
	 * Target users: anyone who has READ permission on the object.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param topic - Topic to subscribe to
	 * @return
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION, method = RequestMethod.POST)
	public @ResponseBody Subscription create(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody Topic topic) {
		return serviceProvider.getSubscriptionService().create(userId, topic);
	}

	/**
	 * This API is used to retrieve subscriptions one has based on a list of provided topics.
	 * These topics must have the same objectType.
	 * <br/>
	 * Target users: all Synapse users.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param request - This object defines what topics the user is asking for
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION_LIST, method = RequestMethod.POST)
	public @ResponseBody SubscriptionPagedResults getList(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SubscriptionRequest request) {
		return serviceProvider.getSubscriptionService().getList(userId, request);
	}

	/**
	 * This API is used to retrieve a subscription given its ID
	 * <br/>
	 * Target users: Synapse user who created this subscription.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param id - the ID of the subscription that is created when the user subscribed to the topic
	  * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION_ID, method = RequestMethod.GET)
	public @ResponseBody Subscription get(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id) {
		return serviceProvider.getSubscriptionService().get(userId, id);
	}

	/**
	 * This API is used to retrieve all subscriptions one has.
	 * <br/>
	 * Target users: all Synapse users.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param limit - Limits the size of the page returned. For example, a page size of 10 require limit = 10. The maximum Limit for this call is 100.
	 * @param offset - The index of the pagination offset. For a page size of 10, the first page would be at offset = 0, and the second page would be at offset = 10.
	 * @param objectType - User can use this param to filter the results by the type of object they subscribed to.
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION_ALL, method = RequestMethod.GET)
	public @ResponseBody SubscriptionPagedResults getAll(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM) Long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM) Long offset,
			@RequestParam(value = ServiceConstants.SUBSCRIPTION_OBJECT_TYPE_PARAM) SubscriptionObjectType objectType) {
		return serviceProvider.getSubscriptionService().getAll(userId, limit, offset, objectType);
	}

	/**
	 * This API is used to unsubscribe to a topic.
	 * <br/>
	 * Target users: Synapse user who created this subscription.
	 * 
	 * @param userId - the ID of the user who is making the request
	 * @param id - the ID of the subscription that is created when the user subscribed to the topic
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION_ID, method = RequestMethod.DELETE)
	public void delete(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id) {
		serviceProvider.getSubscriptionService().delete(userId, id);
	}

	/**
	 * This API is used to unsubscribe all topics one followed.
	 * <br/>
	 * Target users: Synapse users
	 * 
	 * @param userId - the ID of the user who is making the request
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION_ALL, method = RequestMethod.DELETE)
	public void deleteAll(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) {
		serviceProvider.getSubscriptionService().deleteAll(userId);
	}

	/**
	 * This API is used to retrieve the etag of a subscribable object.
	 * The client could use this method to check if an object has changed.
	 * 
	 * @param objectId - The ID of the given object
	 * @param objectType - The type of the given object
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OBJECT_ID_TYPE_ETAG, method = RequestMethod.GET)
	public @ResponseBody Etag getEtag(
			@PathVariable String objectId,
			@PathVariable String objectType) {
		return serviceProvider.getSubscriptionService().getEtag(objectId, ObjectType.valueOf(objectType));
	}
}
