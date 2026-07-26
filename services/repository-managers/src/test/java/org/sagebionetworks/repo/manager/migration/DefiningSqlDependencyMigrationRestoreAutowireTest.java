package org.sagebionetworks.repo.manager.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAO;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBODefiningSqlDependency;
import org.sagebionetworks.repo.model.migration.BackupManifest;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.TypeData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * TEMPORARY — remove together with the {@code DBODefiningSqlDependency} migration bridge once this
 * release reaches prod (see {@code DBODefiningSqlDependency}).
 * <p>
 * Empirical proof that a production backup produced <em>before</em> the materialized-view-source-table
 * rename restores into the generalized {@code DEFINING_SQL_DEPENDENCY} schema. The checked-in fixture
 * {@code MaterializedViewSourceTableLegacyBackup.zip} serializes the legacy field names
 * ({@code materializedViewId} / {@code materializedViewVersion}) and carries no {@code objectType},
 * exactly as a {@code develop}-branch backup would. Reading it through the real
 * {@link BackupFileStream} runs the registered translator (the migration-restore path at
 * {@code BackupFileStreamImpl#readFileFromStream}) and must populate {@code objectId} /
 * {@code objectVersion} from the legacy fields and default {@code objectType} to
 * {@code MATERIALIZED_VIEW}. Restoring the same fixture through {@link MigrationManager} additionally covers the
 * manifest-driven delete-by-range step.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DefiningSqlDependencyMigrationRestoreAutowireTest {

	private static final long LEGACY_OBJECT_ID = 555L;

	@Autowired
	private BackupFileStream backupFileStream;

	@Autowired
	private MigrationManager migrationManager;

	@Autowired
	private MigratableTableDAO migratableTableDao;

	@AfterEach
	public void after() {
		migratableTableDao.deleteByRange(migratableTableDao.getTypeData(MigrationType.MATERIALIZED_VIEW_SOURCE_TABLE),
				LEGACY_OBJECT_ID, LEGACY_OBJECT_ID);
		migratableTableDao.deleteByRange(migratableTableDao.getTypeData(MigrationType.MATERIALIZED_VIEW_ID),
				LEGACY_OBJECT_ID, LEGACY_OBJECT_ID);
	}

	@Test
	public void testReadLegacyMaterializedViewSourceTableBackup() {
		InputStream stream = getClass().getClassLoader()
				.getResourceAsStream("MaterializedViewSourceTableLegacyBackup.zip");

		// Call under test — reads the fixture and runs the registered translator.
		List<MigratableDatabaseObject<?, ?>> restored = new ArrayList<>();
		backupFileStream.readBackupFile(stream, BackupAliasType.MIGRATION_TYPE_NAME).forEach(restored::add);

		// The legacy row translates into the new object fields with a defaulted objectType.
		DBODefiningSqlDependency expected = new DBODefiningSqlDependency();
		expected.setObjectId(555L);
		expected.setObjectVersion(-1L);
		expected.setObjectType(ObjectType.MATERIALIZED_VIEW.name());
		expected.setSourceTableId(123L);
		expected.setSourceTableVersion(-1L);

		assertEquals(List.of(expected), restored);
	}

	/**
	 * The manifest a pre-rename production stack writes names its own backup id column,
	 * {@code MATERIALIZED_VIEW_ID}, which the renamed tables here no longer have. Restoring through the real entry
	 * point covers the delete-by-range step that the read-only test above bypasses.
	 */
	@Test
	public void testRestoreStreamWithLegacyManifest() {
		InputStream stream = getClass().getClassLoader()
				.getResourceAsStream("MaterializedViewSourceTableLegacyBackup.zip");

		BackupManifest manifest = new BackupManifest().setAliasType(BackupAliasType.MIGRATION_TYPE_NAME)
				.setBatchSize(100L).setMinimumId(LEGACY_OBJECT_ID).setMaximumId(LEGACY_OBJECT_ID)
				.setPrimaryType(new TypeData().setMigrationType(MigrationType.MATERIALIZED_VIEW_ID.name())
						.setBackupIdColumnName("MATERIALIZED_VIEW_ID"))
				.setSecondaryTypes(List.of(
						new TypeData().setMigrationType(MigrationType.MATERIALIZED_VIEW_SOURCE_TABLE.name())
								.setBackupIdColumnName("MATERIALIZED_VIEW_ID")));

		// Call under test
		long restoredRowCount = migrationManager.restoreStream(stream, manifest).getRestoredRowCount();

		assertEquals(1L, restoredRowCount);

		DBODefiningSqlDependency expected = new DBODefiningSqlDependency();
		expected.setObjectId(LEGACY_OBJECT_ID);
		expected.setObjectVersion(-1L);
		expected.setObjectType(ObjectType.MATERIALIZED_VIEW.name());
		expected.setSourceTableId(123L);
		expected.setSourceTableVersion(-1L);

		List<MigratableDatabaseObject<?, ?>> persisted = new ArrayList<>();
		migratableTableDao.streamDatabaseObjects(MigrationType.MATERIALIZED_VIEW_SOURCE_TABLE, LEGACY_OBJECT_ID,
				LEGACY_OBJECT_ID, 100L).forEach(persisted::add);

		assertEquals(List.of(expected), persisted);
	}

}
