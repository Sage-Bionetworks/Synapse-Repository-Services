package org.sagebionetworks.worker.config;

import java.util.Map;

import org.apache.commons.dbcp2.BasicDataSource;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentManager;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentManagerImpl;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.manager.monitoring.DataSourcePoolMonitor;
import org.sagebionetworks.repo.manager.monitoring.DataSourcePoolMonitor.ApplicationType;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.worker.utils.StackStatusGate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import com.amazonaws.services.sqs.AmazonSQSClient;

@Configuration
public class WorkersInfraConfig {
	
	private static final long DB_MONITOR_INTERVAL = 10_000;
	
	private StackConfiguration config;
	private Consumer consumer;
	
	public WorkersInfraConfig(StackConfiguration config, Consumer consumer) {
		this.config = config;
		this.consumer = consumer;
	}

	@Bean
	public StackStatusGate stackStatusGate() {
		return new StackStatusGate();
	}

	@Bean
	public ConcurrentManager concurrentStackManager(CountingSemaphore countingSemaphore, AmazonSQSClient amazonSQSClient, StackStatusDao stackStatusDao) {
		return new ConcurrentManagerImpl(countingSemaphore, amazonSQSClient, stackStatusDao);
	}
	
	@Bean
	public SimpleTriggerFactoryBean dataSourceMonitorTrigger(Map<String, BasicDataSource> dataSources) {
		return new WorkerTriggerBuilder()
			.withDataSourceMonitor(new DataSourcePoolMonitor(ApplicationType.workers, dataSources, consumer, config))
			.withRepeatInterval(DB_MONITOR_INTERVAL)
			.withStartDelay(DB_MONITOR_INTERVAL)
			.build();
	}

}
