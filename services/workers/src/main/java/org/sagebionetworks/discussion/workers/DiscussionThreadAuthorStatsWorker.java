package org.sagebionetworks.discussion.workers;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadAuthorStat;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionThreadAuthorStatsWorker implements ProgressingRunner<Void>{

	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private DiscussionReplyDAO replyDao;

	private final Logger logger = LogManager.getLogger(DiscussionThreadAuthorStatsWorker.class);
	public static final Long LIMIT = 100L;

	@Override
	public void run(ProgressCallback<Void> progressCallback) throws Exception {
		logger.trace("Updating thread reply stats");
		Long offset = 0L;
		List<Long> threadIds = threadDao.getAllThreadId(LIMIT, offset);
		while (!threadIds.isEmpty()) {
			List<DiscussionThreadAuthorStat> toUpdate = new ArrayList<DiscussionThreadAuthorStat>();
			for (Long threadId : threadIds) {
				DiscussionThreadAuthorStat stat = replyDao.getDiscussionThreadAuthorStat(threadId);
				if (stat != null && !stat.getActiveAuthors().isEmpty()) {
					toUpdate.add(stat);
				}
			}
			if (!toUpdate.isEmpty()) {
				threadDao.updateThreadAuthorStat(toUpdate);
			}
			offset += LIMIT;
			threadIds = threadDao.getAllThreadId(LIMIT, offset);
		}
	}

}
