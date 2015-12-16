package org.sagebionetworks.discussion.workers;

import static org.sagebionetworks.discussion.workers.DiscussionThreadReplyStatsWorker.LIMIT;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadReplyStat;
import org.springframework.test.util.ReflectionTestUtils;

public class DiscussionThreadReplyStatsWorkerTest {

	@Mock
	private DiscussionThreadDAO mockThreadDao;
	@Mock
	private DiscussionReplyDAO mockReplyDao;
	@Mock
	private ProgressCallback<Void> mockCallback;

	private DiscussionThreadReplyStatsWorker worker;


	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);

		worker = new DiscussionThreadReplyStatsWorker();
		ReflectionTestUtils.setField(worker, "threadDao", mockThreadDao);
		ReflectionTestUtils.setField(worker, "replyDao", mockReplyDao);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNoThreadsHaveBeenViewed() throws Exception {
		Mockito.when(mockReplyDao.getThreadReplyStat(Mockito.anyLong(), Mockito.anyLong()))
				.thenReturn(new ArrayList<DiscussionThreadReplyStat>());
		worker.run(mockCallback);
		Mockito.verify(mockReplyDao).getThreadReplyStat(LIMIT, 0L);
		Mockito.verify(mockReplyDao, Mockito.never()).getThreadReplyStat(LIMIT, LIMIT);
		Mockito.verify(mockThreadDao, Mockito.never()).updateThreadReplyStat(Mockito.anyList());
		Mockito.verify(mockCallback, Mockito.never()).progressMade(null);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSomeThreadsHaveBeenViewed() throws Exception {
		DiscussionThreadReplyStat stat = new DiscussionThreadReplyStat();
		List<DiscussionThreadReplyStat> stats = new ArrayList<DiscussionThreadReplyStat>();
		stats.add(stat);
		Mockito.when(mockReplyDao.getThreadReplyStat(Mockito.anyLong(), Mockito.anyLong()))
				.thenReturn(stats, new ArrayList<DiscussionThreadReplyStat>());
		worker.run(mockCallback);
		Mockito.verify(mockReplyDao).getThreadReplyStat(LIMIT, 0L);
		Mockito.verify(mockReplyDao).getThreadReplyStat(LIMIT, LIMIT);
		Mockito.verify(mockReplyDao, Mockito.never()).getThreadReplyStat(LIMIT, 2*LIMIT);
		Mockito.verify(mockThreadDao).updateThreadReplyStat(stats);
		Mockito.verify(mockCallback).progressMade(null);
	}
}
