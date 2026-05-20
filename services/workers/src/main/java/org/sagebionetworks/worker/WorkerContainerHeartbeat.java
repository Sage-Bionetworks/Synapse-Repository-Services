package org.sagebionetworks.worker;

import java.util.Map;

import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Emits a per-JVM heartbeat metric so the live count of workers-WAR Spring
 * containers can be derived from CloudWatch. Each running container publishes
 * {@link WorkerLogger#METRIC_NAME_WORKER_CONTAINER_COUNT} = 1 on every Quartz
 * tick. With no dimensions on the emission, summing the metric across the
 * fleet over a one-minute window yields the number of live containers.
 */
public class WorkerContainerHeartbeat {

	private final WorkerLogger workerLogger;

	public WorkerContainerHeartbeat(WorkerLogger workerLogger) {
		ValidateArgument.required(workerLogger, "workerLogger");
		this.workerLogger = workerLogger;
	}

	/**
	 * Called from a Quartz timer.
	 */
	public void onTimerFired() {
		workerLogger.logCount(WorkerLogger.METRIC_NAME_WORKER_CONTAINER_COUNT, 1.0, Map.of());
	}
}
