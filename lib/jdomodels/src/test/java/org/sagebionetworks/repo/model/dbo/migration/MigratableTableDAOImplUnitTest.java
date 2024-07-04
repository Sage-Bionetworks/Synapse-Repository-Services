package org.sagebionetworks.repo.model.dbo.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Unit test for MigatableTableDAOImpl
 */
@ExtendWith(MockitoExtension.class)
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
	@BeforeEach
	public void before(){
		databaseObjectRegister = new ArrayList<MigratableDatabaseObject>();
		databaseObjectRegister.add(primary);
		databaseObjectRegister.add(primaryTwo);
		dao.setDatabaseObjectRegister(databaseObjectRegister);
	}
	
	@Test
	public void testNoAutoIncrement() {
		// Add an auto-increment class to the register
		StubAutoIncrement autoIncrement = new StubAutoIncrement();
		databaseObjectRegister.clear();
		databaseObjectRegister.add(autoIncrement);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {
			dao.initialize();
		}).getMessage();

		assertTrue(result.startsWith("AUTO_INCREMENT tables cannot be migrated."));

	}
	
	@Test
	public void testGetChecksumForIdRangeInvalidRange() {
		assertThrows(IllegalArgumentException.class, () -> {
			dao.getChecksumForIdRange(MigrationType.FILE_HANDLE, "SALT", 10, 9);
		});
	}

	
	@Test
	public void testMapSecondaryTablesToPrimaryGroups() {
		when(primaryMapping.getTableName()).thenReturn("primary_table_name");
		when(primary.getTableMapping()).thenReturn(primaryMapping);
		
		when(secondaryOneMapping.getTableName()).thenReturn("secondary_table_one_name");
		when(secondaryOne.getTableMapping()).thenReturn(secondaryOneMapping);
		
		when(secondaryTwoMapping.getTableName()).thenReturn("secondary_table_two_name");
		when(secondaryTwo.getTableMapping()).thenReturn(secondaryTwoMapping);
		// add both secondaries to the primary.
		when(primary.getSecondaryTypes()).thenReturn(Lists.newArrayList(secondaryOne, secondaryTwo));
		// This has no secondaries.
		when(primaryTwo.getSecondaryTypes()).thenReturn(null);
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
}
