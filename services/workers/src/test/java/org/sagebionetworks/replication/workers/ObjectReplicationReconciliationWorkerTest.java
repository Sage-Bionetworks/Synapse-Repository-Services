package org.sagebionetworks.replication.workers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.replication.ReplicationManager;
import org.sagebionetworks.repo.manager.replication.ReplicationMessageManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Unit test for EntityReplicationDeltaWorker.
 *
 */
@ExtendWith(MockitoExtension.class)
public class ObjectReplicationReconciliationWorkerTest {
	
	@Mock
	ReplicationMessageManager mockReplicationMessageManager;
	
	@Mock
	ReplicationManager mockReplicationManager;
	
	@Mock
	WorkerLogger mockWorkerLog;
	
	@Mock
	ProgressCallback mockProgressCallback;
	
	@InjectMocks
	ObjectReplicationReconciliationWorker worker;
	
	IdAndVersion viewId;	
	
	ChangeMessage message;
	
	ViewObjectType viewObjectType;
	
	ViewScopeType viewScopeType;
	
	@BeforeEach
	public void before() throws JSONObjectAdapterException{
		viewObjectType = ViewObjectType.ENTITY;
		
		viewId = IdAndVersion.parse("syn987");
		
		message = new ChangeMessage();
		message.setObjectType(ObjectType.ENTITY_VIEW);
		message.setObjectId(viewId.toString());
		message.setTimestamp(new Date(1L));		
		
		viewScopeType = new ViewScopeType(viewObjectType, ViewTypeMask.File.getMask());
	}
	
	
	@Test
	public void testRun(){
		when(mockReplicationMessageManager.getApproximateNumberOfMessageOnReplicationQueue())
				.thenReturn(ObjectReplicationReconciliationWorker.MAX_MESSAGE_TO_RUN_RECONCILIATION - 1L);
		
		// call under test
		worker.run(mockProgressCallback, message);
		
		verify(mockReplicationMessageManager).getApproximateNumberOfMessageOnReplicationQueue();
		verify(mockReplicationManager).reconcile(viewId);
		
		// no exceptions should occur.
		verifyZeroInteractions(mockWorkerLog);
	}
	
	@Test
	public void testRunMessageCountOverMax(){
		when(mockReplicationMessageManager.getApproximateNumberOfMessageOnReplicationQueue())
		.thenReturn(ObjectReplicationReconciliationWorker.MAX_MESSAGE_TO_RUN_RECONCILIATION + 1L);
		
		// call under test
		worker.run(mockProgressCallback, message);
		// no work should occur when over the max.
		verifyZeroInteractions(mockReplicationManager);
		verify(mockReplicationMessageManager).getApproximateNumberOfMessageOnReplicationQueue();
		
		// no exceptions should occur.
		verifyZeroInteractions(mockWorkerLog);
	}
	
	@Test
	public void testRunFailure(){
		
		when(mockReplicationMessageManager.getApproximateNumberOfMessageOnReplicationQueue())
				.thenReturn(ObjectReplicationReconciliationWorker.MAX_MESSAGE_TO_RUN_RECONCILIATION - 1L);
		
		Exception exception = new RuntimeException("Something went wrong");
		
		doThrow(exception).when(mockReplicationManager).reconcile(any());
		
		// call under test
		worker.run(mockProgressCallback, message);
		
		// the exception should be logged
		boolean willRetry = false;
		verify(mockWorkerLog).logWorkerFailure(ObjectReplicationReconciliationWorker.class.getName(), exception, willRetry);
	}

}
