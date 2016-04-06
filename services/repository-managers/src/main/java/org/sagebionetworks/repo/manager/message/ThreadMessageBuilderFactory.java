package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class ThreadMessageBuilderFactory implements MessageBuilderFactory {

	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;

	@Override
	public BroadcastMessageBuilder createMessageBuilder(String objectId,
			ChangeType changeType, Long userId) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(changeType, "changeType");
		Long threadId = Long.parseLong(objectId);
		// Lookup the thread
		DiscussionThreadBundle threadBundle = threadDao.getThread(threadId, DiscussionFilter.NO_FILTER);
		// Lookup the project
		EntityHeader projectHeader = nodeDao.getEntityHeader(threadBundle.getProjectId(), null);
		// Lookup the user name of the actor
		String actor = principalAliasDAO.getUserName(userId);
		return new ThreadBroadcastMessageBuilder(threadBundle, projectHeader, changeType, actor);
	}

}
