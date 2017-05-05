package org.sagebionetworks.table.worker;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.Lists;


public class EntityReplicationWorkerTest {
	
	@Mock
	NodeDAO mockNodeDao;
	@Mock
	ConnectionFactory mockConnectionFactory;
	@Mock
	TableIndexDAO mockIndexDao;
	@Mock
	TransactionStatus transactionStatus;
	@Mock
	ProgressCallback<Void> mockPogressCallback;
	@Mock
	WorkerLogger mockWorkerLog;
	
	EntityReplicationWorker worker;
	
	List<ChangeMessage> changes;

	@SuppressWarnings("unchecked")
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		worker = new EntityReplicationWorker();
		ReflectionTestUtils.setField(worker, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(worker, "connectionFactory", mockConnectionFactory);
		ReflectionTestUtils.setField(worker, "workerLogger", mockWorkerLog);
		
		ChangeMessage update = new ChangeMessage();
		update.setChangeType(ChangeType.UPDATE);
		update.setObjectType(ObjectType.ENTITY);
		update.setObjectId("111");
		ChangeMessage create = new ChangeMessage();
		create.setChangeType(ChangeType.CREATE);
		create.setObjectType(ObjectType.ENTITY);
		create.setObjectId("222");
		ChangeMessage delete = new ChangeMessage();
		delete.setChangeType(ChangeType.DELETE);
		delete.setObjectType(ObjectType.ENTITY);
		delete.setObjectId("333");
		changes = Lists.newArrayList(update, create, delete);
		
		when(mockConnectionFactory.getAllConnections()).thenReturn(Lists.newArrayList(mockIndexDao));
		
		doAnswer(new Answer<Void>(){

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				TransactionCallback callback = (TransactionCallback) invocation.getArguments()[0];
				callback.doInTransaction(transactionStatus);
				return null;
			}}).when(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
	}
	
	@Test
	public void testGroupByChangeType(){
		List<String> createOrUpdateIds = new LinkedList<>();
		List<String> deleteIds = new LinkedList<String>();
		EntityReplicationWorker.groupByChangeType(changes, createOrUpdateIds, deleteIds);
		List<String> expectedCreateOrUpdate = Lists.newArrayList("111","222");
		List<String> expectedDelete = Lists.newArrayList("333");
		assertEquals(expectedCreateOrUpdate, createOrUpdateIds);
		assertEquals(expectedDelete, deleteIds);
	}
	
	@Test
	public void testRun() throws RecoverableMessageException, Exception{
		List<EntityDTO> entityData = new LinkedList<EntityDTO>();
		when(mockNodeDao.getEntityDTOs(anyListOf(String.class), anyInt())).thenReturn(entityData);
		
		// call under test
		worker.run(mockPogressCallback, changes);
		verify(mockNodeDao).getEntityDTOs(Lists.newArrayList("111", "222"), EntityReplicationWorker.MAX_ANNOTATION_CHARS);
		verify(mockIndexDao).createEntityReplicationTablesIfDoesNotExist();
		verify(mockIndexDao).deleteEntityData(any(ProgressCallback.class) ,eq(Lists.newArrayList(111L,222L,333L)));
		verify(mockIndexDao).addEntityData(any(ProgressCallback.class) ,eq(entityData));
		verify(mockPogressCallback, times(2)).progressMade(null);
		verifyZeroInteractions(mockWorkerLog);
	}
	
	@Test
	public void testLogError() throws RecoverableMessageException, Exception{
		RuntimeException exception = new RuntimeException("something went wrong");
		// setup an exception
		doThrow(exception).when(mockIndexDao).addEntityData(any(ProgressCallback.class), anyListOf(EntityDTO.class));
		// call under test
		worker.run(mockPogressCallback, changes);
		boolean willRetry = false;
		// the exception should be logged.
		verify(mockWorkerLog).logWorkerFailure(EntityReplicationWorker.class.getName(), exception, willRetry);
	}

}
