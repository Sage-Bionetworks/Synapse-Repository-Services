package org.sagebionetworks.repo.model.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * Analyzes DDL statements to determine foreign key dependencies between database tables.
 * Performs topological sort to ensure tables are created in the correct order.
 */
public class DboDependencyAnalyzer {

	// Pattern to extract FOREIGN KEY constraints from DDL
	// Matches: FOREIGN KEY (`COLUMN`) REFERENCES `TABLE_NAME` (`PK_COLUMN`)
	private static final Pattern FK_PATTERN = Pattern.compile(
			"FOREIGN\\s+KEY\\s+.*?REFERENCES\\s+`?([a-zA-Z0-9_]+)`?\\s*\\(",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private static final DdlFileReader DEFAULT_READER = new ClasspathDdlFileReader();

	/**
	 * Sorts DBOs by foreign key dependency order using topological sort.
	 * Tables with no dependencies come first, followed by tables that depend on them.
	 */
	@SuppressWarnings("rawtypes")
	public static List<DatabaseObject> sortByForeignKeyDependencies(List<DatabaseObject> dbos) {
		return sortByForeignKeyDependencies(dbos, DEFAULT_READER);
	}

	/**
	 * Sorts DBOs by foreign key dependency order using topological sort.
	 * Package-private for testing with a custom DDL reader.
	 *
	 * @param dbos Database objects to sort
	 * @param ddlReader DDL file reader (allows mocking in tests)
	 * @return Sorted list of database objects
	 */
	@SuppressWarnings("rawtypes")
	static List<DatabaseObject> sortByForeignKeyDependencies(List<DatabaseObject> dbos, DdlFileReader ddlReader) {
		// Build dependency map: table name -> set of tables it depends on
		Map<String, Set<String>> dependencies = new HashMap<>();
		Map<String, DatabaseObject> nameToDbo = new HashMap<>();

		for (DatabaseObject dbo : dbos) {
			TableMapping mapping = dbo.getTableMapping();
			String tableName = mapping.getTableName();
			nameToDbo.put(tableName, dbo);

			Set<String> deps = extractDependencies(mapping, ddlReader);
			dependencies.put(tableName, deps);
		}

		// Topological sort
		List<DatabaseObject> sorted = new ArrayList<>();
		Set<String> visited = new HashSet<>();
		Set<String> visiting = new HashSet<>();

		for (String tableName : dependencies.keySet()) {
			if (!visited.contains(tableName)) {
				topologicalSort(tableName, dependencies, visited, visiting, sorted, nameToDbo);
			}
		}

		return sorted;
	}

	/**
	 * Extracts foreign key dependencies from a table's DDL statement.
	 */
	@SuppressWarnings("rawtypes")
	private static Set<String> extractDependencies(TableMapping mapping, DdlFileReader ddlReader) {
		Set<String> deps = new HashSet<>();
		String ddl = ddlReader.readDdl(mapping.getDDLFileName());
		if (ddl == null) {
			return deps;
		}

		Matcher matcher = FK_PATTERN.matcher(ddl);
		while (matcher.find()) {
			String referencedTable = matcher.group(1);
			// Ignore self-references (e.g., NODE -> PARENT_NODE)
			if (!referencedTable.equals(mapping.getTableName())) {
				deps.add(referencedTable);
			}
		}

		return deps;
	}

	/**
	 * Performs depth-first topological sort.
	 */
	@SuppressWarnings("rawtypes")
	private static void topologicalSort(String tableName, Map<String, Set<String>> dependencies,
			Set<String> visited, Set<String> visiting, List<DatabaseObject> sorted,
			Map<String, DatabaseObject> nameToDbo) {

		if (visited.contains(tableName)) {
			return;
		}

		if (visiting.contains(tableName)) {
			// Circular dependency detected - this is OK for self-references
			// Just skip and let it be added when we return from recursion
			return;
		}

		visiting.add(tableName);

		Set<String> deps = dependencies.getOrDefault(tableName, new HashSet<>());
		for (String dep : deps) {
			// Only recurse if the dependency is in our DBO set
			if (dependencies.containsKey(dep)) {
				topologicalSort(dep, dependencies, visited, visiting, sorted, nameToDbo);
			}
		}

		visiting.remove(tableName);
		visited.add(tableName);

		// Add to sorted list (dependencies first, then this table)
		DatabaseObject dbo = nameToDbo.get(tableName);
		if (dbo != null) {
			sorted.add(dbo);
		}
	}
}
