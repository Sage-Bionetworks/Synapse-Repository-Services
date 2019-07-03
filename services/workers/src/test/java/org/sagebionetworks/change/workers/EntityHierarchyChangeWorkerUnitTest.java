package org.sagebionetworks.change.workers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySetOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeIdAndType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.worker.entity.EntityHierarchyChangeWorker;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class EntityHierarchyChangeWorkerUnitTest {

	@Mock
	DBOChangeDAO mockChangeDao;
	@Mock
	NodeDAO mockNodeDao;
	@Mock
	RepositoryMessagePublisher mockMessagePublisher;
	@Mock
	Clock mockClock;
	@Mock
	ProgressCallback mockProgressCallback;
	@Captor 
	private ArgumentCaptor<List<ChangeMessage>> publishCapture;
	
	EntityHierarchyChangeWorker worker;
	ChangeMessage message;
	String parentId;
	String folderId;
	List<NodeIdAndType> filesOnly;
	List<NodeIdAndType> empty;
	List<NodeIdAndType> filesAndFolders;
	List<ChangeMessage> chagnes;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		worker = new EntityHierarchyChangeWorker();
		ReflectionTestUtils.setField(worker, "changeDao", mockChangeDao);
		ReflectionTestUtils.setField(worker, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(worker, "messagePublisher", mockMessagePublisher);
		ReflectionTestUtils.setField(worker, "clock", mockClock);
		
		message = new ChangeMessage();
		message.setTimestamp(new Date(1));
		message.setChangeType(ChangeType.UPDATE);
		message.setObjectId("syn123");
		
		when(mockClock.currentTimeMillis()).thenReturn(1L, 2L,3L,4L,5L);
		
		parentId = "syn123";
		folderId = "syn444";
		
		filesOnly = Lists.newArrayList(
				new NodeIdAndType("syn111", EntityType.file),
				new NodeIdAndType("syn222", EntityType.file)
		);
		empty = new LinkedList<>();
		filesAndFolders = Lists.newArrayList(
				new NodeIdAndType("syn333", EntityType.file),
				new NodeIdAndType(folderId, EntityType.folder)
		);
		
		ChangeMessage childChange = new ChangeMessage();
		childChange.setObjectId("syn123");
	}
	
	@Test
	public void testOldMessage() throws RecoverableMessageException, Exception{
		// set the time past the first message
		when(mockClock.currentTimeMillis()).thenReturn(
				EntityHierarchyChangeWorker.MAX_MESSAGE_AGE_MS
						+ message.getTimestamp().getTime() + 1);
		// call under test
		worker.run(mockProgressCallback, message);
		// the message should be ignored.
		verifyNoMoreInteractions(
				mockProgressCallback
				,mockChangeDao
				,mockNodeDao
				,mockMessagePublisher
				);
	}
	
	@Test
	public void testNewMessage() throws RecoverableMessageException, Exception{
		// call under test
		worker.run(mockProgressCallback, message);
		verify(mockNodeDao).getChildren(eq(message.getObjectId()), anyLong(), anyLong());
	}
	
	@Test
	public void testRecursiveBroadcastMessagesWithoutRecursion() throws InterruptedException{
		// setup only files in this container.
		when(mockNodeDao.getChildren(anyString(), anyLong(), anyLong())).thenReturn(filesOnly, empty);
		// call under test
		worker.recursiveBroadcastMessages(mockProgressCallback, parentId, ChangeType.CREATE);
		verify(mockNodeDao, times(2)).getChildren(anyString(), anyLong(), anyLong());
		verify(mockChangeDao).getChangesForObjectIds(ObjectType.ENTITY, Sets.newHashSet(111L,222L));
		verify(mockMessagePublisher).publishBatchToTopic(any(ObjectType.class), anyListOf(ChangeMessage.class));
		verify(mockClock, times(1)).sleep(anyLong());
	}
	
	@Test
	public void testRecursiveBroadcastMessagesWithRecursion() throws InterruptedException{
		// setup files and folders for children
		when(mockNodeDao.getChildren(anyString(), anyLong(), anyLong())).thenReturn(filesAndFolders,empty, filesOnly, empty);
		// call under test
		worker.recursiveBroadcastMessages(mockProgressCallback, parentId, ChangeType.CREATE);
		// should be called twice with the original parent
		verify(mockNodeDao, times(2)).getChildren(eq(parentId), anyLong(), anyLong());
		// should be called twice for the child folder
		verify(mockNodeDao, times(2)).getChildren(eq(folderId), anyLong(), anyLong());
		verify(mockChangeDao).getChangesForObjectIds(ObjectType.ENTITY, Sets.newHashSet(111L,222L));
		verify(mockMessagePublisher, times(2)).publishBatchToTopic(any(ObjectType.class), anyListOf(ChangeMessage.class));
		verify(mockClock, times(2)).sleep(anyLong());
	}
	
	@Test
	public void testRecursiveBroadcastMessagesMultiplePages() throws InterruptedException{
		// setup multiple pages of files
		when(mockNodeDao.getChildren(anyString(), anyLong(), anyLong())).thenReturn(filesOnly, filesOnly, empty);
		// call under test
		worker.recursiveBroadcastMessages(mockProgressCallback, parentId, ChangeType.CREATE);
		// should be called three time with the original parent
		verify(mockNodeDao, times(3)).getChildren(eq(parentId), anyLong(), anyLong());
		verify(mockChangeDao, times(2)).getChangesForObjectIds(ObjectType.ENTITY, Sets.newHashSet(111L,222L));
		verify(mockMessagePublisher, times(2)).publishBatchToTopic(any(ObjectType.class), anyListOf(ChangeMessage.class));
		verify(mockClock, times(2)).sleep(anyLong());
	}
	
	/**
	 * Test for PLFM-1723.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testRecursiveBroadcastMessagesChangeType() throws InterruptedException{
		ChangeMessage currentMessage = new ChangeMessage();
		currentMessage.setChangeType(ChangeType.UPDATE);
		when(mockChangeDao.getChangesForObjectIds(any(ObjectType.class), anySetOf(Long.class))).thenReturn(Lists.newArrayList(currentMessage));
		
		// setup multiple pages of files
		when(mockNodeDao.getChildren(anyString(), anyLong(), anyLong())).thenReturn(filesOnly, empty);
		// call under test
		worker.recursiveBroadcastMessages(mockProgressCallback, parentId, ChangeType.DELETE);

		verify(mockMessagePublisher, times(1)).publishBatchToTopic(any(ObjectType.class), publishCapture.capture());
		List<ChangeMessage> published = publishCapture.getValue();
		assertNotNull(published);
		assertEquals(1, published.size());
		ChangeMessage publishedMessage = published.get(0);
		// the original message was a create but the pushed message should be a delete.
		assertEquals(ChangeType.DELETE, publishedMessage.getChangeType());
	}
	
}
