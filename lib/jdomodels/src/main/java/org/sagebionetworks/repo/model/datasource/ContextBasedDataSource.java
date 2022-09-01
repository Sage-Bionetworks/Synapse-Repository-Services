package org.sagebionetworks.repo.model.datasource;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Special {@link DataSource} that allows to dynamically change the underlying data source according to the {@link DataSourceType} currently bounded to the thread
 */
public class ContextBasedDataSource extends AbstractRoutingDataSource {
	
	@Override
	protected Object determineCurrentLookupKey() {
		return DataSourceContextHolder.get();
	}
}
