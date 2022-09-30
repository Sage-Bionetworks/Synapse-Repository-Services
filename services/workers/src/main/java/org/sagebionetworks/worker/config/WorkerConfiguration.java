package org.sagebionetworks.worker.config;

import org.quartz.SimpleTrigger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentChangeMessageWorkerStackBuilder;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentSingleton;
import org.sagebionetworks.table.worker.TableIndexWorker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerConfiguration {

	@Bean(name = "tableQueueMessageReveiverTrigger")
	SimpleTrigger createTableIndexWorker(ConcurrentSingleton singleton, StackConfiguration stackConfig,
			TableIndexWorker worker) {
		return new SimpleTriggerBuilder()
				.withTargetObject(new ConcurrentChangeMessageWorkerStackBuilder()
						.withSingleton(singleton)
						.withSemaphoreLockKey("tableIndexWorker")
						.withSemaphoreMaxLockCount(10)
						.withSemaphoreLockAndMessageVisibilityTimeoutSec(1200)
						.withMaxThreadsPerMachine(20)
						.withQueueName(stackConfig.getQueueName("TABLE_UPDATE"))
						.withCanRunInReadOnly(true)
						.withWorker(worker)
						.build())
				.withRepeatIntervalMS(1797L).build();
	}

}
