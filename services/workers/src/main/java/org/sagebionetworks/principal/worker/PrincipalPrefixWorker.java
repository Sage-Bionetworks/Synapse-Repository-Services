package org.sagebionetworks.principal.worker;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.Worker;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class PrincipalPrefixWorker implements Worker {
	
	static private Logger log = LogManager.getLogger(PrincipalPrefixWorker.class);
	
	@Autowired
	PrincipalPrefixDAO principalPrefixDao;
	@Autowired
	private UserProfileDAO userProfileDAO;
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	@Autowired
	UserGroupDAO userGroupDao;

	List<Message> messages;
	WorkerProgress workerProgress;
	
	@Override
	public List<Message> call() throws Exception {
		List<Message> toDelete = new LinkedList<Message>();
		for(Message message: messages){
			try{
				Message returned = process(message);
				if(returned != null){
					toDelete.add(returned);
				}
			} catch(Throwable e) {
				log.error("Worker Failed", e);
			}
		}
		return toDelete;
	}

	private Message process(Message message) throws Throwable {
		// Keep this message invisible
		workerProgress.progressMadeForMessage(message);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		if (changeMessage.getObjectType() != ObjectType.PRINCIPAL) {
			// do nothing when receive a non-principal message
			return message;
		}
		// Get the user's profile for their first and last name
		Long principalId = Long.parseLong(changeMessage.getObjectId());

		// Clear any tokens for this principal
		principalPrefixDao.clearPrincipal(principalId);
		// Is this a user or group?
		UserGroup userGroup = null;
		try {
			userGroup = userGroupDao.get(principalId);
		} catch (NotFoundException e1) {
			log.warn("Principal not found: "+principalId);
			return message;
		}
		if(userGroup.getIsIndividual()){
			// User
			UserProfile profile = userProfileDAO.get(changeMessage.getObjectId());
			// Bind the first and last name.
			principalPrefixDao.addPrincipalName(profile.getFirstName(), profile.getLastName(), principalId);
			try {
				String userName = principalAliasDAO.getUserName(principalId);
				principalPrefixDao.addPrincipalAlias(userName, principalId);
			} catch (Exception e) {
				log.warn("Did not find a username for principalId = "+principalId);
			}
		}else{
			// Team
			try {
				List<PrincipalAlias> names = principalAliasDAO.listPrincipalAliases(principalId, AliasType.TEAM_NAME);
				for(PrincipalAlias alias: names){
					principalPrefixDao.addPrincipalAlias(alias.getAlias(), principalId);
				}
			} catch (Exception e) {
				log.warn("Did not find team names for principalId = "+principalId);
			}
		}	
		return message;
	}
	
	@Override
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	@Override
	public void setWorkerProgress(WorkerProgress workerProgress) {
		this.workerProgress = workerProgress;
	}

}
