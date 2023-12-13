package org.sagebionetworks.repo.model.config;

import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.database.semaphore.CountingSemaphoreImpl;
import org.sagebionetworks.db.pool.Dbcp2DatabaseConnectionPoolStats;
import org.sagebionetworks.repo.model.DatabaseConnectionPoolStats;
import org.sagebionetworks.repo.model.DatabaseConnectionPoolStats.DatabaseType;
import org.sagebionetworks.repo.model.DatabaseConnectionPoolStats.PoolType;
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
		dataSource.setMaxTotal(Integer.parseInt(stackConfiguration.getDatabaseConnectionPoolMaxNumberConnections()));
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
	public BasicDataSource dataSourcePool(StackConfiguration stackConfiguration) {
		return configureRepoDataSource(new BasicDataSource(), stackConfiguration);
	}

	@Bean
	public DatabaseConnectionPoolStats mainDatabaseConnectionPoolStats(BasicDataSource dataSourcePool) {
		return new Dbcp2DatabaseConnectionPoolStats().setPool(dataSourcePool).setDatabaseType(DatabaseType.main)
				.setPoolType(PoolType.standard);
	}

	/**
	 * Special repo data source that enabled rewriting batched statements increasing the throughput of
	 * inserts, this is used my migration when restoring data
	 * 
	 * @param stackConfiguration
	 * @return
	 */
	@Bean(destroyMethod = "close")
	public BasicDataSource migrationDataSourcePool(StackConfiguration stackConfiguration) {
		BasicDataSource dataSource = configureRepoDataSource(new BasicDataSource(), stackConfiguration);
		dataSource.addConnectionProperty("rewriteBatchedStatements", String.valueOf(true));
		return dataSource;
	}
	
	@Bean
	public DatabaseConnectionPoolStats migrationDatabaseConnectionPoolStats(BasicDataSource migrationDataSourcePool) {
		return new Dbcp2DatabaseConnectionPoolStats().setPool(migrationDataSourcePool)
				.setDatabaseType(DatabaseType.main).setPoolType(PoolType.migration);
	}

	@Bean
	// Primary transaction manager used by the database semaphore
	@Primary
	public PlatformTransactionManager txManager(BasicDataSource dataSourcePool) {
		return new DataSourceTransactionManager(dataSourcePool);
	}

	@Bean
	public PlatformTransactionManager migrationTxManager(BasicDataSource migrationDataSourcePool) {
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
