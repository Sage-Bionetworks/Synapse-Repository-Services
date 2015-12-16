package org.sagebionetworks.discussion.workers;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadViewStat;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionThreadViewStatsWorker implements ProgressingRunner<Void>{

	@Autowired
	private DiscussionThreadDAO threadDao;

	private final Logger logger = LogManager.getLogger(DiscussionThreadViewStatsWorker.class);
	public static final Long LIMIT = 100L;

	@Override
	public void run(ProgressCallback<Void> progressCallback) throws Exception {
		logger.trace("Updating thread view stats");
		Long offset = 0L;
		List<DiscussionThreadViewStat> viewStats = threadDao.getThreadViewStat(LIMIT, offset);
		while (!viewStats.isEmpty()) {
			threadDao.updateThreadViewStat(viewStats);
			progressCallback.progressMade(null);
			offset += LIMIT;
			viewStats = threadDao.getThreadViewStat(LIMIT, offset);
		}
	}

}
