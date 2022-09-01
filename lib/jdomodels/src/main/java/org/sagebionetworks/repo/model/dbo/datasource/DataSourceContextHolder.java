package org.sagebionetworks.repo.model.dbo.datasource;

import org.sagebionetworks.util.ThreadLocalProvider;

public class DataSourceContextHolder {
	
	private static ThreadLocal<DataSourceType> CONTEXT = ThreadLocalProvider.getInstance("dataSourceContext", DataSourceType.class);

	public static DataSourceType get() {
		return CONTEXT.get();
	}
	
	public static void set(DataSourceType context) {
		CONTEXT.set(context);
	}
	
}
