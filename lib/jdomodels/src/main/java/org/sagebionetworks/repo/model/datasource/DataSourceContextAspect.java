package org.sagebionetworks.repo.model.datasource;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;

/**
 * Aspect used to set the {@link DataSourceType} for the current thread through the {@link DataSourceContext} annotation
 */
@Aspect
// Make sure this executed before any transaction advice (by default it's LOWEST_PRECEDENCE, but it executes before any custom advice)
@Order(1)
public class DataSourceContextAspect {
	
	@Pointcut("execution(public * *(..))")
	private void anyPublicMethod() {}
	
	@Pointcut("@within(dataSourceContext)")
	public void annotatedClassPointCut(DataSourceContext dataSourceContext) {}
	
	@Before("annotatedClassPointCut(dataSourceContext) && anyPublicMethod()")
	public void setDataSourceContextBeforeExecution(DataSourceContext dataSourceContext) {
		DataSourceContextHolder.set(dataSourceContext.value());
	}
	
	@After("annotatedClassPointCut(dataSourceContext) && anyPublicMethod()")
	public void clearDataSourceContextAfterExecution(DataSourceContext dataSourceContext) {
		DataSourceContextHolder.clear();
	}

}
