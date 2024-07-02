package org.sagebionetworks.worker.config;

import org.sagebionetworks.auth.workers.ExpiredAccessTokenWorker;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.file.worker.FileHandleAssociationScanDispatcherWorker;
import org.sagebionetworks.worker.utils.StackStatusGate;
import org.sagebionetworks.workers.util.semaphore.SemaphoreGatedWorkerStack;
import org.sagebionetworks.workers.util.semaphore.SemaphoreGatedWorkerStackConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

/**
 * Configuration for workers that are simply triggered on a timer
 */
@Configuration
public class TimerWorkersConfig {

	private StackStatusGate stackStatusGate;
	private CountingSemaphore countingSemaphore;
	
	public TimerWorkersConfig(StackStatusGate stackStatusGate, CountingSemaphore countingSemaphore) {
		this.stackStatusGate = stackStatusGate;
		this.countingSemaphore = countingSemaphore;
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleAssociationScanDispatcherWorkerTrigger(FileHandleAssociationScanDispatcherWorker fileHandleAssociationScanDispatcherWorker) {
		
		SemaphoreGatedWorkerStackConfiguration config = new SemaphoreGatedWorkerStackConfiguration();
		
		config.setSemaphoreLockKey("fileHandleAssociationScanDispatcher");
		config.setProgressingRunner(fileHandleAssociationScanDispatcherWorker);
		config.setSemaphoreMaxLockCount(1);
		config.setSemaphoreLockTimeoutSec(60);
		config.setGate(stackStatusGate);
		
		return new WorkerTriggerBuilder()
			.withStack(new SemaphoreGatedWorkerStack(countingSemaphore, config))
			// We do not need to check this often, we run this every 5 days. 
			.withRepeatInterval(1800000)
			// Note: the start delay is actually 2 hours, reason being that when the staging stack is first deployed it might take a while 
			// before we start migration which is when the stack is put to read-only mode.
			// If we do not wait for this the scanner will scan a mostly empty database delaying the next scan for at least 5 days.
			.withStartDelay(7200000)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean expiredAccessTokensWorkerTrigger(ExpiredAccessTokenWorker worker) {
		SemaphoreGatedWorkerStackConfiguration config = new SemaphoreGatedWorkerStackConfiguration();
		
		config.setSemaphoreLockKey("expiredAccessTokensWorker");
		config.setProgressingRunner(worker);
		config.setSemaphoreMaxLockCount(1);
		config.setSemaphoreLockTimeoutSec(60);
		config.setGate(stackStatusGate);
		
		return new WorkerTriggerBuilder()
				.withStack(new SemaphoreGatedWorkerStack(countingSemaphore, config))
				.withRepeatInterval(10 * 60 * 1000)
				.withStartDelay(10 * 60 * 1000)
				.build();
	}

}
