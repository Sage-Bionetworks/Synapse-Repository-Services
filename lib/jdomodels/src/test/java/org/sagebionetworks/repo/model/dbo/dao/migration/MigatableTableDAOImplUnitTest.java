package org.sagebionetworks.repo.model.dbo.dao.migration;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.dbo.AutoIncrementDatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigatableTableDAOImpl;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Unit test for MigatableTableDAOImpl.
 *
 */
public class MigatableTableDAOImplUnitTest {

	MigatableTableDAOImpl dao;
	SimpleJdbcTemplate mockSimpleJdbcTemplate;
	List<MigratableDatabaseObject> databaseObjectRegister;
	
	@Before
	public void before(){
		mockSimpleJdbcTemplate = Mockito.mock(SimpleJdbcTemplate.class);
		databaseObjectRegister = new ArrayList<MigratableDatabaseObject>();
		dao = new MigatableTableDAOImpl(mockSimpleJdbcTemplate, databaseObjectRegister, 10);
	}
	
	@Test
	public void testNoAutoIncrement(){
		// Add an auto-increment class to the register
		StubAutoIncrement autoIncrement = new StubAutoIncrement();
		databaseObjectRegister.add(autoIncrement);
		try{
			dao.initialize();
			fail("Should have failed since an AUTO_INCREMENT table was registered");
		}catch (IllegalArgumentException e){
			// expected
			assertTrue(e.getMessage().startsWith("AUTO_INCREMENT tables cannot be migrated."));
		}

	}
	
	public static class StubAutoIncrement implements MigratableDatabaseObject<StubAutoIncrement, StubAutoIncrement>, AutoIncrementDatabaseObject<StubAutoIncrement>{

		@Override
		public TableMapping<StubAutoIncrement> getTableMapping() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Long getId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setId(Long id) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public MigrationType getMigratableTableType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MigratableTableTranslation<StubAutoIncrement, StubAutoIncrement> getTranslator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Class<? extends StubAutoIncrement> getBackupClass() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Class<? extends StubAutoIncrement> getDatabaseObjectClass() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<MigratableDatabaseObject> getSecondaryTypes() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
