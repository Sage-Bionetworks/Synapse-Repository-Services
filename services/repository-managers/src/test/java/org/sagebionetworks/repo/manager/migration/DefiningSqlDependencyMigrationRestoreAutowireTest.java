package org.sagebionetworks.repo.manager.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBODefiningSqlDependency;
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
 * {@code MATERIALIZED_VIEW}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DefiningSqlDependencyMigrationRestoreAutowireTest {

	@Autowired
	private BackupFileStream backupFileStream;

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

}
