package org.sagebionetworks.ids;

import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Configuration for the ID generator, which uses a separate database
 * for generating unique IDs across all Synapse object types.
 */
@Configuration
@ImportResource("classpath:stack-configuration.spb.xml")
public class IdGeneratorConfig {

	/**
	 * DataSource pool for the ID generator database.
	 * This is a separate database from the main repository database.
	 */
	@Bean(destroyMethod = "close")
	public DataSource idGeneratorDataSourcePool(StackConfiguration stackConfiguration) {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(stackConfiguration.getIdGeneratorDatabaseDriver());
		dataSource.setUrl(stackConfiguration.getIdGeneratorDatabaseConnectionUrl());
		dataSource.setUsername(stackConfiguration.getIdGeneratorDatabaseUsername());
		dataSource.setPassword(stackConfiguration.getIdGeneratorDatabasePassword());
		dataSource.setMinIdle(Integer.parseInt(stackConfiguration.getDatabaseConnectionPoolMinNumberConnections()));
		dataSource.setMaxTotal(Integer.parseInt(stackConfiguration.getDatabaseConnectionPoolMaxNumberConnections()));
		dataSource.setMaxIdle(Integer.parseInt(stackConfiguration.getDatabaseConnectionPoolMaxNumberConnections()));
		dataSource.setTestOnBorrow(Boolean.valueOf(stackConfiguration.getDatabaseConnectionPoolShouldValidate()));
		dataSource.setValidationQuery(stackConfiguration.getDatabaseConnectionPoolValidateSql());
		dataSource.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		return dataSource;
	}

	/**
	 * JdbcTemplate for the ID generator database.
	 */
	@Bean(name = "idGeneratorJdbcTemplate")
	public JdbcTemplate idGeneratorJdbcTemplate(@Qualifier("idGeneratorDataSourcePool") DataSource idGeneratorDataSourcePool) {
		return new JdbcTemplate(idGeneratorDataSourcePool);
	}

	/**
	 * IdGenerator bean that uses the ID generator database.
	 */
	@Bean
	public IdGenerator idGenerator(@Qualifier("idGeneratorJdbcTemplate") JdbcTemplate idGeneratorJdbcTemplate) {
		return new IdGeneratorImpl(idGeneratorJdbcTemplate);
	}
}
