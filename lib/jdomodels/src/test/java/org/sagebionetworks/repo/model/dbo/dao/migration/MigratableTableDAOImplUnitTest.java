package org.sagebionetworks.repo.model.dbo.dao.migration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.dbo.AutoIncrementDatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAOImpl;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Unit test for MigatableTableDAOImpl
 */
public class MigratableTableDAOImplUnitTest {

	private MigratableTableDAOImpl dao;
	private SimpleJdbcTemplate mockSimpleJdbcTemplate;
	@SuppressWarnings("rawtypes")
	private List<MigratableDatabaseObject> databaseObjectRegister;
	
	@SuppressWarnings("rawtypes")
	@Before
	public void before(){
		mockSimpleJdbcTemplate = Mockito.mock(SimpleJdbcTemplate.class);
		databaseObjectRegister = new ArrayList<MigratableDatabaseObject>();
		dao = new MigratableTableDAOImpl(mockSimpleJdbcTemplate, databaseObjectRegister);
	}
	
	@Test
	public void testNoAutoIncrement(){
		// Add an auto-increment class to the register
		StubAutoIncrement autoIncrement = new StubAutoIncrement();
		databaseObjectRegister.add(autoIncrement);
		try {
			dao.initialize();
			fail("Should have failed since an AUTO_INCREMENT table was registered");
		} catch (IllegalArgumentException e){
			// expected
			assertTrue(e.getMessage().startsWith("AUTO_INCREMENT tables cannot be migrated."));
		}

	}
	
	public static class StubAutoIncrement implements MigratableDatabaseObject<StubAutoIncrement, StubAutoIncrement>, AutoIncrementDatabaseObject<StubAutoIncrement>{

		@Override
		public TableMapping<StubAutoIncrement> getTableMapping() {
			return null;
		}

		@Override
		public Long getId() {
			return null;
		}

		@Override
		public void setId(Long id) { }

		@Override
		public MigrationType getMigratableTableType() {
			return null;
		}

		@Override
		public MigratableTableTranslation<StubAutoIncrement, StubAutoIncrement> getTranslator() {
			return null;
		}

		@Override
		public Class<? extends StubAutoIncrement> getBackupClass() {
			return null;
		}

		@Override
		public Class<? extends StubAutoIncrement> getDatabaseObjectClass() {
			return null;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public List<MigratableDatabaseObject> getSecondaryTypes() {
			return null;
		}
		
	}
}
