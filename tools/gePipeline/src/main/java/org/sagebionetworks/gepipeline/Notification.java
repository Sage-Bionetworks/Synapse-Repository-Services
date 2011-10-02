package org.sagebionetworks.gepipeline;

import org.apache.log4j.Logger;
import org.sagebionetworks.gepipeline.ConfigHelper;

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
	 * @param topic
	 * @param recipient
	 */
	public static void doSnsSubscribeFollower(String topic, String recipient) {
		log.debug("subscribing to " + topic + " " + recipient);

		AmazonSNS snsClient = ConfigHelper.createSNSClient();
		SubscribeRequest subscribeRequest = new SubscribeRequest(topic,
				EMAIL_SNS_PROTOCOL, recipient);

		SubscribeResult subscribeResult = snsClient.subscribe(subscribeRequest);
		log.debug("SNS subscribe: " + subscribeResult);

	}

	/**
	 * Notify followers subscribed to an SNS topic by publishing a message to
	 * the specified topic
	 * 
	 * @param topic
	 * @param subject
	 * @param message
	 */
	public static void doSnsNotifyFollowers(String topic, String subject,
			String message) {

		AmazonSNS snsClient = ConfigHelper.createSNSClient();
		PublishRequest publishRequest = new PublishRequest(topic, message,
				subject);

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
