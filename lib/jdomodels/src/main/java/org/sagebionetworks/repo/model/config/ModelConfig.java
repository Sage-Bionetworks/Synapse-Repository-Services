package org.sagebionetworks.repo.model.config;

import java.sql.Connection;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.datasource.ContextBasedDataSource;
import org.sagebionetworks.repo.model.datasource.DataSourceContextAspect;
import org.sagebionetworks.repo.model.datasource.DataSourceType;
import org.sagebionetworks.repo.model.datasource.RewritableBatchedStatementsDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
	
	@Bean(destroyMethod = "close")
	public DataSource repoDataSourcePool(StackConfiguration stackConfiguration) {
		return configureRepoDataSource(new BasicDataSource(), stackConfiguration);
	}
	
	@Bean(destroyMethod = "close")
	public DataSource repoBatchingDataSourcePool(StackConfiguration stackConfiguration) {
		RewritableBatchedStatementsDataSource dataSource = configureRepoDataSource(new RewritableBatchedStatementsDataSource(), stackConfiguration);
		dataSource.setRewriteBatchedStatements(true);
		return dataSource;
	}
	
	@Bean
	public DataSource dataSourcePool(DataSource repoDataSourcePool, DataSource repoBatchingDataSourcePool) {
		ContextBasedDataSource dataSource = new ContextBasedDataSource();
		
		dataSource.setTargetDataSources(Map.of(
			DataSourceType.REPO, repoDataSourcePool,
			DataSourceType.REPO_BATCHING, repoBatchingDataSourcePool
		));
		
		dataSource.setDefaultTargetDataSource(repoDataSourcePool);
		dataSource.setLenientFallback(false);
		
		return dataSource;
	}
	
	@Bean
	public PlatformTransactionManager txManager(DataSource dataSourcePool) {
		return new DataSourceTransactionManager(dataSourcePool);
	}
	
	@Bean
	public JdbcTemplate jdbcTemplate(DataSource dataSourcePool) {
		return new JdbcTemplate(dataSourcePool);
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
	public DataSourceContextAspect dataSourceContextAspect() {
		return new DataSourceContextAspect();
	}	
}
