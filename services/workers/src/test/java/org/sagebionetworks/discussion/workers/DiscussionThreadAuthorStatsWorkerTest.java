package org.sagebionetworks.discussion.workers;

import static org.sagebionetworks.discussion.workers.DiscussionThreadAuthorStatsWorker.LIMIT;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadAuthorStat;
import org.springframework.test.util.ReflectionTestUtils;

public class DiscussionThreadAuthorStatsWorkerTest {

	@Mock
	private DiscussionThreadDAO mockThreadDao;
	@Mock
	private DiscussionReplyDAO mockReplyDao;
	@Mock
	private ProgressCallback<Void> mockCallback;

	private DiscussionThreadAuthorStatsWorker worker;


	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);

		worker = new DiscussionThreadAuthorStatsWorker();
		ReflectionTestUtils.setField(worker, "threadDao", mockThreadDao);
		ReflectionTestUtils.setField(worker, "replyDao", mockReplyDao);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNoThreads() throws Exception {
		Mockito.when(mockThreadDao.getAllThreadId(Mockito.anyLong(), Mockito.anyLong()))
				.thenReturn(new ArrayList<Long>());
		worker.run(mockCallback);
		Mockito.verify(mockThreadDao).getAllThreadId(LIMIT, 0L);
		Mockito.verify(mockThreadDao, Mockito.never()).getAllThreadId(LIMIT, LIMIT);
		Mockito.verify(mockReplyDao, Mockito.never()).getDiscussionThreadAuthorStat(Mockito.anyLong());
		Mockito.verify(mockThreadDao, Mockito.never()).updateThreadAuthorStat(Mockito.anyList());
		Mockito.verify(mockCallback, Mockito.never()).progressMade(null);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNoThreadsHaveBeenReplied() throws Exception {
		Long threadId = 1L;
		Mockito.when(mockThreadDao.getAllThreadId(Mockito.anyLong(), Mockito.anyLong()))
				.thenReturn(Arrays.asList(threadId), new ArrayList<Long>());
		DiscussionThreadAuthorStat stat = new DiscussionThreadAuthorStat();
		stat.setThreadId(threadId);
		stat.setActiveAuthors(new ArrayList<String>());
		Mockito.when(mockReplyDao.getDiscussionThreadAuthorStat(threadId)).thenReturn(stat);
		worker.run(mockCallback);
		Mockito.verify(mockThreadDao).getAllThreadId(LIMIT, 0L);
		Mockito.verify(mockThreadDao).getAllThreadId(LIMIT, LIMIT);
		Mockito.verify(mockThreadDao, Mockito.never()).getAllThreadId(LIMIT, 2*LIMIT);
		Mockito.verify(mockReplyDao).getDiscussionThreadAuthorStat(threadId);
		Mockito.verify(mockThreadDao, Mockito.never()).updateThreadAuthorStat(Mockito.anyList());
		Mockito.verify(mockCallback).progressMade(null);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSomeThreadsHaveBeenReplies() throws Exception {
		Long threadId = 1L;
		Mockito.when(mockThreadDao.getAllThreadId(Mockito.anyLong(), Mockito.anyLong()))
				.thenReturn(Arrays.asList(threadId), new ArrayList<Long>());
		DiscussionThreadAuthorStat stat = new DiscussionThreadAuthorStat();
		stat.setThreadId(threadId);
		stat.setActiveAuthors(Arrays.asList("2", "3"));
		Mockito.when(mockReplyDao.getDiscussionThreadAuthorStat(threadId)).thenReturn(stat);
		worker.run(mockCallback);
		Mockito.verify(mockThreadDao).getAllThreadId(LIMIT, 0L);
		Mockito.verify(mockThreadDao).getAllThreadId(LIMIT, LIMIT);
		Mockito.verify(mockThreadDao, Mockito.never()).getAllThreadId(LIMIT, 2*LIMIT);
		Mockito.verify(mockReplyDao).getDiscussionThreadAuthorStat(threadId);
		Mockito.verify(mockThreadDao).updateThreadAuthorStat(Mockito.eq(Arrays.asList(stat)));
		Mockito.verify(mockCallback).progressMade(null);
	}
}
