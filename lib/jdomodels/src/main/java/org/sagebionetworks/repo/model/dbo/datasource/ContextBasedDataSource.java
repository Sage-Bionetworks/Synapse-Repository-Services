package org.sagebionetworks.repo.model.dbo.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class ContextBasedDataSource extends AbstractRoutingDataSource {
	
	@Override
	protected Object determineCurrentLookupKey() {
		return DataSourceContextHolder.get();
	}
}
