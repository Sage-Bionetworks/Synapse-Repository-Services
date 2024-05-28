package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.MaterializedViewManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatusChangeEvent;
import org.sagebionetworks.util.progress.ProgressCallback;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class MaterializedViewSourceUpdateWorkerTest {
	
	@Mock
	private MaterializedViewManager mockManager;
	
	@InjectMocks
	private MaterializedViewSourceUpdateWorker worker;
	
	@Mock
	private ProgressCallback mockCallBack;
	
	@Mock
	private TableStatusChangeEvent mockEvent;
	
	@Mock
	private Message mockMessage;

	@Test
	public void testRun() throws Exception {
		when(mockEvent.getObjectType()).thenReturn(ObjectType.TABLE_STATUS_EVENT);
		when(mockEvent.getObjectId()).thenReturn("syn123");
		when(mockEvent.getObjectVersion()).thenReturn(null);
		when(mockEvent.getState()).thenReturn(TableState.AVAILABLE);
		
		IdAndVersion expectedIdAndVersion = IdAndVersion.parse("123");
		
		// Call under test
		worker.run(mockCallBack, mockMessage, mockEvent);
		
		verify(mockManager).refreshDependentMaterializedViews(expectedIdAndVersion);
		
	}
	
	@Test
	public void testRunWithVersion() throws Exception {
		when(mockEvent.getObjectType()).thenReturn(ObjectType.TABLE_STATUS_EVENT);
		when(mockEvent.getObjectId()).thenReturn("syn123");
		when(mockEvent.getObjectVersion()).thenReturn(2L);
		when(mockEvent.getState()).thenReturn(TableState.AVAILABLE);
		
		IdAndVersion expectedIdAndVersion = IdAndVersion.parse("123.2");
		
		// Call under test
		worker.run(mockCallBack, mockMessage, mockEvent);
		
		verify(mockManager).refreshDependentMaterializedViews(expectedIdAndVersion);
		
	}
	
	@Test
	public void testRunWithProcessingState() throws Exception {
		when(mockEvent.getObjectType()).thenReturn(ObjectType.TABLE_STATUS_EVENT);
		when(mockEvent.getState()).thenReturn(TableState.PROCESSING);
		
		// Call under test
		worker.run(mockCallBack, mockMessage, mockEvent);
		
		verifyZeroInteractions(mockManager);
		
	}
	
	@Test
	public void testRunWithFailedState() throws Exception {
		when(mockEvent.getObjectType()).thenReturn(ObjectType.TABLE_STATUS_EVENT);
		when(mockEvent.getState()).thenReturn(TableState.PROCESSING_FAILED);
		
		// Call under test
		worker.run(mockCallBack, mockMessage, mockEvent);
		
		verifyZeroInteractions(mockManager);
		
	}
	
	@Test
	public void testRunWithWrongObjectType() throws Exception {
		when(mockEvent.getObjectType()).thenReturn(ObjectType.ENTITY);
		
		String message = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			worker.run(mockCallBack, mockMessage, mockEvent);
		}).getMessage();
		
		assertEquals("Unsupported object type: expected TABLE_STATUS_EVENT, got ENTITY", message);

		verifyZeroInteractions(mockManager);
		
	}

}
