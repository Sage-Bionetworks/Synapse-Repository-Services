package org.sagebionetworks.repo.model.dbo.dao;

import java.util.Date;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.CommentDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOComment;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageContent;
import org.sagebionetworks.repo.model.message.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.sagebionetworks.repo.transactions.WriteTransaction;


public class DBOCommentDAOImpl implements CommentDAO {
	
	@Autowired
	private DBOBasicDao basicDAO;
	
	@Autowired
	private IdGenerator idGenerator;

	@Override
	@WriteTransaction
	public Comment createComment(Comment dto) {
		DBOMessageContent content = new DBOMessageContent();
		DBOComment info = new DBOComment();
		MessageUtils.copyDTOtoDBO(dto, content, info);
		
		// Generate an ID for all the new rows
		Long messageId = idGenerator.generateNewId(IdType.MESSAGE_ID);
		
		// Insert the message content
		content.setMessageId(messageId);
		content.setCreatedOn(new Date().getTime());
		content.setEtag(UUID.randomUUID().toString());
		MessageUtils.validateDBO(content);
		basicDAO.createNew(content);
		
		// Insert the comment info
		info.setMessageId(messageId);
		MessageUtils.validateDBO(info);
		basicDAO.createNew(info);
		
		dto = new Comment();
		MessageUtils.copyDBOToDTO(content, info, dto);
		return dto;
	}
}
