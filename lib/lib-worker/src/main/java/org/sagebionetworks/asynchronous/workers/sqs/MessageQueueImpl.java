package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.sagebionetworks.repo.model.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;

/**
 * The default implementation of a message queue.
 *
 * @author jmhill
 * @author ewu
 */
public class MessageQueueImpl implements MessageQueue {

	private Logger logger = LogManager.getLogger(MessageQueueImpl.class);

	// The first argument is the ARN of the queue, and the second is the ARN of the topic.
	private static final String GRAN_SET_MESSAGE_TEMPLATE = "{ \"Id\":\"GrantRepoTopicSendMessage\", \"Statement\": [{ \"Sid\":\"1\",  \"Resource\": \"%1$s\", \"Effect\": \"Allow\", \"Action\": \"SQS:SendMessage\", \"Condition\": {\"ArnEquals\": {\"aws:SourceArn\": \"%2$s\"}}, \"Principal\": {\"AWS\": \"*\"}}]}";

	@Autowired
	private AmazonSQSClient awsSQSClient;

	@Autowired
	private AmazonSNSClient awsSNSClient;

	private final String queueName;
	private final List<ObjectType> objectTypes;
	private String queueUrl;
	private String topicPrefixOrName;
	private boolean isEnabled;

	public MessageQueueImpl(final String queueName, String topicPrefixOrName, List<ObjectType> objectTypes, boolean isEnabled) {
		if (queueName == null) {
			throw new IllegalArgumentException("QueueName cannot be null");
		}
		if (objectTypes != null && objectTypes.isEmpty()) {
			throw new IllegalArgumentException("ObjectTypes cannot be empty");
		}
		this.isEnabled = isEnabled;
		this.queueName = queueName;
		this.objectTypes = objectTypes;
		this.topicPrefixOrName = topicPrefixOrName;
	}

	@PostConstruct
	private void init() {
		// Do nothing if it is not enabled
		if(!isEnabled){
			logger.info("Queue: "+queueName+" will not be configured because it is not enabled");
			return;
		}
		// Create the queue if it does not already exist
		CreateQueueRequest cqRequest = new CreateQueueRequest(queueName);
		CreateQueueResult cqResult = this.awsSQSClient.createQueue(cqRequest);
		final String queueUrl = cqResult.getQueueUrl();

		String attrName = "QueueArn";
		GetQueueAttributesRequest attrRequest = new GetQueueAttributesRequest()
				.withQueueUrl(queueUrl)
				.withAttributeNames(attrName);
		GetQueueAttributesResult attrResult = this.awsSQSClient.getQueueAttributes(attrRequest);
		final String queueArn = attrResult.getAttributes().get(attrName);

		if (queueArn == null) {
			throw new IllegalStateException("Failed to get the ARN for Queue: " + getQueueName());
		}

		this.logger.info("Queue created. URL: " + queueUrl + " ARN: " + queueArn);
		// create topics and setup access.
		createAndGrandAccessToTopics(queueArn, queueUrl, this.objectTypes);

		this.queueUrl = queueUrl;
	}

	@Override
	public String getQueueUrl() {
		return this.queueUrl;
	}

	@Override
	public String getQueueName(){
		return this.queueName;
	}
	
	/**
	 * Create a topic (if needed) for each type and grant access to publish from the topics to the queue.
	 * 
	 * @param queueArn
	 * @param queueUrl
	 * @param type
	 */
	private void createAndGrandAccessToTopics(String queueArn, String queueUrl, List<ObjectType> types) {
		if (types == null) {
			createAndGrandAccessToTopic(queueArn, queueUrl, null);
		} else {
			for (ObjectType type : types) {
				createAndGrandAccessToTopic(queueArn, queueUrl, type);
			}
		}
	}
	
	/**
	 * Create a topic (if needed) and grant access to publish from the topic to the queue.
	 * @param queueArn
	 * @param queueUrl
	 * @param type
	 */
	private void createAndGrandAccessToTopic(String queueArn, String queueUrl, ObjectType type){
		// Let the queue subscribe to the topic
		String topicName;
		if (type == null) {
			topicName = topicPrefixOrName;
		} else {
			topicName = topicPrefixOrName + type.name();
		}
		CreateTopicRequest ctRequest = new CreateTopicRequest(topicName);
		CreateTopicResult topicResult = this.awsSNSClient.createTopic(ctRequest);
		final String topicArn = topicResult.getTopicArn();
		final Subscription subscription = subscribeQueueToTopicIfNeeded(topicArn, queueArn);
		if (subscription == null) {
			throw new IllegalStateException("Failed to subscribe queue (" + queueArn + ") to topic (" + topicArn + ")");
		}
		// Make sure the topic has the permission it needs
		grantPolicyIfNeeded(topicArn, queueArn, queueUrl, type);
	}

	/**
	 * Subscribes this queue to the topic if needed.
	 */
	private Subscription subscribeQueueToTopicIfNeeded(final String topicArn, final String queueArn) {
		logger.info("Subscribing " + queueArn + " to " + topicArn);
		assert topicArn != null;
		assert queueArn != null;
		Subscription sub = findSubscription(topicArn, queueArn);
		if (sub != null) {
			return sub;
		}
		// We did not find the subscription so create it
		SubscribeRequest request = new SubscribeRequest(topicArn, "sqs", queueArn);
		this.awsSNSClient.subscribe(request);
		return findSubscription(topicArn, queueArn);
	}

	/**
	 * Finds this subscription if it exists.
	 */
	private Subscription findSubscription(final String topicArn, final String queueArn) {
		assert topicArn != null;
		assert queueArn != null;
		ListSubscriptionsByTopicResult result;
		do {
			// Keep looking until we find it or run out of nextTokens.
			ListSubscriptionsByTopicRequest request = new ListSubscriptionsByTopicRequest(topicArn);
			result = this.awsSNSClient.listSubscriptionsByTopic(request);
			for (Subscription subscription : result.getSubscriptions()) {
				if (subscription.getProtocol().equals("sqs") &&
						subscription.getEndpoint().equals(queueArn)) {
					return subscription;
				}
			}
		} while (result.getNextToken() != null);
		return null;
	}

	/**
	 * Grants the topic permission to write to the queue if it does not already have such a permission.
	 */
	private void grantPolicyIfNeeded(final String topicArn, final String queueArn, final String queueUrl, ObjectType type) {
		assert topicArn != null;
		String arnToGrant = topicArn;
		if (type != null) {
			// All topics for this stack should be able to publish to this queue, so we setup a wildcard ARN:
			// for example: 'arn:aws:sns:us-east-1:<userid>:<stack>-<instances>-repo-ENTITY' will become
			// 'arn:aws:sns:us-east-1:<userid>:<stack>-<instances>-repo-*;
			String wildArn = topicArn.replaceAll(type.name(), "*");
			arnToGrant = wildArn;
		}
		GetQueueAttributesRequest attrRequest = new GetQueueAttributesRequest()
				.withQueueUrl(queueUrl)
				.withAttributeNames("Policy");
		GetQueueAttributesResult attrResult = this.awsSQSClient.getQueueAttributes(attrRequest);
		String policyString =  attrResult.getAttributes().get("Policy");
		this.logger.info("Currently policy: " + policyString);
		if (policyString == null || policyString.indexOf(arnToGrant) < 1) {
			this.logger.info("Policy not set to grant the topic write permission to the queue. Adding a policy now...");
			// Now we need to grant the topic permission to send messages to the queue.
			String permissionString = String.format(GRAN_SET_MESSAGE_TEMPLATE, queueArn, arnToGrant);
			Map<String, String> map = new HashMap<String, String>();
			map.put("Policy", permissionString);
			this.logger.info("Setting policy to: "+permissionString);
			SetQueueAttributesRequest setAttrRequest = new SetQueueAttributesRequest()
					.withQueueUrl(queueUrl)
					.withAttributes(map);
			this.awsSQSClient.setQueueAttributes(setAttrRequest);
		} else {
			this.logger.info("Topic already has sendMessage permission on this queue");
		}
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}
}
