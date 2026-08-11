package org.sagebionetworks.repo.model.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * Utility for auto-discovering DBO classes via classpath scanning. This
 * eliminates the need to manually maintain 200+ line lists of DBO
 * instantiations.
 */
public class DboAutoDiscovery {

	// @formatter:off
	private static final String[] DBO_PACKAGES = { 
			"org.sagebionetworks.repo.model.dbo.persistence",
			"org.sagebionetworks.repo.model.dbo.wikiV2",
			"org.sagebionetworks.repo.model.message",
			"org.sagebionetworks.repo.model.dbo.auth",
			"org.sagebionetworks.repo.model.dbo.asynch",
			"org.sagebionetworks.repo.model.dbo.dao.dataaccess",
			"org.sagebionetworks.repo.model.dbo.dao.files",
			"org.sagebionetworks.repo.model.dbo.dao.table",
			"org.sagebionetworks.repo.model.dbo.file",
			"org.sagebionetworks.repo.model.dbo.form",
			"org.sagebionetworks.repo.model.dbo.principal",
			"org.sagebionetworks.repo.model.dbo.schema",
			"org.sagebionetworks.repo.model.dbo.ses",
			"org.sagebionetworks.repo.model.dbo.statistics",
			"org.sagebionetworks.repo.model.dbo.throttle",
			"org.sagebionetworks.repo.model.dbo.trash",
			"org.sagebionetworks.repo.model.dbo.verification",
			"org.sagebionetworks.repo.model.dbo.webhook",
			"org.sagebionetworks.repo.model.dbo.feature",
			"org.sagebionetworks.repo.model.dbo.loginlockout",
			"org.sagebionetworks.repo.model.dbo.otp",
			"org.sagebionetworks.repo.model.dbo.agent",
			"org.sagebionetworks.repo.model.dbo.grid",
			"org.sagebionetworks.repo.model.dbo.limits",
			"org.sagebionetworks.repo.model.dbo.curation",
			"org.sagebionetworks.repo.model.dbo.portals",
			"org.sagebionetworks.repo.model.dbo.search",
			"org.sagebionetworks.evaluation.dbo"
	};
	// @formatter:on

	/**
	 * Discovers all DatabaseObject implementations via classpath scanning. Returns
	 * instantiated DBOs sorted by foreign key dependency order using topological sort
	 * on the actual DDL FOREIGN KEY constraints.
	 *
	 * This ensures tables are created in the correct order for foreign key dependencies.
	 */
	@SuppressWarnings("rawtypes")
	public static List<DatabaseObject> discoverAllDatabaseObjects() {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AssignableTypeFilter(DatabaseObject.class));

		List<DatabaseObject> dbos = new ArrayList<>();
		for (String basePackage : DBO_PACKAGES) {
			Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
			for (BeanDefinition bd : candidates) {
				try {
					Class<?> clazz = Class.forName(bd.getBeanClassName());
					// Skip abstract classes and interfaces
					if (!clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
						dbos.add((DatabaseObject) clazz.getDeclaredConstructor().newInstance());
					}
				} catch (Exception e) {
					throw new IllegalStateException("Failed to instantiate DBO: " + bd.getBeanClassName(), e);
				}
			}
		}
		// Sort by foreign key dependency order using topological sort on DDL
		return DboDependencyAnalyzer.sortByForeignKeyDependencies(dbos);
	}

	/**
	 * Discovers all PRIMARY MigratableDatabaseObject implementations and sorts them
	 * by MigrationType enum order. Primary types are those NOT listed in any other
	 * DBO's getSecondaryTypes(). Secondary types are discovered automatically via
	 * their owner's getSecondaryTypes() method. This ensures migration order
	 * matches the MigrationType enum declaration order.
	 */
	@SuppressWarnings("rawtypes")
	public static List<MigratableDatabaseObject> discoverPrimaryMigratableDatabaseObjects(
			List<DatabaseObject> alldbos) {
		// Get all MigrationType values in enum order
		List<MigrationType> migrationTypeOrder = Arrays.asList(MigrationType.values());

		// Discover all migratable DBOs
		List<MigratableDatabaseObject> allMigratableDbos = alldbos.stream()
				.filter(dbo -> dbo instanceof MigratableDatabaseObject).map(dbo -> (MigratableDatabaseObject) dbo)
				.collect(Collectors.toList());

		Stream<MigratableDatabaseObject> sStream = allMigratableDbos.stream().filter(d -> d.getSecondaryTypes() != null)
				.flatMap(d -> d.getSecondaryTypes().stream());
		Set<MigrationType> secondaryTypes = sStream.map(MigratableDatabaseObject::getMigratableTableType)
				.collect(Collectors.toSet());

		// Filter to only primary types (not in any getSecondaryTypes())
		List<MigratableDatabaseObject> primaryDbos = allMigratableDbos.stream()
				.filter(dbo -> !secondaryTypes.contains(dbo.getMigratableTableType())).collect(Collectors.toList());

		// Sort by MigrationType enum order
		primaryDbos.sort(Comparator.comparingInt(dbo -> {
			MigrationType type = dbo.getMigratableTableType();
			int index = migrationTypeOrder.indexOf(type);
			if (index == -1) {
				throw new IllegalStateException(
						"DBO has unregistered MigrationType: " + type + " for class " + dbo.getClass().getName());
			}
			return index;
		}));

		return primaryDbos;
	}
}
