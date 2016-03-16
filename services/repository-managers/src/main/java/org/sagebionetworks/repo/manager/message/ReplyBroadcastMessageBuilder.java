package org.sagebionetworks.repo.manager.message;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.entity.ContentType;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Message builder for discussion replies.
 * 
 *
 */
public class ReplyBroadcastMessageBuilder implements BroadcastMessageBuilder {
	
	
	public static final String THREAD_REPLY_TEMPLATE = "message/threadReplyTemplate.html";
	
	public static String SUBJECT_TEMPLATE = "Synapse Notification: New reply in %1$s thread ";
	
	@Autowired
	private DiscussionReplyDAO replyDao;
	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private ForumDAO forumDAO;

	@Override
	public BroadcastMessage buildMessage(String objectId, ChangeType changeType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(changeType, "changeType");
		Long replyId = Long.parseLong(objectId);
		// Lookup the reply
		DiscussionReplyBundle replyBundle = replyDao.getReply(replyId, DiscussionFilter.NO_FILTER);
		// Lookup the thread
		DiscussionThreadBundle threadBundle = threadDao.getThread(Long.parseLong(replyBundle.getThreadId()), DiscussionFilter.NO_FILTER);
		// Lookup the forum
		Forum forum = forumDAO.getForum(Long.parseLong(threadBundle.getForumId()));
		
		// the topic for a reply is the thread
		Topic topic = new Topic();
		topic.setObjectId(threadBundle.getId());
		topic.setObjectType(SubscriptionObjectType.DISCUSSION_THREAD);
		
		BroadcastMessage message = new BroadcastMessage();
		message.setTopic(topic);
		message.setSubject(String.format(SUBJECT_TEMPLATE, threadBundle.getTitle()));
		message.setContentType(ContentType.TEXT_HTML);
		
		Map<String,String> fieldValues = new HashMap<String,String>();
		
		
		String body = EmailUtils.readMailTemplate(THREAD_REPLY_TEMPLATE, fieldValues);
		
		return message;
	}

}
