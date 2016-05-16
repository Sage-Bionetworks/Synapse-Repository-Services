package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class ThreadMessageBuilderFactory implements MessageBuilderFactory {
	public static final String THREAD_TEMPLATE = "message/threadTemplate.txt";
	public static final String THREAD_CREATED_TITLE = "Synapse Notification: New thread '%1$s'";

	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	@Autowired
	private UploadContentToS3DAO uploadDao;
	@Autowired
	private MarkdownDao markdownDao;

	@Override
	public BroadcastMessageBuilder createMessageBuilder(String objectId,
			ChangeType changeType, Long userId) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(changeType, "changeType");
		Long threadId = Long.parseLong(objectId);
		// Lookup the thread
		DiscussionThreadBundle threadBundle = threadDao.getThread(threadId, DiscussionFilter.NO_FILTER);
		// Lookup the project
		String projectName = nodeDao.getEntityHeader(threadBundle.getProjectId(), null).getName();
		// Lookup the user name of the actor
		String actor = principalAliasDAO.getUserName(userId);
		String markdown = null;
		markdown = uploadDao.getMessage(threadBundle.getMessageKey());
		return new DiscussionBroadcastMessageBuilder(actor, userId.toString(), threadBundle.getTitle(),
				threadBundle.getId(), threadBundle.getProjectId(), projectName, markdown,
				THREAD_TEMPLATE, THREAD_CREATED_TITLE, markdownDao);
	}

}
