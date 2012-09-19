package org.sagebionetworks.search.workers.sqs.search;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageQueue;
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
 * Implementation of the search message queue.  This queue is used to update the search index.
 * 
 * @author John
 *
 */
public class SearchMessageQueue implements MessageQueue {
	
	static private Log log = LogFactory.getLog(SearchMessageQueue.class);
	
	// The first argument is the ARN of the queue, and the second is the ARN of the topic.
	public static final String GRAN_SET_MESSAGE_TEMPLATE = "{ \"Id\":\"GrantRepoTopicSendMessage\", \"Statement\": [{ \"Sid\":\"1\",  \"Resource\": \"%1$s\", \"Effect\": \"Allow\", \"Action\": \"SQS:SendMessage\", \"Condition\": {\"ArnEquals\": {\"aws:SourceArn\": \"%2$s\"}}, \"Principal\": {\"AWS\": \"*\"}}]}";
	
	@Autowired
	AmazonSQSClient awsSQSClient;
	
	@Autowired
	AmazonSNSClient awsSNSClient;
	
	Subscription subscription;
	
	private String queueUrl;
	private String queueArn;
	private String topicArn;
	
	/**
	 * Called by Spring when this bean is created.
	 */
	public void initialize(){
		// Make sure our queue exists
		CreateQueueResult result = awsSQSClient.createQueue(new CreateQueueRequest(getQueueName()));
		queueUrl = result.getQueueUrl();
		queueArn = lookupQueueARN();
		if(queueArn == null) throw new IllegalStateException("Failed to get the ARN for Queue: "+getQueueName());
		log.info(this.toString());
		// Next make sure this Queue is subscribing to the repository topic.
		CreateTopicResult topic = awsSNSClient.createTopic(new CreateTopicRequest(StackConfiguration.getRepositoryChangeTopicName()));
		topicArn = topic.getTopicArn();
		// Subscribe this queue to the topic if needed.
		subscription = subscribQueueToTopicIfNeeded(topicArn);
		if(subscription == null) throw new IllegalStateException("Failed to subscribe to a topic");
		
		// Make sure the topic has the permission it needs
		grantPolicyIfNeeded(topic.getTopicArn());
	}
	
	public String getTopicArn(){
		return topicArn;
	}

	@Override
	public String getQueueUrl() {
		return queueUrl;
	}
	
	/**
	 * Lookup the Queue's ARN
	 * @return
	 */
	private String lookupQueueARN(){
		GetQueueAttributesResult attsResult = awsSQSClient.getQueueAttributes(new GetQueueAttributesRequest().withQueueUrl(queueUrl).withAttributeNames("QueueArn"));
		return attsResult.getAttributes().get("QueueArn");
	}
	
	/**
	 * Grant the topic permission to write to the queue if it does not already have such a permission.
	 * @param topicArn
	 */
	private void grantPolicyIfNeeded(String topicArn){
		GetQueueAttributesResult attsResult = awsSQSClient.getQueueAttributes(new GetQueueAttributesRequest().withQueueUrl(queueUrl).withAttributeNames("Policy"));
		String policyString =  attsResult.getAttributes().get("Policy");
		log.info("Currently policy: "+policyString);
		if(policyString == null || policyString.indexOf(topicArn) < 1){
			log.info("Policy not set to grant the topic write permission to the queue.  Adding a policy now...");
			// Now we need to grant the topic permission to send messages to the queue.
			String permissionString = String.format(GRAN_SET_MESSAGE_TEMPLATE, queueArn,  topicArn);
			Map<String, String> map = new HashMap<String, String>();
			map.put("Policy", permissionString);
			log.info("Setting policy to: "+permissionString);
			awsSQSClient.setQueueAttributes(new SetQueueAttributesRequest().withQueueUrl(queueUrl).withAttributes(map));
		}else{
			log.info("Topic already has sendMessage permssion on this queue");
		}
	}
	
	public String getQueueName(){
		return StackConfiguration.getSearchUpdateQueueName();
	}
	
	public String toString(){
		return String.format("Search Update Queue Name: '%1$s', URL: '%2$s'", getQueueName(), getQueueUrl());
	}
	
	/**
	 * Subscribe this queue to the topic if needed.
	 * @param topicArn
	 */
	public Subscription subscribQueueToTopicIfNeeded(String topicArn){
		Subscription sub = findSubscription(topicArn);
		if(sub != null) return sub;
		// We did not find the subscription so create it.
		awsSNSClient.subscribe(new SubscribeRequest(topicArn, "sqs", queueArn));
		return findSubscription(topicArn);
	}
	
	/**
	 * Find this subscription if it exists
	 * @param topicArn
	 * @return
	 */
	private Subscription findSubscription(String topicArn){
		ListSubscriptionsByTopicResult result;
		do{
			// Keep looking until we find it or run out of nextTokens.
			result = awsSNSClient.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn));
			for(Subscription subscription: result.getSubscriptions()){
				if(subscription.getProtocol().equals("sqs") && subscription.getEndpoint().equals(queueArn)){
					return subscription;
				}
			}
		}while (result.getNextToken() != null);
		return null;
	}
	

}
