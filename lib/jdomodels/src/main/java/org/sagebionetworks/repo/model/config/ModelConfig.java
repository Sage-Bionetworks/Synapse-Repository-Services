package org.sagebionetworks.repo.model.config;

import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.database.semaphore.CountingSemaphoreImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class ModelConfig {

	private static <T extends BasicDataSource> T configureRepoDataSource(T dataSource, StackConfiguration stackConfiguration) {
		dataSource.setDriverClassName(stackConfiguration.getRepositoryDatabaseDriver());
		dataSource.setUsername(stackConfiguration.getRepositoryDatabaseUsername());
		dataSource.setPassword(stackConfiguration.getRepositoryDatabasePassword());
		dataSource.setUrl(stackConfiguration.getRepositoryDatabaseConnectionUrl());
		dataSource.setMinIdle(Integer.parseInt(stackConfiguration.getDatabaseConnectionPoolMinNumberConnections()));
		dataSource.setMaxIdle(Integer.parseInt(stackConfiguration.getDatabaseConnectionPoolMaxNumberConnections()));
		dataSource.setTestOnBorrow(Boolean.valueOf(stackConfiguration.getDatabaseConnectionPoolShouldValidate()));
		dataSource.setValidationQuery(stackConfiguration.getDatabaseConnectionPoolValidateSql());
		dataSource.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		return dataSource;
	}

	/**
	 * Default repo data source pool
	 * 
	 * @param stackConfiguration
	 * @return
	 */
	@Bean(destroyMethod = "close")
	public DataSource dataSourcePool(StackConfiguration stackConfiguration) {
		return configureRepoDataSource(new BasicDataSource(), stackConfiguration);
	}

	/**
	 * Special repo data source that enabled rewriting batched statements increasing the throughput of
	 * inserts, this is used my migration when restoring data
	 * 
	 * @param stackConfiguration
	 * @return
	 */
	@Bean(destroyMethod = "close")
	public DataSource migrationDataSourcePool(StackConfiguration stackConfiguration) {
		BasicDataSource dataSource = configureRepoDataSource(new BasicDataSource(), stackConfiguration);
		dataSource.addConnectionProperty("rewriteBatchedStatements", String.valueOf(true));
		return dataSource;
	}

	@Bean
	// Primary transaction manager used by the database semaphore
	@Primary
	public PlatformTransactionManager txManager(DataSource dataSourcePool) {
		return new DataSourceTransactionManager(dataSourcePool);
	}

	@Bean
	public PlatformTransactionManager migrationTxManager(DataSource migrationDataSourcePool) {
		return new DataSourceTransactionManager(migrationDataSourcePool);
	}

	@Bean
	public JdbcTemplate jdbcTemplate(DataSource dataSourcePool) {
		return new JdbcTemplate(dataSourcePool);
	}
	
	@Bean
	public JdbcTemplate migrationJdbcTemplate(DataSource migrationDataSourcePool) {
		return new JdbcTemplate(migrationDataSourcePool);
	}

	@Bean
	public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
		return new NamedParameterJdbcTemplate(jdbcTemplate);
	}

	@Bean
	public TransactionTemplate readCommitedTransactionTemplate(PlatformTransactionManager txManager) {
		DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();

		txDefinition.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
		txDefinition.setReadOnly(false);
		txDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		txDefinition.setName("readCommitedTransactionTemplate");

		return new TransactionTemplate(txManager, txDefinition);
	}
	
	@Bean
	public CountingSemaphore countingSemaphore(DataSource dataSourcePool) {
		return new CountingSemaphoreImpl(dataSourcePool);
	}
}
