package org.sagebionetworks.repo.manager.message;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.principal.SynapseEmailService;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.BroadcastMessageDao;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.util.TimeoutUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class BroadcastMessageManagerImpl implements BroadcastMessageManager {
	
	static private Logger log = LogManager.getLogger(BroadcastMessageManagerImpl.class);
	
	/**
	 * Ignore any message older than 24 hours.
	 */
	public static final long MESSAGE_EXPIRATION_MS = 1000*60*60*24; // 24 hours
	
	// Maps ObjectTypes to factory. Injected (IoC).
	Map<ObjectType, MessageBuilderFactory> factoryMap;

	@Autowired
	SubscriptionDAO subscriptionDAO;
	@Autowired
	BroadcastMessageDao broadcastMessageDao;
	@Autowired
	SynapseEmailService sesClient;
	@Autowired
	DBOChangeDAO changeDao;
	@Autowired
	TimeoutUtils timeoutUtils;

	@Override
	public void broadcastMessage(UserInfo user,	ProgressCallback<ChangeMessage> progressCallback, ChangeMessage changeMessage) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(changeMessage, "changeMessage");
		ValidateArgument.required(changeMessage.getUserId(), "ChangeMessage.userId");
		if(!user.isAdmin()){
			throw new UnauthorizedException("Only an Administrator may call this method");
		}
		// Ignore old change messages
		if(timeoutUtils.hasExpired(MESSAGE_EXPIRATION_MS, changeMessage.getTimestamp().getTime())){
			if(log.isDebugEnabled()){
				log.debug("Ignoring "+changeMessage.getChangeNumber()+" since it is older than the maximum expiration time.");
			}
			return;
		}
		// Does this change message still exist?
		if(!changeDao.doesChangeNumberExist(changeMessage.getChangeNumber())){
			if(log.isDebugEnabled()){
				log.debug("Ignoring "+changeMessage.getChangeNumber()+" since it no longer exists.");
			}
			return;
		}
		// Ignore messages that have already been sent
		if(broadcastMessageDao.wasBroadcast(changeMessage.getChangeNumber())){
			if(log.isDebugEnabled()){
				log.debug("Ignoring "+changeMessage.getChangeNumber()+" since it was already broadcast.");
			}
			return;
		}
		// Record this message as sent to prevent the messages from being sent again.
		broadcastMessageDao.setBroadcast(changeMessage.getChangeNumber());
		// Lookup the factory for this type.
		MessageBuilderFactory factory = factoryMap.get(changeMessage.getObjectType());
		if(factory == null){
			throw new IllegalArgumentException("No factory found for object type: "+changeMessage.getObjectType());
		}
		// The builder creates the email.
		BroadcastMessageBuilder builder = factory.createMessageBuilder(changeMessage.getObjectId(), changeMessage.getChangeType(), changeMessage.getUserId());
		Topic topic = builder.getBroadcastTopic();
		valdiateTopic(topic);
		// Get all of the email subscribers for this topic.
		List<Subscriber> subscribers = subscriptionDAO.getAllEmailSubscribers(topic.getObjectId(), topic.getObjectType());
		// The builder will prepare an email for each subscriber
		for(Subscriber subscriber: subscribers){
			if (subscriber.getSubscriberId().equals(changeMessage.getUserId().toString())) {
				// do not send an email to the user who created this change
				continue;
			}
			// progress between each message
			progressCallback.progressMade(changeMessage);
			SendRawEmailRequest emailRequest = builder.buildEmailForSubscriber(subscriber);
			log.debug("sending email to "+subscriber.getNotificationEmail());
			sesClient.sendRawEmail(emailRequest);
		}
	}
	
	/**
	 * Validate the given topic.
	 * @param topic
	 */
	public static void valdiateTopic(Topic topic){
		ValidateArgument.required(topic, "topic");
		ValidateArgument.required(topic.getObjectId(), "topic.ObjectId");
		ValidateArgument.required(topic.getObjectType(), "topic.ObjectType");
	}

	/**
	 * IoC.
	 * 
	 * @param factoryMap
	 */
	public void setFactoryMap(Map<ObjectType, MessageBuilderFactory> factoryMap) {
		this.factoryMap = factoryMap;
	}
	
	
}
