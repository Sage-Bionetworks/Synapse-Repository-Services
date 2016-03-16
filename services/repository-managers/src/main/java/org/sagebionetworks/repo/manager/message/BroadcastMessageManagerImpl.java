package org.sagebionetworks.repo.manager.message;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.BroadcastMessageDao;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.util.TimeoutUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class BroadcastMessageManagerImpl implements BroadcastMessageManager {
	
	static private Logger log = LogManager.getLogger(BroadcastMessageManagerImpl.class);
	
	/**
	 * Ignore any message older than 24 hours.
	 */
	public static final long MESSAGE_EXPIRATION_MS = 1000*60*60*24; // 24 hours
	
	// Maps ObjectTypes to builders. Injected (IoC).
	Map<ObjectType, BroadcastMessageBuilder> builderMap;

	@Autowired
	SubscriptionDAO subscriptionDAO;
	@Autowired
	BroadcastMessageDao broadcastMessageDao;
	@Autowired
	DBOChangeDAO changeDao;
	@Autowired
	TimeoutUtils timeoutUtils;
	@Autowired
	NotificationManager notificationManager;

	@WriteTransactionReadCommitted
	@Override
	public void broadcastMessage(UserInfo user,	ChangeMessage changeMessage) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(changeMessage, "changeMessage");
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
		// Lookup the builder for this type.
		BroadcastMessageBuilder builder = builderMap.get(changeMessage.getObjectType());
		if(builder == null){
			throw new IllegalArgumentException("No builder found for object type: "+changeMessage.getObjectType());
		}
		// The builder creates the email.
		BroadcastMessage message = builder.buildMessage(changeMessage.getObjectId(), changeMessage.getChangeType());
		validateBroadcastMessage(message);
		// Get the uses subscribed to this topic
		List<String> subscribers = subscriptionDAO.getAllSubscribers(message.getTopic().getObjectId(), message.getTopic().getObjectType());
		// Put it all together and send it out
		MessageToUser messageToUser = new MessageToUser();
		messageToUser.setRecipients(new HashSet<String>(subscribers));
		messageToUser.setSubject(message.getSubject());
		MessageToUserAndBody messageAndBody = new MessageToUserAndBody();
		messageAndBody.setBody(message.getBody());
		messageAndBody.setMimeType(message.getContentType().getMimeType());
		// Send it out
		messageToUser = notificationManager.sendNotification(user, messageAndBody);
		// Record this message as sent
		broadcastMessageDao.setBroadcast(changeMessage.getChangeNumber(), Long.parseLong(messageToUser.getId()));
	}

	/**
	 * IoC.
	 * @param builderMap
	 */
	public void setBuilderMap(Map<ObjectType, BroadcastMessageBuilder> builderMap) {
		this.builderMap = builderMap;
	}
	
	/**
	 * Validate the given BroadcastMessage message.
	 * @param message
	 */
	public void validateBroadcastMessage(BroadcastMessage message){
		ValidateArgument.required(message, "message");
		ValidateArgument.required(message.getTopic(), "message.Topic");
		ValidateArgument.required(message.getTopic().getObjectId(), "message.Topic.ObjectId");
		ValidateArgument.required(message.getTopic().getObjectType(), "message.Topic.ObjectType");
		ValidateArgument.required(message.getBody(), "message.Body");
		ValidateArgument.required(message.getSubject(), "message.Subject");
		ValidateArgument.required(message.getContentType(), "message.ContentType");
	}
	
	
}
