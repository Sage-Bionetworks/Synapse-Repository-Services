package org.sagebionetworks.discussion.workers;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadReplyStat;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionThreadReplyStatsWorker implements ProgressingRunner<Void>{

	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private DiscussionReplyDAO replyDao;

	private final Logger logger = LogManager.getLogger(DiscussionThreadReplyStatsWorker.class);
	public static final Long LIMIT = 100L;

	@Override
	public void run(ProgressCallback<Void> progressCallback) throws Exception {
		logger.trace("Updating thread reply stats");
		Long offset = 0L;
		List<DiscussionThreadReplyStat> replyStats = replyDao.getThreadReplyStat(LIMIT, offset);
		while (!replyStats.isEmpty()) {
			threadDao.updateThreadReplyStat(replyStats);
			offset += LIMIT;
			replyStats = replyDao.getThreadReplyStat(LIMIT, offset);
		}
	}

}
