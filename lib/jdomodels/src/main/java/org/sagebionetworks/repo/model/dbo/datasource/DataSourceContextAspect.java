package org.sagebionetworks.repo.model.dbo.datasource;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;

@Aspect
// Make sure this executed before any transaction advice (by default it's LOWEST_PRECEDENCE, but it executes before any custom advide)
@Order(1)
public class DataSourceContextAspect {

	@Before("@within(dataSourceContext)")
	public void setDataSourceContext(DataSourceContext dataSourceContext) {
		// If the datasource context was already set by an outer context we do not want to override it 
		if (DataSourceContextHolder.get() == null) {
			DataSourceContextHolder.set(dataSourceContext.value());
		}
	}

}
