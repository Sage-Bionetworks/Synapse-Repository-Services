package org.sagebionetworks.repo.model.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DataSourceContextAspectTest {
	
	@Autowired
	private ComponentWithDataSourceContext annotatedClass;
		
	@Test
	public void testDataSourceContextAspect() {
		assertNull(DataSourceContextHolder.get());
		// Call under test
		annotatedClass.doSomething();
		assertNull(DataSourceContextHolder.get());
	}
	
	@Test
	public void testDataSourceContextAspectWithProtectedMethod() {
		assertNull(DataSourceContextHolder.get());
		// Call under test
		annotatedClass.doSomethingElse();
		assertNull(DataSourceContextHolder.get());
	}
	
	@Test
	public void testDataSourceContextAspectWithMixedDataSources() {
		assertNull(DataSourceContextHolder.get());
		assertThrows(IllegalStateException.class, () -> {
			// Call under test
			annotatedClass.doSomethingWithInnerComponent();
		});
	}
		
	@Component
	@DataSourceContext(DataSourceType.REPO_BATCHING)
	public static class ComponentWithDataSourceContext {
		
		private JdbcTemplate jdbcTemplate;
		private InnerComponentWithDataSourceContext innerComponent;
		
		public ComponentWithDataSourceContext(JdbcTemplate jdbcTemplate, InnerComponentWithDataSourceContext innerComponent) {
			this.jdbcTemplate = jdbcTemplate;
			this.innerComponent = innerComponent;
		}

		@WriteTransaction
		public void doSomething() {
			assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
			assertEquals(DataSourceType.REPO_BATCHING, DataSourceContextHolder.get());
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void beforeCommit(boolean readOnly) {
					assertEquals(DataSourceType.REPO_BATCHING, DataSourceContextHolder.get());
				}
				
				@Override
				public void afterCommit() {
					assertEquals(DataSourceType.REPO_BATCHING, DataSourceContextHolder.get());
				}
			});
			jdbcTemplate.queryForObject("SELECT 1", Long.class);
		}
		
		protected void doSomethingElse() {
			assertNull(DataSourceContextHolder.get());
		}
		
		@WriteTransaction
		public void doSomethingWithInnerComponent() {
			assertEquals(DataSourceType.REPO_BATCHING, DataSourceContextHolder.get());
			innerComponent.doSomething();
		}

	}
	
	@Component
	@DataSourceContext(DataSourceType.REPO)
	public static class InnerComponentWithDataSourceContext {
		
		public void doSomething() {
			
		}

	}

}
