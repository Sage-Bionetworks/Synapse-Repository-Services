package org.sagebionetworks.repo.model.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;

@ExtendWith(MockitoExtension.class)
public class DboDependencyAnalyzerTest {

	@Mock
	private DdlFileReader mockDdlReader;

	@Test
	public void testSortByForeignKeyDependenciesWithNoDependencies() {
		// Setup: Three tables with no FK dependencies
		DatabaseObject<?> table1 = createMockDBO("TABLE_A", "ddl_a.sql");
		DatabaseObject<?> table2 = createMockDBO("TABLE_B", "ddl_b.sql");
		DatabaseObject<?> table3 = createMockDBO("TABLE_C", "ddl_c.sql");

		when(mockDdlReader.readDdl("ddl_a.sql")).thenReturn("CREATE TABLE TABLE_A (ID BIGINT PRIMARY KEY)");
		when(mockDdlReader.readDdl("ddl_b.sql")).thenReturn("CREATE TABLE TABLE_B (ID BIGINT PRIMARY KEY)");
		when(mockDdlReader.readDdl("ddl_c.sql")).thenReturn("CREATE TABLE TABLE_C (ID BIGINT PRIMARY KEY)");

		// call under test
		List<DatabaseObject> result = DboDependencyAnalyzer.sortByForeignKeyDependencies(
				Arrays.asList(table1, table2, table3), mockDdlReader);

		// All tables should be returned (order doesn't matter when there are no dependencies)
		assertEquals(3, result.size());
	}

	@Test
	public void testSortByForeignKeyDependenciesWithLinearChain() {
		// Setup: C depends on B, B depends on A
		DatabaseObject<?> tableA = createMockDBO("TABLE_A", "ddl_a.sql");
		DatabaseObject<?> tableB = createMockDBO("TABLE_B", "ddl_b.sql");
		DatabaseObject<?> tableC = createMockDBO("TABLE_C", "ddl_c.sql");

		when(mockDdlReader.readDdl("ddl_a.sql")).thenReturn("CREATE TABLE TABLE_A (ID BIGINT PRIMARY KEY)");
		when(mockDdlReader.readDdl("ddl_b.sql")).thenReturn(
				"CREATE TABLE TABLE_B (ID BIGINT PRIMARY KEY, A_ID BIGINT, " +
				"FOREIGN KEY (A_ID) REFERENCES TABLE_A (ID))");
		when(mockDdlReader.readDdl("ddl_c.sql")).thenReturn(
				"CREATE TABLE TABLE_C (ID BIGINT PRIMARY KEY, B_ID BIGINT, " +
				"FOREIGN KEY (B_ID) REFERENCES TABLE_B (ID))");

		// call under test (pass in reverse order to ensure sorting works)
		List<DatabaseObject> result = DboDependencyAnalyzer.sortByForeignKeyDependencies(
				Arrays.asList(tableC, tableB, tableA), mockDdlReader);

		// Should be sorted: A first, then B, then C
		assertEquals(3, result.size());
		assertEquals("TABLE_A", result.get(0).getTableMapping().getTableName());
		assertEquals("TABLE_B", result.get(1).getTableMapping().getTableName());
		assertEquals("TABLE_C", result.get(2).getTableMapping().getTableName());
	}

	@Test
	public void testSortByForeignKeyDependenciesWithMultipleDependencies() {
		// Setup: D depends on both B and C, B and C depend on A
		DatabaseObject<?> tableA = createMockDBO("TABLE_A", "ddl_a.sql");
		DatabaseObject<?> tableB = createMockDBO("TABLE_B", "ddl_b.sql");
		DatabaseObject<?> tableC = createMockDBO("TABLE_C", "ddl_c.sql");
		DatabaseObject<?> tableD = createMockDBO("TABLE_D", "ddl_d.sql");

		when(mockDdlReader.readDdl("ddl_a.sql")).thenReturn("CREATE TABLE TABLE_A (ID BIGINT PRIMARY KEY)");
		when(mockDdlReader.readDdl("ddl_b.sql")).thenReturn(
				"CREATE TABLE TABLE_B (ID BIGINT PRIMARY KEY, A_ID BIGINT, " +
				"FOREIGN KEY (A_ID) REFERENCES TABLE_A (ID))");
		when(mockDdlReader.readDdl("ddl_c.sql")).thenReturn(
				"CREATE TABLE TABLE_C (ID BIGINT PRIMARY KEY, A_ID BIGINT, " +
				"FOREIGN KEY (A_ID) REFERENCES TABLE_A (ID))");
		when(mockDdlReader.readDdl("ddl_d.sql")).thenReturn(
				"CREATE TABLE TABLE_D (ID BIGINT PRIMARY KEY, B_ID BIGINT, C_ID BIGINT, " +
				"FOREIGN KEY (B_ID) REFERENCES TABLE_B (ID), " +
				"FOREIGN KEY (C_ID) REFERENCES TABLE_C (ID))");

		// call under test
		List<DatabaseObject> result = DboDependencyAnalyzer.sortByForeignKeyDependencies(
				Arrays.asList(tableD, tableC, tableB, tableA), mockDdlReader);

		// A must come first, B and C can be in any order, D must come last
		assertEquals(4, result.size());
		assertEquals("TABLE_A", result.get(0).getTableMapping().getTableName());

		// B and C can be in either order
		String second = result.get(1).getTableMapping().getTableName();
		String third = result.get(2).getTableMapping().getTableName();
		assertEquals(true, (second.equals("TABLE_B") && third.equals("TABLE_C")) ||
				           (second.equals("TABLE_C") && third.equals("TABLE_B")));

		assertEquals("TABLE_D", result.get(3).getTableMapping().getTableName());
	}

	@Test
	public void testSortByForeignKeyDependenciesWithSelfReference() {
		// Setup: TABLE_NODE has a self-reference (parent node FK)
		DatabaseObject<?> tableNode = createMockDBO("NODE", "ddl_node.sql");

		when(mockDdlReader.readDdl("ddl_node.sql")).thenReturn(
				"CREATE TABLE NODE (ID BIGINT PRIMARY KEY, PARENT_ID BIGINT, " +
				"FOREIGN KEY (PARENT_ID) REFERENCES NODE (ID))");

		// call under test
		List<DatabaseObject> result = DboDependencyAnalyzer.sortByForeignKeyDependencies(
				Arrays.asList(tableNode), mockDdlReader);

		// Self-reference should be ignored, table should be in result
		assertEquals(1, result.size());
		assertEquals("NODE", result.get(0).getTableMapping().getTableName());
	}

	@Test
	public void testSortByForeignKeyDependenciesWithMissingDdlFile() {
		// Setup: Table with DDL file that doesn't exist
		DatabaseObject<?> table = createMockDBO("TABLE_A", "missing.sql");

		when(mockDdlReader.readDdl("missing.sql")).thenReturn(null);

		// call under test
		List<DatabaseObject> result = DboDependencyAnalyzer.sortByForeignKeyDependencies(
				Arrays.asList(table), mockDdlReader);

		// Should still include the table (assumes no dependencies)
		assertEquals(1, result.size());
		assertEquals("TABLE_A", result.get(0).getTableMapping().getTableName());
	}

	@Test
	public void testSortByForeignKeyDependenciesWithExternalDependency() {
		// Setup: Table B depends on TABLE_EXTERNAL which is not in our DBO list
		DatabaseObject<?> tableB = createMockDBO("TABLE_B", "ddl_b.sql");

		when(mockDdlReader.readDdl("ddl_b.sql")).thenReturn(
				"CREATE TABLE TABLE_B (ID BIGINT PRIMARY KEY, EXT_ID BIGINT, " +
				"FOREIGN KEY (EXT_ID) REFERENCES TABLE_EXTERNAL (ID))");

		// call under test
		List<DatabaseObject> result = DboDependencyAnalyzer.sortByForeignKeyDependencies(
				Arrays.asList(tableB), mockDdlReader);

		// Should handle external dependency gracefully
		assertEquals(1, result.size());
		assertEquals("TABLE_B", result.get(0).getTableMapping().getTableName());
	}

	/**
	 * Helper method to create a mock DatabaseObject with a TableMapping.
	 * Only stubs the methods that are always called by the analyzer.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private DatabaseObject<?> createMockDBO(String tableName, String ddlFileName) {
		DatabaseObject mockDBO = mock(DatabaseObject.class);
		TableMapping mockMapping = mock(TableMapping.class);

		when(mockMapping.getTableName()).thenReturn(tableName);
		when(mockMapping.getDDLFileName()).thenReturn(ddlFileName);
		when(mockDBO.getTableMapping()).thenReturn(mockMapping);

		return mockDBO;
	}
}
