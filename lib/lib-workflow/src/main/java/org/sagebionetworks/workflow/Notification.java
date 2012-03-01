package org.sagebionetworks.workflow;

import org.apache.log4j.Logger;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.SubscribeResult;

/**
 * Workflow activities relevant to notification of workflow events.
 * 
 * @author deflaux
 * 
 */
public class Notification {

	private static final Logger log = Logger.getLogger(Notification.class
			.getName());
	private static final String EMAIL_SNS_PROTOCOL = "email";

	/**
	 * Subscribe a particular recipients email address to the specified SNS
	 * topic
	 * 
	 * @param snsClient
	 * @param topic
	 * @param recipient
	 */
	public static void doSnsSubscribeFollower(AmazonSNS snsClient,
			String topic, String recipient) {
		log.debug("subscribing to " + topic + " " + recipient);

		SubscribeRequest subscribeRequest = new SubscribeRequest(topic,
				EMAIL_SNS_PROTOCOL, recipient);

		SubscribeResult subscribeResult = snsClient.subscribe(subscribeRequest);
		log.debug("SNS subscribe: " + subscribeResult);

	}

	// Per
	// http://docs.amazonwebservices.com/sns/latest/api/
	// Constraints: Messages must be UTF-8 encoded strings at most 8 KB in size
	// (8192 bytes, not 8192 characters).
	public static int MAX_MESSAGE_BYTE_LENGTH = 8000;

	public static String truncateMessageToMaxLength(String message) {
		int byteLength = message.getBytes().length;
		while (byteLength > MAX_MESSAGE_BYTE_LENGTH) {
			int newCharLength = message.length() * MAX_MESSAGE_BYTE_LENGTH
					/ byteLength;
			message = message.substring(0, newCharLength);
			byteLength = message.getBytes().length;
		}
		return message;
	}

	/**
	 * Notify followers subscribed to an SNS topic by publishing a message to
	 * the specified topic
	 * 
	 * @param snsClient
	 * @param topic
	 * @param subject
	 * @param message
	 */
	public static void doSnsNotifyFollowers(AmazonSNS snsClient, String topic,
			String subject, String message) {

		PublishRequest publishRequest = new PublishRequest(topic,
				truncateMessageToMaxLength(message), subject);

		PublishResult publishResult = snsClient.publish(publishRequest);
		log.debug("SNS publish: " + publishResult + " to topic " + topic);
	}

	/**
	 * Notify a follower by sending an email message.
	 * 
	 * Dev Note: SNS is more robust than a regular old email so prefer SNS when
	 * both are equally appropriate
	 * 
	 * @param recipient
	 * @param subject
	 * @param message
	 */
	public static void doEmailNotifyFollower(String recipient, String subject,
			String message) {

		throw new Error("not yet implemented");
	}

}
