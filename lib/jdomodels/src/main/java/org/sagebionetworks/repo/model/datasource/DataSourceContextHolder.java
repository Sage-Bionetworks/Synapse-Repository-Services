package org.sagebionetworks.repo.model.datasource;

import org.sagebionetworks.util.ThreadLocalProvider;

/**
 * Utility to set the {@link DataSourceType} for the current thread
 */
public class DataSourceContextHolder {
	
	private static ThreadLocal<DataSourceType> CONTEXT = ThreadLocalProvider.getInstance("dataSourceContext", DataSourceType.class);

	public static DataSourceType get() {
		return CONTEXT.get();
	}
	
	public static void set(DataSourceType type) {
		DataSourceType currentType = CONTEXT.get();
		
		// Prevent setting different data source types within the same thread, this avoids
		// inconsistent transactional contexts and unexpected behavior, since the nested transaction might participate in the 
		// parent transaction in which case the transaction would be started in the outer data source
		if (currentType != null && currentType != type) {
			throw new IllegalStateException("Cannot mix different types of data sources in the same thread");
		}
		
		CONTEXT.set(type);
	}
	
	public static void clear() {
		CONTEXT.remove();
	}
	
}
