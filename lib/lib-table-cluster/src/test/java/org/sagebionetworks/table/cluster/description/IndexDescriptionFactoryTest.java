package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;

public class IndexDescriptionFactoryTest {

	private final IdAndVersion idAndVersion = IdAndVersion.parse("syn123");

	// A defining-SQL type resolves the sources its SQL names; every source here is a plain table.
	private final IndexDescriptionLookup lookup = TableIndexDescription::new;

	@Test
	public void testCreateIndexDescriptionWithTable() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.table, null, 7L);

		// call under test
		IndexDescription result = IndexDescriptionFactory.createIndexDescription(state, lookup);

		assertEquals(new TableIndexDescription(idAndVersion, 7L), result);
	}

	@Test
	public void testCreateIndexDescriptionWithTableAndNoIndexVersion() {
		// A table captured before its first build has no index version yet.
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.table, null, null);

		// call under test
		IndexDescription result = IndexDescriptionFactory.createIndexDescription(state, lookup);

		assertEquals(new TableIndexDescription(idAndVersion, null), result);
	}

	@Test
	public void testCreateIndexDescriptionWithRecordSet() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.recordset, null, 7L);

		// call under test
		IndexDescription result = IndexDescriptionFactory.createIndexDescription(state, lookup);

		assertEquals(new RecordSetIndexDescription(idAndVersion, 7L), result);
	}

	@Test
	public void testCreateIndexDescriptionWithEntityView() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.entityview, null, 7L);

		// call under test
		IndexDescription result = IndexDescriptionFactory.createIndexDescription(state, lookup);

		assertEquals(new ViewIndexDescription(idAndVersion, TableType.entityview, 7L), result);
	}

	@Test
	public void testCreateIndexDescriptionWithSubmissionView() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.submissionview, null, 7L);

		// call under test
		IndexDescription result = IndexDescriptionFactory.createIndexDescription(state, lookup);

		assertEquals(new ViewIndexDescription(idAndVersion, TableType.submissionview, 7L), result);
	}

	@Test
	public void testCreateIndexDescriptionWithDataset() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.dataset, null, 7L);

		// call under test
		IndexDescription result = IndexDescriptionFactory.createIndexDescription(state, lookup);

		assertEquals(new ViewIndexDescription(idAndVersion, TableType.dataset, 7L), result);
	}

	@Test
	public void testCreateIndexDescriptionWithDatasetCollection() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.datasetcollection, null, 7L);

		// call under test
		IndexDescription result = IndexDescriptionFactory.createIndexDescription(state, lookup);

		assertEquals(new ViewIndexDescription(idAndVersion, TableType.datasetcollection, 7L), result);
	}

	@Test
	public void testCreateIndexDescriptionWithMaterializedView() {
		String definingSql = "select * from syn1 join syn2 on (syn1.id = syn2.id)";
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.materializedview, definingSql,
				null);

		// call under test
		IndexDescription result = IndexDescriptionFactory.createIndexDescription(state, lookup);

		// Both sources its defining SQL names are resolved through the lookup, in SQL order.
		assertEquals(new MaterializedViewIndexDescription(idAndVersion, definingSql, lookup), result);
		assertEquals(Arrays.asList(new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn2"))), result.getDependencies());
	}

	@Test
	public void testCreateIndexDescriptionWithVirtualTable() {
		String definingSql = "select * from syn1";
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.virtualtable, definingSql, null);

		// call under test
		IndexDescription result = IndexDescriptionFactory.createIndexDescription(state, lookup);

		assertEquals(idAndVersion, result.getIdAndVersion());
		assertEquals(TableType.virtualtable, result.getTableType());
		// The rebuilt description carries the behavior that makes a virtual table queryable: it has no
		// index of its own, so its defining SQL is inlined as a common table expression.
		assertEquals("WITH syn123 AS (select * from syn1) select * from syn123",
				result.preprocessQuery("select * from syn123"));
	}

	@ParameterizedTest
	@EnumSource(TableType.class)
	public void testCreateIndexDescriptionWithEveryTableType(TableType tableType) {
		// Every queryable type must be rebuildable, because a type the factory does not handle cannot be
		// queried from a snapshot at all. A defining SQL is supplied for the types that require one and
		// ignored by the rest.
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, tableType, "select * from syn1", 7L);

		// call under test
		IndexDescription result = IndexDescriptionFactory.createIndexDescription(state, lookup);

		assertEquals(idAndVersion, result.getIdAndVersion());
		assertEquals(tableType, result.getTableType());
	}

	@Test
	public void testCreateIndexDescriptionWithMaterializedViewAndNoDefiningSql() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.materializedview, null, null);

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			IndexDescriptionFactory.createIndexDescription(state, lookup);
		}).getMessage();
		assertEquals("materializedview syn123 was captured without its defining SQL", message);
	}

	@Test
	public void testCreateIndexDescriptionWithVirtualTableAndNoDefiningSql() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.virtualtable, null, null);

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			IndexDescriptionFactory.createIndexDescription(state, lookup);
		}).getMessage();
		assertEquals("virtualtable syn123 was captured without its defining SQL", message);
	}

	@Test
	public void testCreateIndexDescriptionWithNullState() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			IndexDescriptionFactory.createIndexDescription(null, lookup);
		}).getMessage();
		assertEquals("state is required.", message);
	}

	@Test
	public void testCreateIndexDescriptionWithNullLookup() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.table, null, 7L);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			IndexDescriptionFactory.createIndexDescription(state, null);
		}).getMessage();
		assertEquals("lookup is required.", message);
	}
}
