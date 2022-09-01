package org.sagebionetworks.repo.model.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class ContextBasedDataSourceTest {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DataSource repoDataSourcePool;
	
	@Autowired
	private DataSource repoBatchingDataSourcePool;
	
	@Test
	public void testContextBasedDataSource() {
		ContextBasedDataSource dataSource = (ContextBasedDataSource) jdbcTemplate.getDataSource();
		
		assertEquals(repoDataSourcePool, dataSource.getResolvedDefaultDataSource());
		assertEquals(Map.of(
				DataSourceType.REPO_BATCHING, repoBatchingDataSourcePool,
				DataSourceType.REPO, repoDataSourcePool), 
			dataSource.getResolvedDataSources()
		);
	}

}
