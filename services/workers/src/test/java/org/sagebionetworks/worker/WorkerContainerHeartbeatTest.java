package org.sagebionetworks.worker;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;

@ExtendWith(MockitoExtension.class)
public class WorkerContainerHeartbeatTest {

	@Mock
	private WorkerLogger mockWorkerLogger;

	@Test
	public void testOnTimerFired() {
		WorkerContainerHeartbeat heartbeat = new WorkerContainerHeartbeat(mockWorkerLogger);

		// call under test
		heartbeat.onTimerFired();

		verify(mockWorkerLogger).logCount(WorkerLogger.METRIC_NAME_WORKER_CONTAINER_COUNT, 1.0, Map.of());
		verifyNoMoreInteractions(mockWorkerLogger);
	}

	@Test
	public void testConstructorWithNullLogger() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> new WorkerContainerHeartbeat(null));
	}
}
