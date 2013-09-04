package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	private final String topicName;
	private String queueUrl;

	public MessageQueueImpl(final String queueName, final String topicName) {

		if (queueName == null) {
			throw new NullPointerException();
		}
		if (topicName == null) {
			throw new NullPointerException();
		}

		this.queueName = queueName;
		this.topicName = topicName;
	}

	@PostConstruct
	private void init() {

		// Create the queue if it does not already exist
		CreateQueueRequest cqRequest = new CreateQueueRequest(queueName);
		CreateQueueResult cqResult = this.awsSQSClient.createQueue(cqRequest);
		final String qUrl = cqResult.getQueueUrl();

		String attrName = "QueueArn";
		GetQueueAttributesRequest attrRequest = new GetQueueAttributesRequest()
				.withQueueUrl(qUrl)
				.withAttributeNames(attrName);
		GetQueueAttributesResult attrResult = this.awsSQSClient.getQueueAttributes(attrRequest);
		final String qArn = attrResult.getAttributes().get(attrName);

		if (qArn == null) {
			throw new IllegalStateException("Failed to get the ARN for Queue: " + getQueueName());
		}

		this.logger.info("Queue created. URL: " + qUrl + " ARN: " + qArn);

		// Let the queue subscribe to the topic
		CreateTopicRequest ctRequest = new CreateTopicRequest(topicName);
		CreateTopicResult topicResult = this.awsSNSClient.createTopic(ctRequest);
		final String tArn = topicResult.getTopicArn();
		final Subscription subscription = subscribeQueueToTopicIfNeeded(tArn, qArn);
		if (subscription == null) {
			throw new IllegalStateException("Failed to subscribe queue (" + qArn + ") to topic (" + tArn + ")");
		}

		// Make sure the topic has the permission it needs
		grantPolicyIfNeeded(tArn, qArn, qUrl);

		this.queueUrl = qUrl;
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
	 * Subscribes this queue to the topic if needed.
	 */
	private Subscription subscribeQueueToTopicIfNeeded(final String topicArn, final String queueArn) {
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
	private void grantPolicyIfNeeded(final String topicArn, final String queueArn, final String queueUrl) {
		assert topicArn != null;
		GetQueueAttributesRequest attrRequest = new GetQueueAttributesRequest()
				.withQueueUrl(queueUrl)
				.withAttributeNames("Policy");
		GetQueueAttributesResult attrResult = this.awsSQSClient.getQueueAttributes(attrRequest);
		String policyString =  attrResult.getAttributes().get("Policy");
		this.logger.info("Currently policy: " + policyString);
		if(policyString == null || policyString.indexOf(topicArn) < 1) {
			this.logger.info("Policy not set to grant the topic write permission to the queue. Adding a policy now...");
			// Now we need to grant the topic permission to send messages to the queue.
			String permissionString = String.format(GRAN_SET_MESSAGE_TEMPLATE, queueArn, topicArn);
			Map<String, String> map = new HashMap<String, String>();
			map.put("Policy", permissionString);
			this.logger.info("Setting policy to: "+permissionString);
			SetQueueAttributesRequest setAttrRequest = new SetQueueAttributesRequest()
					.withQueueUrl(queueUrl)
					.withAttributes(map);
			this.awsSQSClient.setQueueAttributes(setAttrRequest);
		} else {
			this.logger.info("Topic already has sendMessage permssion on this queue");
		}
	}
}
