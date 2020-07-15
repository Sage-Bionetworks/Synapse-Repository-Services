package org.sagebionetworks.repo.model.dbo.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.AutoIncrementDatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Unit test for MigatableTableDAOImpl
 */
@RunWith(MockitoJUnitRunner.class)
public class MigratableTableDAOImplUnitTest {

	@InjectMocks
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

	static final String STUB_TABLE_NAME = "STUB";
	
	@SuppressWarnings("rawtypes")
	@Before
	public void before(){
		databaseObjectRegister = new ArrayList<MigratableDatabaseObject>();
		dao.setDatabaseObjectRegister(databaseObjectRegister);
		
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


	/**
	 * The only way to test that the initialize method worked is to actually use the XStream object since there is
	 * currently no way to interrogate the XStream object about what aliases it currently uses
	 */
	@Test
	public void testInitializeAliasTypeToXStreamMap(){
		StubAutoIncrement stubAutoIncrement = new StubAutoIncrement();
		MigratableTableDAOImpl.initializeAliasTypeToXStreamMap(Collections.singletonList(stubAutoIncrement));
		UnmodifiableXStream tableNameXStream = dao.getXStream(BackupAliasType.TABLE_NAME);
		UnmodifiableXStream migrationTypeNameXStream = dao.getXStream(BackupAliasType.MIGRATION_TYPE_NAME);

		assertNotSame(tableNameXStream, migrationTypeNameXStream);

		StringWriter tableNameXMLStringWriter = new StringWriter();
		StringWriter migrationTypeXMLStringWriter = new StringWriter();

		tableNameXStream.toXML(stubAutoIncrement, tableNameXMLStringWriter);
		migrationTypeNameXStream.toXML(stubAutoIncrement, migrationTypeXMLStringWriter);

		assertNotEquals(tableNameXMLStringWriter.toString(), migrationTypeNameXStream.toString());
		assertTrue(tableNameXMLStringWriter.toString().contains("STUB"));
		// gave stub fake migration type of VIEW_SCOPE to the stub class.
		// double underscore is because of the way XSTREAM uses to escape underscores:
		// http://x-stream.github.io/faq.html#XML_double_underscores
		assertTrue(migrationTypeXMLStringWriter.toString().contains("VIEW__SCOPE"));
	}
	
	@Test
	public void testInitializeAliasTypeToXStreamMapWithSecondaryTypes(){
		PrimaryClass primaryClass = new PrimaryClass();
		MigratableTableDAOImpl.initializeAliasTypeToXStreamMap(Collections.singletonList(primaryClass));
		
		UnmodifiableXStream tableNameXStream = dao.getXStream(BackupAliasType.TABLE_NAME);
		UnmodifiableXStream migrationTypeNameXStream = dao.getXStream(BackupAliasType.MIGRATION_TYPE_NAME);

		assertNotSame(tableNameXStream, migrationTypeNameXStream);

		StringWriter tableNameXMLStringWriter = new StringWriter();
		StringWriter migrationTypeXMLStringWriter = new StringWriter();
		
		SecondaryClass secondaryClass = new SecondaryClass();

		tableNameXStream.toXML(secondaryClass, tableNameXMLStringWriter);
		migrationTypeNameXStream.toXML(secondaryClass, migrationTypeXMLStringWriter);
		
		String tableXML = tableNameXMLStringWriter.toString();
		String typeXML = migrationTypeXMLStringWriter.toString();

		assertEquals("<" +secondaryClass.getTableMapping().getTableName().replaceAll("_", "__")+ "/>", tableXML);
		assertEquals("<" +secondaryClass.getMigratableTableType().name().replaceAll("_", "__")+ "/>", typeXML);

	}
	
	public static class StubAutoIncrement implements MigratableDatabaseObject<StubAutoIncrement, StubAutoIncrement>, AutoIncrementDatabaseObject<StubAutoIncrement>{
		
		@Override
		public TableMapping<StubAutoIncrement> getTableMapping() {
			return new TableMapping<StubAutoIncrement>() {
				@Override
				public String getTableName() {
					return STUB_TABLE_NAME;
				}

				@Override
				public String getDDLFileName() {
					return null;
				}

				@Override
				public FieldColumn[] getFieldColumns() {
					return new FieldColumn[0];
				}

				@Override
				public Class<? extends StubAutoIncrement> getDBOClass() {
					return null;
				}

				@Override
				public StubAutoIncrement mapRow(ResultSet resultSet, int i) throws SQLException {
					return null;
				}
			};
		}

		@Override
		public Long getId() {
			return null;
		}

		@Override
		public void setId(Long id) { }

		@Override
		public MigrationType getMigratableTableType() {
			return MigrationType.VIEW_SCOPE;
		}

		@Override
		public MigratableTableTranslation<StubAutoIncrement, StubAutoIncrement> getTranslator() {
			return null;
		}

		@Override
		public Class<? extends StubAutoIncrement> getBackupClass() {
			return StubAutoIncrement.class;
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
