package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.message.Comment;

/**
 * TODO Other than *some* of the DB representation, this DAO has not been designed yet
 */
public interface CommentDAO {
	
	/**
	 * Saves the message information so that it can be processed by a worker
	 */
	public Comment createComment(Comment dto);
	
}
