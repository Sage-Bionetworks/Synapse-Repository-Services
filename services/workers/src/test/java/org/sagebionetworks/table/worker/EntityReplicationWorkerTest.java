package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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
import org.sagebionetworks.database.semaphore.LockReleaseFailedException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.worker.entity.EntityReplicationWorker;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.model.AmazonSQSException;
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
	ProgressCallback mockPogressCallback;
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
		int count = 5;
		List<EntityDTO> entityData = createEntityDtos(count);
		when(mockNodeDao.getEntityDTOs(anyListOf(String.class), anyInt())).thenReturn(entityData);
		
		// call under test
		worker.run(mockPogressCallback, changes);
		verify(mockNodeDao).getEntityDTOs(Lists.newArrayList("111", "222"), EntityReplicationWorker.MAX_ANNOTATION_CHARS);
		verify(mockIndexDao).deleteEntityData(any(ProgressCallback.class) ,eq(Lists.newArrayList(111L,222L,333L)));
		verify(mockIndexDao).addEntityData(any(ProgressCallback.class) ,eq(entityData));
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
	
	@Test
	public void testLockReleaseFailedException() throws RecoverableMessageException, Exception{
		LockReleaseFailedException exception = new LockReleaseFailedException("something went wrong");
		// setup an exception
		doThrow(exception).when(mockIndexDao).addEntityData(any(ProgressCallback.class), anyListOf(EntityDTO.class));
		// call under test
		try {
			worker.run(mockPogressCallback, changes);
			fail("Should have thrown RecoverableMessageException");
		} catch (RecoverableMessageException e) {
			// expected
		}
		// the exception should not be logged.
		verify(mockWorkerLog, never()).logWorkerFailure(anyString(), any(Exception.class), anyBoolean());
	}

	@Test
	public void testCannotAcquireLockException() throws RecoverableMessageException, Exception{
		CannotAcquireLockException exception = new CannotAcquireLockException("something went wrong");
		// setup an exception
		doThrow(exception).when(mockIndexDao).addEntityData(any(ProgressCallback.class), anyListOf(EntityDTO.class));
		// call under test
		try {
			worker.run(mockPogressCallback, changes);
			fail("Should have thrown RecoverableMessageException");
		} catch (RecoverableMessageException e) {
			// expected
		}
		// the exception should not be logged.
		verify(mockWorkerLog, never()).logWorkerFailure(anyString(), any(Exception.class), anyBoolean());
	}
	
	@Test
	public void testDeadlockLoserDataAccessException() throws RecoverableMessageException, Exception{
		DeadlockLoserDataAccessException exception = new DeadlockLoserDataAccessException("message", new RuntimeException());
		// setup an exception
		doThrow(exception).when(mockIndexDao).addEntityData(any(ProgressCallback.class), anyListOf(EntityDTO.class));
		// call under test
		try {
			worker.run(mockPogressCallback, changes);
			fail("Should have thrown RecoverableMessageException");
		} catch (RecoverableMessageException e) {
			// expected
		}
		// the exception should not be logged.
		verify(mockWorkerLog, never()).logWorkerFailure(anyString(), any(Exception.class), anyBoolean());
	}
	
	@Test
	public void testAmazonServiceException() throws RecoverableMessageException, Exception{
		AmazonServiceException exception = new AmazonSQSException("message");
		// setup an exception
		doThrow(exception).when(mockIndexDao).addEntityData(any(ProgressCallback.class), anyListOf(EntityDTO.class));
		// call under test
		try {
			worker.run(mockPogressCallback, changes);
			fail("Should have thrown RecoverableMessageException");
		} catch (RecoverableMessageException e) {
			// expected
		}
		// the exception should not be logged.
		verify(mockWorkerLog, never()).logWorkerFailure(anyString(), any(Exception.class), anyBoolean());
	}
	
	/**
	 * If a single entity is replicated with a null benefactor, then the worker should fail with no-retry.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPLFM_4497Single() throws Exception{
		int count = 1;
		List<EntityDTO> entityData = createEntityDtos(count);
		// set a benefactor ID to be null;
		entityData.get(0).setBenefactorId(null);
		when(mockNodeDao.getEntityDTOs(anyListOf(String.class), anyInt())).thenReturn(entityData);
		// Call under test.
		worker.run(mockPogressCallback, changes);
		// the exception should be logged as retry=false
		boolean willRetry = false;
		verify(mockWorkerLog).logWorkerFailure(anyString(), any(Exception.class), eq(willRetry));
	}
	
	
	/**
	 * Given a batch of entities to replicate, if a single entity in the batch
	 * has a null benefactor, then the entire batch should be retried. Batches
	 * will be retried as individuals.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPLFM_4497Batch() throws Exception{
		int count = 2;
		List<EntityDTO> entityData = createEntityDtos(count);
		// set a benefactor ID to be null;
		entityData.get(0).setBenefactorId(null);
		when(mockNodeDao.getEntityDTOs(anyListOf(String.class), anyInt())).thenReturn(entityData);
		// Call under test.
		try {
			worker.run(mockPogressCallback, changes);
			fail("Should have thrown RecoverableMessageException");
		} catch (RecoverableMessageException e) {
			// expected
		}
		// the exception should not be logged.
		verify(mockWorkerLog, never()).logWorkerFailure(anyString(), any(Exception.class), anyBoolean());
	}
	
	/**
	 * Test helper
	 * 
	 * @param count
	 * @return
	 */
	List<EntityDTO> createEntityDtos(int count){
		List<EntityDTO> dtos = new LinkedList<>();
		for(int i=0; i<count; i++){
			EntityDTO dto = new EntityDTO();
			dto.setId(new Long(i));
			dto.setBenefactorId(new Long(i-1));
			dtos.add(dto);
		}
		return dtos;
	}
}
