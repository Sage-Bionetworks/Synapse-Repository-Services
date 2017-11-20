package org.sagebionetworks.repo.model.dbo.dao.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.dbo.AutoIncrementDatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAOImpl;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Unit test for MigatableTableDAOImpl
 */
@RunWith(MockitoJUnitRunner.class)
public class MigratableTableDAOImplUnitTest {

	private MigratableTableDAOImpl dao;
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	@SuppressWarnings("rawtypes")
	private List<MigratableDatabaseObject> databaseObjectRegister;
	
	@Mock
	MigratableDatabaseObject primary;
	@Mock
	TableMapping primaryMapping;
	
	@Mock
	MigratableDatabaseObject primaryTwo;
	@Mock
	TableMapping primaryMappingTwo;
	
	@Mock
	MigratableDatabaseObject secondaryOne;
	@Mock
	TableMapping secondaryOneMapping;
	@Mock
	MigratableDatabaseObject secondaryTwo;
	@Mock
	TableMapping secondaryTwoMapping;
	
	List<MigratableDatabaseObject> mockRegistredObjects;
	
	@SuppressWarnings("rawtypes")
	@Before
	public void before(){
		mockJdbcTemplate = Mockito.mock(JdbcTemplate.class);
		databaseObjectRegister = new ArrayList<MigratableDatabaseObject>();
		dao = new MigratableTableDAOImpl(mockJdbcTemplate, databaseObjectRegister);
		
		when(primaryMapping.getTableName()).thenReturn("primary_table_name");
		when(primary.getTableMapping()).thenReturn(primaryMapping);
		
		when(secondaryOneMapping.getTableName()).thenReturn("secondary_table_one_name");
		when(secondaryOne.getTableMapping()).thenReturn(secondaryOneMapping);
		
		when(secondaryTwoMapping.getTableName()).thenReturn("secondary_table_two_name");
		when(secondaryTwo.getTableMapping()).thenReturn(secondaryTwoMapping);
		// add both secondaries to the primary.
		when(primary.getSecondaryTypes()).thenReturn(Lists.newArrayList(secondaryOne, secondaryTwo));
		

		when(primaryMappingTwo.getTableName()).thenReturn("primary_two_table_name");
		when(primaryTwo.getTableMapping()).thenReturn(primaryMappingTwo);
		// This has no secondaries.
		when(primaryTwo.getSecondaryTypes()).thenReturn(null);
		
		
		databaseObjectRegister.add(primary);
		databaseObjectRegister.add(primaryTwo);
	}
	
	@Test
	public void testNoAutoIncrement() {
		// Add an auto-increment class to the register
		StubAutoIncrement autoIncrement = new StubAutoIncrement();
		databaseObjectRegister.clear();
		databaseObjectRegister.add(autoIncrement);
		try {
			dao.initialize();
			fail("Should have failed since an AUTO_INCREMENT table was registered");
		} catch (IllegalArgumentException e){
			// expected
			assertTrue(e.getMessage().startsWith("AUTO_INCREMENT tables cannot be migrated."));
		}

	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetChecksumForIdRangeInvalidRange() {
		dao.getChecksumForIdRange(MigrationType.FILE_HANDLE, "SALT", 10, 9);
	}

	
	@Test
	public void testMapSecondaryTablesToPrimaryGroups() {
		// call under test
		Map<String, Set<String>> results = dao.mapSecondaryTablesToPrimaryGroups();
		assertNotNull(results);
		assertEquals(2, results.size());
		// All tables
		Set<String> expectedGroup = Sets.newHashSet(
				secondaryOneMapping.getTableName().toUpperCase(),
				secondaryTwoMapping.getTableName().toUpperCase(),
				primaryMapping.getTableName().toUpperCase());
		Set<String> oneGroup = results.get(secondaryOneMapping.getTableName().toUpperCase());
		assertEquals(expectedGroup, oneGroup);
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

		@Override
		public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
			return null;
		}
		
	}
}
