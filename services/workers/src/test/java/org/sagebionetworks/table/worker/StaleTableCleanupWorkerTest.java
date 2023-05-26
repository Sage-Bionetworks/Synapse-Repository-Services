package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexManager;

@ExtendWith(MockitoExtension.class)
public class StaleTableCleanupWorkerTest {

	@Mock
	private LoggerProvider mockLoggerProvider;
	@Mock
	private Logger mockLogger;
	@Mock
	private TableIndexConnectionFactory mockConnectionFactory;
	@Mock
	private TableIndexManager mockIndexManager;
	@Mock
	private ProgressCallback mockCallback;
	@Captor
	private ArgumentCaptor<String> logCaptor;

	private StaleTableCleaupWorker worker;

	@BeforeEach
	public void before() {
		when(mockLoggerProvider.getLogger(any())).thenReturn(mockLogger);
		worker = new StaleTableCleaupWorker(mockConnectionFactory, mockLoggerProvider);
	}
	
	@Test
	public void testRun() throws Exception {
		when(mockConnectionFactory.connectToFirstIndex()).thenReturn(mockIndexManager);
		when(mockIndexManager.deleteStaleTables()).thenReturn(10);
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockConnectionFactory).connectToFirstIndex();
		verify(mockIndexManager).deleteStaleTables();
		verify(mockLogger).info(logCaptor.capture());
		
		assertTrue(logCaptor.getValue().startsWith("Deleted 10 stale tables (Took:"));
	}
	
	@Test
	public void testRunWithNoDeletedTable() throws Exception {
		when(mockConnectionFactory.connectToFirstIndex()).thenReturn(mockIndexManager);
		when(mockIndexManager.deleteStaleTables()).thenReturn(0);
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockConnectionFactory).connectToFirstIndex();
		verify(mockIndexManager).deleteStaleTables();
		verifyZeroInteractions(mockLogger);
	}
}
