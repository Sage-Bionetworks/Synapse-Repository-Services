package org.sagebionetworks.message.workers;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.UnsentMessageRange;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.model.Message;

/**
 * Gets UnsentMessageRange messages from a queue and processes them
 */
public class UnsentMessagePopper implements Callable<List<Message>> {
	
	private Log log = LogFactory.getLog(UnsentMessagePopper.class);
	
	private AmazonSNSClient awsSNSClient;
	private Consumer consumer;
	private DBOChangeDAO changeDAO;
	private String topicArn;
	private List<Message> messages;
	
	public UnsentMessagePopper(AmazonSNSClient awsSNSClient, 
			Consumer consumer, DBOChangeDAO changeDAO, 
			String topicArn, List<Message> messages) {
		this.awsSNSClient = awsSNSClient;
		this.consumer = consumer;
		this.changeDAO = changeDAO;
		this.topicArn = topicArn;
		this.messages = messages;
	}

	@Override
	public List<Message> call() throws Exception {
		final long start = System.currentTimeMillis();
		long unsentMessageCount = 0;
		
		List<Message> processedMessages = new LinkedList<Message>();
		for (Message message: messages) {
			// Get all the unsent ChangeMessages in a range and publish them
			UnsentMessageRange range = MessageUtils.extractUnsentMessageBody(message);
			List<ChangeMessage> changes = changeDAO.listUnsentMessages(range.getLowerBound(), range.getUpperBound());
			for (int i = 0; i < changes.size(); i++) {
				publishMessage(changes.get(i));
			}
			
			// Keep track of how many messages were unsent
			unsentMessageCount += changes.size();
			
			processedMessages.add(message);
		}
		
		// Record the amount of time spent and how many messages were resent
		addMetric("ProcessRangeOfMessagesLatency", System.currentTimeMillis() - start, "Milliseconds");
		addMetric("UnsentMessageCount", unsentMessageCount, "Count");
		return processedMessages;
	}
	
	/**
	 * Publish the ChangeMessage and record it as "sent"
	 */
	private void publishMessage(ChangeMessage message) {
		try {
			String json = EntityFactory.createJSONStringForEntity(message);
			awsSNSClient.publish(new PublishRequest(this.topicArn, json));
			changeDAO.registerMessageSent(message.getChangeNumber());
		} catch (JSONObjectAdapterException e) {
			// This should not occur.
			// If it does we want to log it but continue to send messages
			//   as this is called from a timer and not a web-service.
			log.error("Failed to parse ChangeMessage:", e);
		}
	}

	private void addMetric(String name, long metric, String unit) {
		ProfileData profileData = new ProfileData();
		profileData.setNamespace("UnsentMessagePopper");
		profileData.setName(name);
		profileData.setLatency(metric);
		profileData.setUnit(unit);
		profileData.setTimestamp(new Date());
		consumer.addProfileData(profileData);
	}
}
