package org.sagebionetworks.discussion.workers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadReplyStat;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadStat;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.test.util.ReflectionTestUtils;

public class DiscussionThreadStatsWorkerUnitTest {

	@Mock
	private DiscussionThreadDAO mockThreadDao;
	@Mock
	private DiscussionReplyDAO mockReplyDao;
	@Mock
	private ChangeMessage mockMessage;
	@Mock
	private ProgressCallback mockCallback;
	private DiscussionThreadStatsWorker worker;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		worker = new DiscussionThreadStatsWorker();
		ReflectionTestUtils.setField(worker, "threadDao", mockThreadDao);
		ReflectionTestUtils.setField(worker, "replyDao", mockReplyDao);
	}

	@Test
	public void testNotUpdateMessage() throws RecoverableMessageException {
		when(mockMessage.getChangeType()).thenReturn(ChangeType.CREATE);
		worker.run(mockCallback, mockMessage);
		verifyZeroInteractions(mockReplyDao);
		verifyZeroInteractions(mockThreadDao);
	}

	@Test
	public void testUpdateMessage() throws RecoverableMessageException {
		Long threadId = 1L;
		DiscussionThreadReplyStat replyStat = new DiscussionThreadReplyStat();
		Long lastActivity = System.currentTimeMillis();
		replyStat.setLastActivity(lastActivity);
		replyStat.setNumberOfReplies(2L);
		when(mockMessage.getObjectId()).thenReturn(threadId.toString());
		when(mockMessage.getChangeType()).thenReturn(ChangeType.UPDATE);
		when(mockReplyDao.getThreadReplyStat(threadId)).thenReturn(replyStat);
		when(mockThreadDao.countThreadView(threadId)).thenReturn(3L);
		when(mockReplyDao.getActiveAuthors(threadId)).thenReturn(null);
		worker.run(mockCallback, mockMessage);
		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
		verify(mockThreadDao).updateThreadStats(captor.capture());
		List value = captor.getValue();
		assertNotNull(value);
		assertEquals(1, value.size());
		DiscussionThreadStat stat = (DiscussionThreadStat) value.get(0);
		assertEquals(threadId, stat.getThreadId());
		assertEquals((Long)2L, stat.getNumberOfReplies());
		assertEquals(lastActivity, stat.getLastActivity());
		assertEquals((Long)3L, stat.getNumberOfViews());
		assertNull(stat.getActiveAuthors());
	}
}
