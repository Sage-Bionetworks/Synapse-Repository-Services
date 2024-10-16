package org.sagebionetworks.discussion.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dbo.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadReplyStat;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadStat;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class DiscussionThreadStatsWorkerUnitTest {

	@Mock
	private DiscussionThreadDAO mockThreadDao;
	@Mock
	private DiscussionReplyDAO mockReplyDao;
	@Mock
	private ChangeMessage mockMessage;
	@Mock
	private ProgressCallback mockCallback;
	@InjectMocks
	private DiscussionThreadStatsWorker worker;

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
