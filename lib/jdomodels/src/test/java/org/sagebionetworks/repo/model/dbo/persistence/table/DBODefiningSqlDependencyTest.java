package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.util.json.JavaJSONUtil;

public class DBODefiningSqlDependencyTest {

	private final MigratableTableTranslation<DBODefiningSqlDependency, DBODefiningSqlDependency> translator = new DBODefiningSqlDependency()
			.getTranslator();

	@Test
	public void testCreateDatabaseObjectFromBackupWithLegacyMaterializedViewFields() {
		// A backup from before the rename: only the legacy materializedView* fields and no objectType.
		DBODefiningSqlDependency backup = new DBODefiningSqlDependency();
		backup.setMaterializedViewId(123L);
		backup.setMaterializedViewVersion(2L);
		backup.setSourceTableId(456L);
		backup.setSourceTableVersion(-1L);

		// Call under test
		DBODefiningSqlDependency result = translator.createDatabaseObjectFromBackup(backup);

		assertEquals(123L, result.getObjectId());
		assertEquals(2L, result.getObjectVersion());
		assertEquals(ObjectType.MATERIALIZED_VIEW.name(), result.getObjectType());
		assertEquals(456L, result.getSourceTableId());
		assertEquals(-1L, result.getSourceTableVersion());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupDefaultsObjectTypeWhenNull() {
		// A row already using the new object fields but predating the objectType column defaults to MV.
		DBODefiningSqlDependency backup = new DBODefiningSqlDependency();
		backup.setObjectId(123L);
		backup.setObjectVersion(1L);
		backup.setObjectType(null);
		backup.setSourceTableId(456L);
		backup.setSourceTableVersion(-1L);

		// Call under test
		DBODefiningSqlDependency result = translator.createDatabaseObjectFromBackup(backup);

		assertEquals(ObjectType.MATERIALIZED_VIEW.name(), result.getObjectType());
		// The new fields are preserved and the legacy bridge is not consulted.
		assertEquals(123L, result.getObjectId());
		assertEquals(1L, result.getObjectVersion());
		assertNull(result.getMaterializedViewId());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupPreservesExplicitObjectType() {
		// A new-format search index backup keeps its own objectType and object fields untouched.
		DBODefiningSqlDependency backup = new DBODefiningSqlDependency();
		backup.setObjectId(789L);
		backup.setObjectVersion(-1L);
		backup.setObjectType(ObjectType.SEARCH_INDEX.name());
		backup.setSourceTableId(456L);
		backup.setSourceTableVersion(3L);

		// Call under test
		DBODefiningSqlDependency result = translator.createDatabaseObjectFromBackup(backup);

		assertEquals(789L, result.getObjectId());
		assertEquals(-1L, result.getObjectVersion());
		assertEquals(ObjectType.SEARCH_INDEX.name(), result.getObjectType());
		assertEquals(456L, result.getSourceTableId());
		assertEquals(3L, result.getSourceTableVersion());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupThrowsWhenBothObjectIdsPopulated() {
		// The temporary bridge must be removed once production serializes the new column names: if a
		// backup carries both the new objectId and the legacy materializedViewId, the bridge is stale.
		DBODefiningSqlDependency backup = new DBODefiningSqlDependency();
		backup.setObjectId(123L);
		backup.setMaterializedViewId(123L);
		backup.setSourceTableId(456L);
		backup.setSourceTableVersion(-1L);

		String message = assertThrows(IllegalStateException.class, () -> {
			// Call under test
			translator.createDatabaseObjectFromBackup(backup);
		}).getMessage();

		assertEquals(
				"Both objectId and the legacy materializedViewId are populated; the temporary migration bridge must be removed.",
				message);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupThrowsWhenBothObjectVersionsPopulated() {
		DBODefiningSqlDependency backup = new DBODefiningSqlDependency();
		backup.setObjectId(123L);
		backup.setObjectVersion(1L);
		backup.setMaterializedViewVersion(1L);
		backup.setSourceTableId(456L);
		backup.setSourceTableVersion(-1L);

		String message = assertThrows(IllegalStateException.class, () -> {
			// Call under test
			translator.createDatabaseObjectFromBackup(backup);
		}).getMessage();

		assertEquals(
				"Both objectVersion and the legacy materializedViewVersion are populated; the temporary migration bridge must be removed.",
				message);
	}

	@Test
	public void testMigrationRestoreFromLegacyBackupJson() {
		// End-to-end proof of the migration-restore path for a backup produced BEFORE the rename:
		// the JSON carries only the legacy field names and no objectType, exactly as a develop-branch
		// backup serializes it. JavaJSONUtil is the same deserializer the migration stream uses.
		String legacyBackupJson = "[{"
				+ "\"materializedViewId\": 555,"
				+ "\"materializedViewVersion\": -1,"
				+ "\"sourceTableId\": 123,"
				+ "\"sourceTableVersion\": -1"
				+ "}]";

		List<DBODefiningSqlDependency> backupObjects = JavaJSONUtil
				.streamFromJSONArray(DBODefiningSqlDependency.class, new StringReader(legacyBackupJson));

		// Call under test — deserialized backup runs through the registered translator.
		DBODefiningSqlDependency result = translator.createDatabaseObjectFromBackup(backupObjects.get(0));

		DBODefiningSqlDependency expected = new DBODefiningSqlDependency();
		expected.setObjectId(555L);
		expected.setObjectVersion(-1L);
		expected.setObjectType(ObjectType.MATERIALIZED_VIEW.name());
		expected.setSourceTableId(123L);
		expected.setSourceTableVersion(-1L);

		assertEquals(expected, result);
	}

}
