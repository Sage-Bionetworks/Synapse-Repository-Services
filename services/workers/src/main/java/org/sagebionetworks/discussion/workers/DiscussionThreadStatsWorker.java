package org.sagebionetworks.discussion.workers;

import java.util.Arrays;

import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadReplyStat;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadStat;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

public class DiscussionThreadStatsWorker implements ChangeMessageDrivenRunner{
	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private DiscussionReplyDAO replyDao;

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message)
			throws RecoverableMessageException {
		if (message.getChangeType() != ChangeType.UPDATE) {
			// only process update events
			return;
		}
		long threadId = Long.parseLong(message.getObjectId());

		DiscussionThreadStat stat = new DiscussionThreadStat();
		stat.setThreadId(threadId);
		stat.setActiveAuthors(replyDao.getActiveAuthors(threadId));

		DiscussionThreadReplyStat replyStat = replyDao.getThreadReplyStat(threadId);
		stat.setLastActivity(replyStat.getLastActivity());
		stat.setNumberOfReplies(replyStat.getNumberOfReplies());

		stat.setNumberOfViews(threadDao.countThreadView(threadId));
		try {
			threadDao.updateThreadStats(Arrays.asList(stat));
		} catch (DataIntegrityViolationException e) {
			// the thread no longer exist, do nothing
		}
	}

}
