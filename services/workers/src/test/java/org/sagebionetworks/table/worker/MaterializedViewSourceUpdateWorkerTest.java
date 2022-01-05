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
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.MaterializedViewManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.ChangeMessage;

@ExtendWith(MockitoExtension.class)
public class MaterializedViewSourceUpdateWorkerTest {
	
	@Mock
	private MaterializedViewManager mockManager;
	
	@InjectMocks
	private MaterializedViewSourceUpdateWorker worker;
	
	@Mock
	private ProgressCallback mockCallBack;
	
	@Mock
	private ChangeMessage mockMessage;

	@Test
	public void testRun() throws Exception {
		when(mockMessage.getObjectType()).thenReturn(ObjectType.ENTITY);
		when(mockMessage.getObjectId()).thenReturn("syn123");
		when(mockMessage.getObjectVersion()).thenReturn(null);
		
		IdAndVersion expectedVersion = IdAndVersion.parse("123");
		
		// Call under test
		worker.run(mockCallBack, mockMessage);
		
		verify(mockManager).refreshDependentMaterializedViews(expectedVersion);
		
	}
	
	@Test
	public void testRunWithVersion() throws Exception {
		when(mockMessage.getObjectType()).thenReturn(ObjectType.ENTITY);
		when(mockMessage.getObjectId()).thenReturn("syn123");
		when(mockMessage.getObjectVersion()).thenReturn(2L);
		
		IdAndVersion expectedVersion = IdAndVersion.parse("123.2");
		
		// Call under test
		worker.run(mockCallBack, mockMessage);
		
		verify(mockManager).refreshDependentMaterializedViews(expectedVersion);
		
	}
	
	@Test
	public void testWithWrongObjectType() throws Exception {
		
		for (ObjectType type : ObjectType.values()) {
			
			when(mockMessage.getObjectType()).thenReturn(type);
			
			if (ObjectType.ENTITY == type) {
				continue;
			}
			
			String errorMessage = assertThrows(IllegalStateException.class, () -> {				
				// Call under test
				worker.run(mockCallBack, mockMessage);
			}).getMessage();
			
			verifyZeroInteractions(mockManager);
			
			assertEquals("Expected ENTITY type, got: " + type, errorMessage);
		}
	}

}
