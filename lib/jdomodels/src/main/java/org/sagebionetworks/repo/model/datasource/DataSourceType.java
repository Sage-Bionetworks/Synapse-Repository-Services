package org.sagebionetworks.repo.model.datasource;

import javax.sql.DataSource;

/**
 * Types of {@link DataSource} exposed by the repo services, can be switched at runtime using the
 * {@link DataSourceContext} annotation or through the {@link DataSourceContextHolder}
 */
public enum DataSourceType {
	/**
	 * Default repository services data source
	 */
	REPO,
	/**
	 * Special data source used for migration that enable high throughput when inserting data 
	 */
	REPO_BATCHING
}
