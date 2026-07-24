package org.sagebionetworks.repo.model.config;

import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.lib.dbuserhelper.DBUserHelper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Database infrastructure configuration providing core beans for database access.
 * This configuration provides DataSource, JdbcTemplate, and TransactionManager beans
 * for both the main repository database and the migration database.
 */
@Configuration
@EnableTransactionManagement
@ImportResource("classpath:stack-configuration.spb.xml")
public class DatabaseInfrastructureConfiguration {

	private static <T extends BasicDataSource> T configureRepoDataSource(T dataSource, StackConfiguration stackConfiguration) {
		dataSource.setDriverClassName(stackConfiguration.getRepositoryDatabaseDriver());
		dataSource.setUsername(stackConfiguration.getRepositoryDatabaseUsername());
		dataSource.setPassword(stackConfiguration.getRepositoryDatabasePassword());
		dataSource.setUrl(stackConfiguration.getRepositoryDatabaseConnectionUrl());
		dataSource.setMinIdle(Integer.parseInt(stackConfiguration.getDatabaseConnectionPoolMinNumberConnections()));
		// See: https://sagebionetworks.jira.com/browse/PLFM-8344
		dataSource.setMaxTotal(-1);
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
	@Primary
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

	// This is the primary transaction manager used by the application, it is also used by the semaphore
	// but under a different name for clarity
	@Primary
	@Bean(name = {"txManager"})
	public PlatformTransactionManager txManager(@Qualifier("dataSourcePool") DataSource dataSourcePool) {
		return new DataSourceTransactionManager(dataSourcePool);
	}

	@Bean
	public PlatformTransactionManager migrationTxManager(@Qualifier("migrationDataSourcePool") DataSource migrationDataSourcePool) {
		return new DataSourceTransactionManager(migrationDataSourcePool);
	}

	@Primary
	@Bean (name="jdbcTemplate")
	public JdbcTemplate jdbcTemplate(@Qualifier("dataSourcePool") DataSource dataSourcePool,
			org.sagebionetworks.StackConfiguration stackConfiguration) {
		JdbcTemplate template = new JdbcTemplate(dataSourcePool);
		// Create the read-only user in the main database
		DBUserHelper.createDbReadOnlyUser(template, stackConfiguration);
		return template;
	}

	@Bean
	public JdbcTemplate migrationJdbcTemplate(@Qualifier("migrationDataSourcePool") DataSource migrationDataSourcePool) {
		return new JdbcTemplate(migrationDataSourcePool);
	}

	@Primary
	@Bean
	public NamedParameterJdbcTemplate namedParameterJdbcTemplate(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
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
	public TransactionTemplate readCommittedRequiresNew(PlatformTransactionManager txManager) {
		DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();

		txDefinition.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
		txDefinition.setReadOnly(false);
		txDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		txDefinition.setName("readCommittedRequiresNew");

		return new TransactionTemplate(txManager, txDefinition);
	}
}
