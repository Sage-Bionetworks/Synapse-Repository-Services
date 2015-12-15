package org.sagebionetworks.discussion.workers;

import static org.sagebionetworks.discussion.workers.DiscussionThreadViewStatsWorker.LIMIT;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadViewStat;
import org.springframework.test.util.ReflectionTestUtils;

public class DiscussionThreadViewStatsWorkerTest {

	@Mock
	private DiscussionThreadDAO mockThreadDao;
	@Mock
	private ProgressCallback<Void> mockCallback;

	private DiscussionThreadViewStatsWorker worker;


	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);

		worker = new DiscussionThreadViewStatsWorker();
		ReflectionTestUtils.setField(worker, "threadDao", mockThreadDao);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNoThreadsHaveBeenViewed() throws Exception {
		Mockito.when(mockThreadDao.getThreadViewStat(Mockito.anyLong(), Mockito.anyLong()))
				.thenReturn(new ArrayList<DiscussionThreadViewStat>());
		worker.run(mockCallback);
		Mockito.verify(mockThreadDao).getThreadViewStat(LIMIT, 0L);
		Mockito.verify(mockThreadDao, Mockito.never()).getThreadViewStat(LIMIT, LIMIT);
		Mockito.verify(mockThreadDao, Mockito.never()).updateThreadViewStat(Mockito.anyList());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSomeThreadsHaveBeenViewed() throws Exception {
		DiscussionThreadViewStat stat = new DiscussionThreadViewStat();
		List<DiscussionThreadViewStat> stats = new ArrayList<DiscussionThreadViewStat>();
		stats.add(stat);
		Mockito.when(mockThreadDao.getThreadViewStat(Mockito.anyLong(), Mockito.anyLong()))
				.thenReturn(stats, new ArrayList<DiscussionThreadViewStat>());
		worker.run(mockCallback);
		Mockito.verify(mockThreadDao).getThreadViewStat(LIMIT, 0L);
		Mockito.verify(mockThreadDao).getThreadViewStat(LIMIT, LIMIT);
		Mockito.verify(mockThreadDao, Mockito.never()).getThreadViewStat(LIMIT, 2*LIMIT);
		Mockito.verify(mockThreadDao).updateThreadViewStat(stats);
	}
}
