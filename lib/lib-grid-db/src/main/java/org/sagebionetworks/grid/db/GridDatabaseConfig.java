package org.sagebionetworks.grid.db;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.apache.commons.dbcp2.BasicDataSource;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.grid.encoding.SnapshotFileIndexBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class GridDatabaseConfig {

	@Bean(destroyMethod = "close")
	public BasicDataSource gridDatabaseConnectionPool(StackConfiguration config) {
		// For now we are using the table's database.
		String endpoint = config.getTablesDatabaseEndpointForIndex(0);
		String schema = config.getTablesDatabaseSchemaForIndex(0);
		boolean useSSL = config.useSSLConnectionForTablesDatabase();

		String additionalParameters = "";
		
		if (useSSL) {
			additionalParameters = "&verifyServerCertificate=false&useSSL=true&requireSSL=true";
		}
		
		String url = String.format("jdbc:mysql://%1$s/%2$s?rewriteBatchedStatements=true%3$s&sessionVariables=cte_max_recursion_depth=200000", endpoint, schema,
				additionalParameters);

		// Use the one instance to create a single connection pool
		// Create a database connection pool.
		BasicDataSource connectionPool = new BasicDataSource();
		connectionPool.setDriverClassName(config.getTableDatabaseDriver());
		connectionPool.setUrl(url);
		// For now we use the same username and password as repo.
		connectionPool.setUsername(config.getRepositoryDatabaseUsername());
		connectionPool.setPassword(config.getRepositoryDatabasePassword());
		connectionPool.setMinIdle(Integer.parseInt(config.getDatabaseConnectionPoolMinNumberConnections()));
		connectionPool.setMaxTotal(Integer.parseInt(config.getDatabaseConnectionPoolMaxNumberConnections()));
		connectionPool.setMaxIdle(Integer.parseInt(config.getDatabaseConnectionPoolMaxNumberConnections()));
		connectionPool.setTestOnBorrow(Boolean.parseBoolean(config.getDatabaseConnectionPoolShouldValidate()));
		connectionPool.setValidationQuery(config.getDatabaseConnectionPoolValidateSql());
		return connectionPool;
	}

	@Bean
	public JdbcTemplate gridDatabaseJdbcTemplate(@Qualifier("gridDatabaseConnectionPool") BasicDataSource gridDatabaseConnectionPool) {
		return new JdbcTemplate(gridDatabaseConnectionPool);
	}

	@Bean
	public NamedParameterJdbcTemplate gridDatabaseNamedParameterJdbcTemplate(
			@Qualifier("gridDatabaseConnectionPool") BasicDataSource gridDatabaseConnectionPool) {
		return new NamedParameterJdbcTemplate(gridDatabaseConnectionPool);
	}

	@Bean
	public PlatformTransactionManager gridTransactionManager(@Qualifier("gridDatabaseConnectionPool") BasicDataSource gridDatabaseConnectionPool) {
		return new DataSourceTransactionManager(gridDatabaseConnectionPool);
	}

	@Bean
	public SnapshotFileIndexBuilder snapshotFileIndexBuilder() {
		return new SnapshotFileIndexBuilder();
	}
}
