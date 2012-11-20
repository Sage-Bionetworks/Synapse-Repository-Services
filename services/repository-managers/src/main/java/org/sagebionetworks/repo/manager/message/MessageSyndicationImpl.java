package org.sagebionetworks.repo.manager.message;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Basic implementation of the message syndication.
 * 
 * @author jmhill
 *
 */
public class MessageSyndicationImpl implements MessageSyndication {
	
	static private Log log = LogFactory.getLog(RepositoryMessagePublisherImpl.class);
	
	@Autowired
	RepositoryMessagePublisher messagePublisher;
	
	@Autowired
	DBOChangeDAO changeDAO;
	
	@Override
	public void rebroadcastAllChangeMessages() {
		// List all change messages
		List<ChangeMessage> list = null;
		long lastNumber = 0;
		do{
			list = changeDAO.listChanges(lastNumber, ObjectType.ENTITY, 100);
			log.info("Sending "+list.size()+" change messages to the topic");
			if(list.size() > 0){
				log.info("First change number on the list: "+list.get(0).getChangeNumber());
			}
			// Add each message
			for(ChangeMessage change: list){
				messagePublisher.fireChangeMessage(change);
				lastNumber = change.getChangeNumber()+1;
			}
		}while(list.size() > 0);
		
	}

	/**
	 * This can be called to run against a give stack by passing all the stack information.
	 * @param args
	 */
	public static void main(String[] args){
		// Create the spring context
		ClassPathXmlApplicationContext beanContext = new ClassPathXmlApplicationContext("classpath:managers-spb.xml");
		
		// Get the MessageSyndication bean
		MessageSyndication messageSyndication = beanContext.getBean(MessageSyndication.class);
		// Rebroadcast all messages
		messageSyndication.rebroadcastAllChangeMessages();
	}
}
