package org.sagebionetworks.repo.model.dbo.migration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;

@Deprecated
public class MigrationXStreamBuilder {

	public static Map<BackupAliasType, UnmodifiableXStream> buildXStream(
			List<MigratableDatabaseObject> databaseObjectRegister) {
		// create maps for alias type to xstream
		UnmodifiableXStream.Builder tableNameXStreamBuilder = UnmodifiableXStream.builder();
		tableNameXStreamBuilder.allowTypeHierarchy(MigratableDatabaseObject.class);
		UnmodifiableXStream.Builder migrationTypeNameXStreamBuilder = UnmodifiableXStream.builder();
		migrationTypeNameXStreamBuilder.allowTypeHierarchy(MigratableDatabaseObject.class);

		Function<MigratableDatabaseObject<?, ?>, String> tableAliasProvider = (dbo) -> dbo.getTableMapping()
				.getTableName();
		Function<MigratableDatabaseObject<?, ?>, String> typeAliasProvider = (dbo) -> dbo.getMigratableTableType()
				.name();

		for (MigratableDatabaseObject<?, ?> dbo : databaseObjectRegister) {
			// Add aliases to XStream for each alias type
			// BackupAliasType.TABLE_NAME
			addAlias(tableNameXStreamBuilder, dbo, tableAliasProvider);
			// BackupAliasType.MIGRATION_TYPE_NAME
			addAlias(migrationTypeNameXStreamBuilder, dbo, typeAliasProvider);
		}
		return Map.of(BackupAliasType.TABLE_NAME, tableNameXStreamBuilder.build(),
				BackupAliasType.MIGRATION_TYPE_NAME, migrationTypeNameXStreamBuilder.build());
	}

	static void addAlias(UnmodifiableXStream.Builder streamBuilder, MigratableDatabaseObject<?, ?> dbo,
			Function<MigratableDatabaseObject<?, ?>, String> aliasProvider) {
		streamBuilder.alias(aliasProvider.apply(dbo), dbo.getBackupClass());
		// Also add the alias for the secondary objects
		if (dbo.getSecondaryTypes() != null) {
			dbo.getSecondaryTypes().forEach(secondaryType -> {
				addAlias(streamBuilder, secondaryType, aliasProvider);
			});
		}
	}
}
