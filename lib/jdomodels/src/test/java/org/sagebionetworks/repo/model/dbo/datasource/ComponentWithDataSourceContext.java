package org.sagebionetworks.repo.model.dbo.datasource;

import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@DataSourceContext(DataSourceType.MIGRATION)
public class ComponentWithDataSourceContext {
	
	private JdbcTemplate jdbcTemplate;
	
	public ComponentWithDataSourceContext(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@WriteTransaction
	public void doSomething() {
		jdbcTemplate.queryForObject("SELECT 1", Long.class);
	}

}
