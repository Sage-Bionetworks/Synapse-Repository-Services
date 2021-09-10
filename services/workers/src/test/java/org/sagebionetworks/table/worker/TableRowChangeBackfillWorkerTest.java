package org.sagebionetworks.table.worker;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.TableEntityManager;

@ExtendWith(MockitoExtension.class)
public class TableRowChangeBackfillWorkerTest {

	@Mock
	private ProgressCallback mockCallback;
	
	@Mock
	private TableEntityManager mockManager;
	
	@InjectMocks
	private TableRowChangeBackfillWorker worker;
	
	@Test
	public void testRun() throws Exception {
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockManager).backfillTableRowChangesBatch(TableRowChangeBackfillWorker.BATCH_SIZE);
	}

}
