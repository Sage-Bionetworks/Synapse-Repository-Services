package org.sagebionetworks.repo.model.dbo.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DataSourceContextTest {
	
	@Autowired
	private ComponentWithDataSourceContext component;
	@Autowired
	private ContextBasedDataSource dataSource;

	@Test
	public void testDataSourceContext() {
		assertNull(DataSourceContextHolder.get());
		component.doSomething();
		assertEquals(DataSourceType.MIGRATION, DataSourceContextHolder.get());
		assertEquals(DataSourceType.MIGRATION, dataSource.determineCurrentLookupKey());
	}

}
