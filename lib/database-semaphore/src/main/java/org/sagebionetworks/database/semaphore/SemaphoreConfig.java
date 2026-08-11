package org.sagebionetworks.database.semaphore;

import javax.sql.DataSource;

import org.sagebionetworks.repo.model.config.DatabaseInfrastructureConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuration for database semaphore beans.
 * Imports DatabaseInfrastructureConfiguration to get DataSource and TransactionManager.
 */
@Configuration
@EnableTransactionManagement
@Import(DatabaseInfrastructureConfiguration.class)
public class SemaphoreConfig {

	/**
	 * Creates the CountingSemaphore bean using the primary DataSource.
	 * This bean is used for cluster-wide locking and coordination.
	 */
	@Bean
	public CountingSemaphore countingSemaphore(@Qualifier("dataSourcePool") DataSource dataSourcePool) {
		return new CountingSemaphoreImpl(dataSourcePool);
	}
}
