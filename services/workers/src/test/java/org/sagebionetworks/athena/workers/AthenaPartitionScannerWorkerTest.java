package org.sagebionetworks.athena.workers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.model.athena.AthenaSupport;

import com.amazonaws.services.glue.model.Database;
import com.amazonaws.services.glue.model.Table;

@ExtendWith(MockitoExtension.class)
public class AthenaPartitionScannerWorkerTest {

	@Mock
	private AthenaSupport mockAthenaSupport;
	
	@Mock
	private WorkerLogger mockWorkerLogger;
	
	@InjectMocks
	private AthenaPartitionScannerWorker worker;
	
	@Test
	public void testFireTrigger() throws Exception {
		
		Database database = new Database().withName("Some database");
		Table table = new Table().withName("Some table");
		
		when(mockAthenaSupport.getDatabases()).thenReturn(Collections.singletonList(database).iterator());
		when(mockAthenaSupport.getPartitionedTables(database)).thenReturn(Collections.singletonList(table).iterator());
		
		// Trigger the worker manually
		worker.run(null);
		
		verify(mockAthenaSupport).getDatabases();
		verify(mockAthenaSupport).getPartitionedTables(database);
		verify(mockAthenaSupport).repairTable(table);
		
	}
	
	@Test
	public void testFireTriggerAndFail() throws Exception {
		
		Database database = new Database().withName("Some database");
		Table table = new Table().withName("Some table");
		
		IllegalStateException ex = new IllegalStateException();
		
		when(mockAthenaSupport.getDatabases()).thenReturn(Collections.singletonList(database).iterator());
		when(mockAthenaSupport.getPartitionedTables(database)).thenReturn(Collections.singletonList(table).iterator());
		
		when(mockAthenaSupport.repairTable(any())).thenThrow(ex);

		// Trigger the worker manually
		worker.run(null);
		
		verify(mockAthenaSupport).getDatabases();
		verify(mockAthenaSupport).getPartitionedTables(database);
		verify(mockAthenaSupport).repairTable(table);
		verify(mockWorkerLogger).logWorkerFailure(AthenaPartitionScannerWorker.class.getName(), ex, false);
		
	}
	
}
