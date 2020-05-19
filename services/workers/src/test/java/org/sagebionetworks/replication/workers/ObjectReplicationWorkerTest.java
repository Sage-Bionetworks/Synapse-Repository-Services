package org.sagebionetworks.replication.workers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.database.semaphore.LockReleaseFailedException;
import org.sagebionetworks.repo.manager.replication.ReplicationManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class ObjectReplicationWorkerTest {
	
	@Mock
	ReplicationManager mockReplicationManager;
	@Mock
	ProgressCallback mockPogressCallback;
	@Mock
	WorkerLogger mockWorkerLog;
	
	@InjectMocks
	ObjectReplicationWorker worker;
	
	List<ChangeMessage> changes;

	@BeforeEach
	public void before(){
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
	}
	
	@Test
	public void testRun() throws RecoverableMessageException, Exception{
		// call under test
		worker.run(mockPogressCallback, changes);
		verify(mockReplicationManager).replicate(changes);
		verifyZeroInteractions(mockWorkerLog);
	}
	
	@Test
	public void testLogError() throws RecoverableMessageException, Exception{
		RuntimeException exception = new RuntimeException("something went wrong");
		// setup an exception
		doThrow(exception).when(mockReplicationManager).replicate(changes);
		// call under test
		worker.run(mockPogressCallback, changes);
		boolean willRetry = false;
		// the exception should be logged.
		verify(mockWorkerLog).logWorkerFailure(ObjectReplicationWorker.class.getName(), exception, willRetry);
	}
	
	@Test
	public void testLockReleaseFailedException() throws RecoverableMessageException, Exception{
		LockReleaseFailedException exception = new LockReleaseFailedException("something went wrong");
		// setup an exception
		doThrow(exception).when(mockReplicationManager).replicate(changes);

		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockPogressCallback, changes);
		});
		
		// the exception should not be logged.
		verify(mockWorkerLog, never()).logWorkerFailure(anyString(), any(Exception.class), anyBoolean());
	}

	@Test
	public void testCannotAcquireLockException() throws RecoverableMessageException, Exception{
		CannotAcquireLockException exception = new CannotAcquireLockException("something went wrong");
		// setup an exception
		doThrow(exception).when(mockReplicationManager).replicate(changes);
		
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockPogressCallback, changes);
		});
		
		// the exception should not be logged.
		verify(mockWorkerLog, never()).logWorkerFailure(anyString(), any(Exception.class), anyBoolean());
	}
	
	@Test
	public void testDeadlockLoserDataAccessException() throws RecoverableMessageException, Exception{
		DeadlockLoserDataAccessException exception = new DeadlockLoserDataAccessException("message", new RuntimeException());
		// setup an exception
		doThrow(exception).when(mockReplicationManager).replicate(changes);
		
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockPogressCallback, changes);
		});
		
		// the exception should not be logged.
		verify(mockWorkerLog, never()).logWorkerFailure(anyString(), any(Exception.class), anyBoolean());
	}
	
	@Test
	public void testAmazonServiceException() throws RecoverableMessageException, Exception{
		AmazonServiceException exception = new AmazonSQSException("message");
		// setup an exception
		doThrow(exception).when(mockReplicationManager).replicate(changes);

		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockPogressCallback, changes);
		});
		
		// the exception should not be logged.
		verify(mockWorkerLog, never()).logWorkerFailure(anyString(), any(Exception.class), anyBoolean());
	}
	
	/**
	 * Test helper
	 * 
	 * @param count
	 * @return
	 */
	List<ObjectDataDTO> createEntityDtos(int count){
		List<ObjectDataDTO> dtos = new LinkedList<>();
		for(int i=0; i<count; i++){
			ObjectDataDTO dto = new ObjectDataDTO();
			dto.setId(new Long(i));
			dto.setBenefactorId(new Long(i-1));
			dtos.add(dto);
		}
		return dtos;
	}
}
