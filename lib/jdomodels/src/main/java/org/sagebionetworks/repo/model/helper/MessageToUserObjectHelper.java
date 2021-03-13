package org.sagebionetworks.repo.model.helper;

import java.util.Collections;
import java.util.function.Consumer;

import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageToUserObjectHelper implements DaoObjectHelper<MessageToUser> {

	private MessageDAO messageDao;
	
	@Autowired
	public MessageToUserObjectHelper(MessageDAO messageDao) {
		this.messageDao = messageDao;
	}
	
	@Override
	public MessageToUser create(Consumer<MessageToUser> consumer) {
		MessageToUser message = new MessageToUser();
		
		message.setCreatedBy("123");
		message.setFileHandleId("456");
		message.setSubject("Subject");
		message.setRecipients(Collections.singleton("789"));	
		
		consumer.accept(message);

		return messageDao.createMessage(message, false);
	}

}
