package org.sagebionetworks.grid.db;

import org.apache.commons.dbcp2.BasicDataSource;
import org.sagebionetworks.StackConfiguration;
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
		String url = String.format("jdbc:mysql://%1$s/%2$s?rewriteBatchedStatements=true%3$s", endpoint, schema,
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
	public JdbcTemplate gridDatabaseJdbcTempalte(BasicDataSource gridDatabaseConnectionPool) {
		return new JdbcTemplate(gridDatabaseConnectionPool);
	}

	@Bean
	public NamedParameterJdbcTemplate gridDatabaseNamedParameterJdbcTempalte(
			BasicDataSource gridDatabaseConnectionPool) {
		return new NamedParameterJdbcTemplate(gridDatabaseConnectionPool);
	}

	@Bean
	public PlatformTransactionManager gridTransactionManager(BasicDataSource gridDatabaseConnectionPool) {
		return new DataSourceTransactionManager(gridDatabaseConnectionPool);
	}

}
