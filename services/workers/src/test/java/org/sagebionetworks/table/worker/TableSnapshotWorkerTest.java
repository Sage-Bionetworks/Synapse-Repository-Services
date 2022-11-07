package org.sagebionetworks.table.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatusChangeEvent;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class TableSnapshotWorkerTest {
	
	@Mock
	private TableEntityManager mockTableManager;
	
	@Mock
	private TableManagerSupport mockTableManagerSupport;
	
	@InjectMocks
	private TableSnapshotWorker worker;
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Mock
	private Message mockMessage;
	
	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		
		IdAndVersion idAndVersion = IdAndVersion.parse("123.2");
		
		TableStatusChangeEvent event = new TableStatusChangeEvent()
			.setObjectId(idAndVersion.getId().toString())
			.setObjectVersion(idAndVersion.getVersion().get())
			.setState(TableState.AVAILABLE);
		
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.table);
		
		// Call under test
		worker.run(mockCallback, mockMessage, event);
		
		verify(mockTableManagerSupport).getTableType(idAndVersion);
		verify(mockTableManager).storeTableSnapshot(idAndVersion);
	}
	
	@Test
	public void testRunWithNoVersion() throws RecoverableMessageException, Exception {
		
		TableStatusChangeEvent event = new TableStatusChangeEvent()
			.setObjectId("123")
			.setState(TableState.AVAILABLE);
		
		// Call under test
		worker.run(mockCallback, mockMessage, event);
		
		verifyZeroInteractions(mockTableManager);
		verifyZeroInteractions(mockTableManagerSupport);
	}
	
	@Test
	public void testRunWithNotAvailable() throws RecoverableMessageException, Exception {
		
		TableStatusChangeEvent event = new TableStatusChangeEvent()
			.setObjectId("123")
			.setObjectVersion(2L)
			.setState(TableState.PROCESSING);
		
		// Call under test
		worker.run(mockCallback, mockMessage, event);
		
		verifyZeroInteractions(mockTableManager);
		verifyZeroInteractions(mockTableManagerSupport);
	}
	
	@Test
	public void testRunWithNotATable() throws RecoverableMessageException, Exception {
		
		TableStatusChangeEvent event = new TableStatusChangeEvent()
			.setObjectId("123")
			.setObjectVersion(2L)
			.setState(TableState.AVAILABLE);
		
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		
		// Call under test
		worker.run(mockCallback, mockMessage, event);

		verifyZeroInteractions(mockTableManagerSupport);
		verifyZeroInteractions(mockTableManager);
	}

}
