package org.sagebionetworks.repo.manager.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.markdown.MarkdownClientException;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.principal.SynapseEmailService;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.ses.EmailQuarantineDao;
import org.sagebionetworks.repo.model.message.BroadcastMessageDao;
import org.sagebionetworks.repo.model.message.ChangeMessage;
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
	
	/*
	 *  For each ChangeMessage that is processed by this manager, we map the
	 *  message's ObjectType to a MessageBuilderFactory. A MessageBuilderFactory
	 *  create a single instance of BroadcastMessageBuilder for each ChangeMessage.
	 *  The BroadcastMessageBuilder instance acts as SendRawEmailRequest's factory.
	 *  It builds SendRawEmailRequest for each user about the event that it was
	 *  configured with.
	 *  
	 *  Injected (IoC).
	 */
	private Map<ObjectType, MessageBuilderFactory> messageBuilderFactoryMap;

	@Autowired
	private SubscriptionDAO subscriptionDAO;
	
	@Autowired
	private BroadcastMessageDao broadcastMessageDao;
	
	@Autowired
	private SynapseEmailService sesClient;
	
	@Autowired
	private DBOChangeDAO changeDao;
	
	@Autowired
	private TimeoutUtils timeoutUtils;
	
	@Autowired
	private UserProfileDAO userProfileDao;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AuthorizationManager authManager;
	
	@Autowired
	private EmailQuarantineDao emailQuarantineDao;
	
	@Override
	public void broadcastMessage(UserInfo user,	ProgressCallback progressCallback, ChangeMessage changeMessage) throws ClientProtocolException, JSONException, IOException, MarkdownClientException {
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
		MessageBuilderFactory factory = messageBuilderFactoryMap.get(changeMessage.getObjectType());
		if(factory == null){
			throw new IllegalArgumentException("No factory found for object type: "+changeMessage.getObjectType());
		}
		// The builder creates the email.
		BroadcastMessageBuilder builder = factory.createMessageBuilder(changeMessage.getObjectId(), changeMessage.getChangeType(), changeMessage.getUserId());
		Topic topic = builder.getBroadcastTopic();
		valdiateTopic(topic);
		// Get all of the email subscribers for this topic.
		List<Subscriber> subscribers = subscriptionDAO.getAllEmailSubscribers(topic.getObjectId(), topic.getObjectType());
		List<String> subscriberIds = new ArrayList<String>();
		// The builder will prepare an email for each subscriber
		for(Subscriber subscriber: subscribers){
			subscriberIds.add(subscriber.getSubscriberId());
			// do not send an email to the user who created this change
			if (subscriber.getSubscriberId().equals(changeMessage.getUserId().toString())) {
				continue;
			}
			if (emailQuarantineDao.isQuarantined(subscriber.getNotificationEmail())) {
				log.warn("Cannot send message to quarantined address: {}", subscriber.getNotificationEmail());
				continue;
			}
			SendRawEmailRequest emailRequest = builder.buildEmailForSubscriber(subscriber);
			log.debug("sending email to "+subscriber.getNotificationEmail());
			sesClient.sendRawEmail(emailRequest);
		}

		sendMessageToNonSubscribers(progressCallback, changeMessage, builder, subscriberIds, topic);
	}

	/*
	 * Send email notification to users who is not subscribed to the topic, but
	 * related to the topic and need to be notified (ex: mentioned users in a thread).
	 * An email is only being sent to a user if he/she has permission to subscribe
	 * to the topic.
	 */
	public void sendMessageToNonSubscribers(ProgressCallback progressCallback,
			ChangeMessage changeMessage, BroadcastMessageBuilder builder, List<String> subscriberIds,
			Topic topic)
			throws ClientProtocolException, JSONException, IOException, MarkdownClientException {
		Set<String> mentionedUserIds = builder.getRelatedUsers();
		if (mentionedUserIds == null || mentionedUserIds.isEmpty()) {
			return;
		}
		// remove mentioned users who subscribed to the topic
		mentionedUserIds.removeAll(subscriberIds);
		// create list of MentionedUser from their ids
		List<UserNotificationInfo> mentionedUsers = userProfileDao.getUserNotificationInfo(mentionedUserIds);
		// build and send email to each mentioned user
		for(UserNotificationInfo userNotificationInfo: mentionedUsers){
			// do not send an email to the user who created this change
			if (userNotificationInfo.getUserId().equals(changeMessage.getUserId().toString())) {
				continue;
			}
			if (emailQuarantineDao.isQuarantined(userNotificationInfo.getNotificationEmail())) {
				log.warn("Cannot send message to quarantined address: {}", userNotificationInfo.getNotificationEmail());
				continue;
			}
			UserInfo userInfo = userManager.getUserInfo(Long.parseLong(userNotificationInfo.getUserId()));
			if (authManager.canSubscribe(userInfo, topic.getObjectId(), topic.getObjectType()).isAuthorized()) {
				SendRawEmailRequest emailRequest = builder.buildEmailForNonSubscriber(userNotificationInfo);
				log.debug("sending email to "+userNotificationInfo.getNotificationEmail());
				sesClient.sendRawEmail(emailRequest);
			}
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
	public void setMessageBuilderFactoryMap(Map<ObjectType, MessageBuilderFactory> factoryMap) {
		this.messageBuilderFactoryMap = factoryMap;
	}
	
	
}
