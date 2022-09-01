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
	
	public static void set(DataSourceType context) {
		CONTEXT.set(context);
	}
	
	public static void clear() {
		CONTEXT.remove();
	}
	
}
