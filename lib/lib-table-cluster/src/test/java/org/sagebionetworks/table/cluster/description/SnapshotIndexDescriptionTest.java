package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_BENEFACTOR;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.BenefactorColumn;
import org.sagebionetworks.repo.model.table.ColumnLineageEntry;
import org.sagebionetworks.repo.model.table.DerivationKind;
import org.sagebionetworks.repo.model.table.IndexAuthorizationSnapshot;
import org.sagebionetworks.repo.model.table.IndexDescriptionSnapshot;
import org.sagebionetworks.repo.model.table.SourceColumnReference;
import org.sagebionetworks.repo.model.table.SourceDependency;
import org.sagebionetworks.table.query.model.SqlContext;

public class SnapshotIndexDescriptionTest {

	// A change-number provider that knows nothing; every object contributes nothing to the hash.
	private final Function<IdAndVersion, Optional<Long>> emptyChangeNumbers = id -> Optional.empty();

	@Test
	public void testFromSnapshotWithView() {
		IndexDescriptionSnapshot snapshot = snapshot("syn123", TableType.entityview)
				.setBenefactors(Collections.singletonList(new BenefactorColumn().setBenefactorColumnName(ROW_BENEFACTOR)
						.setBenefactorType(ObjectType.ENTITY.name())));

		// call under test
		SnapshotIndexDescription description = fromSnapshot(snapshot, emptyChangeNumbers);

		assertEquals(IdAndVersion.parse("syn123"), description.getIdAndVersion());
		assertEquals(TableType.entityview, description.getTableType());
		assertEquals(Collections.singletonList(new BenefactorDescription(ROW_BENEFACTOR, ObjectType.ENTITY)),
				description.getBenefactors());
		assertEquals(Collections.emptyList(), description.getDependencies());
	}

	@Test
	public void testFromSnapshotWithVersionPinned() {
		IndexDescriptionSnapshot snapshot = snapshot("syn123", TableType.entityview).setVersionNumber(4L);

		// call under test
		SnapshotIndexDescription description = fromSnapshot(snapshot, emptyChangeNumbers);

		assertEquals(IdAndVersion.parse("syn123.4"), description.getIdAndVersion());
	}

	@Test
	public void testFromSnapshotWithSubmissionViewBenefactor() {
		IndexDescriptionSnapshot snapshot = snapshot("syn123", TableType.submissionview)
				.setBenefactors(Collections.singletonList(new BenefactorColumn().setBenefactorColumnName(ROW_BENEFACTOR)
						.setBenefactorType(ObjectType.EVALUATION.name())));

		// call under test
		SnapshotIndexDescription description = fromSnapshot(snapshot, emptyChangeNumbers);

		assertEquals(Collections.singletonList(new BenefactorDescription(ROW_BENEFACTOR, ObjectType.EVALUATION)),
				description.getBenefactors());
	}

	@Test
	public void testFromSnapshotWithNullBenefactorsAndDependencies() {
		IndexDescriptionSnapshot snapshot = snapshot("syn123", TableType.table).setBenefactors(null)
				.setDependencies(null);

		// call under test
		SnapshotIndexDescription description = fromSnapshot(snapshot, emptyChangeNumbers);

		assertEquals(Collections.emptyList(), description.getBenefactors());
		assertEquals(Collections.emptyList(), description.getDependencies());
	}

	@Test
	public void testFromSnapshotWithDependencies() {
		// The flattened closure carries syn2 even though the defining SQL only names syn1: syn2 is a
		// transitive source the ACL check must still evaluate.
		IndexDescriptionSnapshot snapshot = snapshot("syn123", TableType.materializedview)
				.setDefiningSql("select * from syn1")
				.setDependencies(Arrays.asList(
						new SourceDependency().setObjectId("syn1").setTableType(TableType.entityview.name()),
						new SourceDependency().setObjectId("syn2").setVersionNumber(7L)
								.setTableType(TableType.table.name())));

		// call under test
		SnapshotIndexDescription description = fromSnapshot(snapshot, emptyChangeNumbers);

		List<? extends QueryIndexDescription> dependencies = description.getDependencies();
		assertEquals(2, dependencies.size());

		QueryIndexDescription one = dependencies.get(0);
		assertEquals(IdAndVersion.parse("syn1"), one.getIdAndVersion());
		assertEquals(TableType.entityview, one.getTableType());
		// A dependency node carries only id/version/type; it has no benefactors or nested dependencies.
		assertEquals(Collections.emptyList(), one.getBenefactors());
		assertEquals(Collections.emptyList(), one.getDependencies());

		QueryIndexDescription two = dependencies.get(1);
		assertEquals(IdAndVersion.parse("syn2.7"), two.getIdAndVersion());
		assertEquals(TableType.table, two.getTableType());
	}

	@Test
	public void testPreprocessQueryWithDefiningSqlNamingAnAbsentSource() {
		// The dependency closure and the defining SQL are captured together, so a source the SQL names
		// but the closure omits means the snapshot is internally inconsistent.
		SnapshotIndexDescription description = fromSnapshot(
				snapshot("syn123", TableType.materializedview).setDefiningSql("select * from syn9"),
				emptyChangeNumbers);

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			description.preprocessQuery("select * from syn123");
		}).getMessage();
		assertEquals("The snapshot of syn123 does not include its source syn9", message);
	}

	@Test
	public void testPreprocessQueryWithMaterializedViewMissingDefiningSql() {
		// A materialized view is defined by nothing but its SQL, so it cannot be rebuilt without it.
		SnapshotIndexDescription description = fromSnapshot(snapshot("syn123", TableType.materializedview),
				emptyChangeNumbers);

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			description.preprocessQuery("select * from syn123");
		}).getMessage();
		assertEquals("materializedview syn123 was captured without its defining SQL", message);
	}

	@Test
	public void testGetColumnLineageWithMaterializedViewMissingDefiningSql() {
		// The authorization projection and the lineage are readable without the state that only shapes
		// SQL, so the snapshot pre-load path is never blocked by a source captured without its SQL.
		List<ColumnLineageEntry> lineage = Collections.singletonList(new ColumnLineageEntry()
				.setOutputColumnId("11").setDerivationKind(DerivationKind.IDENTITY).setInputs(Collections
						.singletonList(new SourceColumnReference().setSourceObjectId("syn1").setSourceColumnId("101"))));
		SnapshotIndexDescription description = SnapshotIndexDescription.fromSnapshot(
				new IndexAuthorizationSnapshot()
						.setIndexDescription(snapshot("syn123", TableType.materializedview))
						.setColumnLineage(lineage),
				emptyChangeNumbers);

		// call under test
		assertEquals(lineage, description.getColumnLineage());
	}

	@Test
	public void testFromSnapshotWithNullSnapshot() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			SnapshotIndexDescription.fromSnapshot(null, emptyChangeNumbers);
		}).getMessage();
		assertEquals("authorizationSnapshot is required.", message);
	}

	@Test
	public void testFromSnapshotWithNullIndexDescription() {
		IndexAuthorizationSnapshot authorizationSnapshot = new IndexAuthorizationSnapshot();
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			SnapshotIndexDescription.fromSnapshot(authorizationSnapshot, emptyChangeNumbers);
		}).getMessage();
		assertEquals("authorizationSnapshot.indexDescription is required.", message);
	}

	@Test
	public void testFromSnapshotWithNullChangeNumberProvider() {
		IndexAuthorizationSnapshot authorizationSnapshot = new IndexAuthorizationSnapshot()
				.setIndexDescription(snapshot("syn123", TableType.table));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			SnapshotIndexDescription.fromSnapshot(authorizationSnapshot, null);
		}).getMessage();
		assertEquals("changeNumberProvider is required.", message);
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithViewWithEtag() {
		SnapshotIndexDescription description = fromSnapshot(snapshot("syn123", TableType.entityview),
				emptyChangeNumbers);
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");

		// call under test
		List<ColumnToAdd> result = description.getColumnNamesToAddToSelect(SqlContext.query, true, false);

		assertEquals(Arrays.asList(new ColumnToAdd(idAndVersion, ROW_ID), new ColumnToAdd(idAndVersion, ROW_VERSION),
				new ColumnToAdd(idAndVersion, ROW_ETAG), new ColumnToAdd(idAndVersion, ROW_BENEFACTOR)), result);
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithViewWithoutEtag() {
		SnapshotIndexDescription description = fromSnapshot(snapshot("syn123", TableType.entityview),
				emptyChangeNumbers);
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");

		// call under test
		List<ColumnToAdd> result = description.getColumnNamesToAddToSelect(SqlContext.query, false, false);

		assertEquals(Arrays.asList(new ColumnToAdd(idAndVersion, ROW_ID), new ColumnToAdd(idAndVersion, ROW_VERSION),
				new ColumnToAdd(idAndVersion, ROW_BENEFACTOR)), result);
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithMaterializedView() {
		SnapshotIndexDescription description = materializedViewOverTable();
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");

		// call under test
		List<ColumnToAdd> result = description.getColumnNamesToAddToSelect(SqlContext.query, true, false);

		assertEquals(Arrays.asList(new ColumnToAdd(idAndVersion, ROW_ID), new ColumnToAdd(idAndVersion, ROW_VERSION)),
				result);
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithVirtualTable() {
		// A virtual table has no index of its own, so there is no row metadata to select.
		SnapshotIndexDescription description = virtualTableOverTable();

		// call under test
		List<ColumnToAdd> result = description.getColumnNamesToAddToSelect(SqlContext.query, true, false);

		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithAggregate() {
		SnapshotIndexDescription description = fromSnapshot(snapshot("syn123", TableType.entityview),
				emptyChangeNumbers);

		// call under test
		List<ColumnToAdd> result = description.getColumnNamesToAddToSelect(SqlContext.query, true, true);

		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithBuildContext() {
		SnapshotIndexDescription description = fromSnapshot(snapshot("syn123", TableType.entityview),
				emptyChangeNumbers);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			description.getColumnNamesToAddToSelect(SqlContext.build, true, false);
		}).getMessage();
		assertEquals("Only 'query' is supported for a snapshot index description", message);
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithDependencyNode() {
		QueryIndexDescription dependency = materializedViewOverTable().getDependencies().get(0);

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			dependency.getColumnNamesToAddToSelect(SqlContext.query, true, false);
		}).getMessage();
		assertEquals("The flattened dependency node syn1 is not queryable;"
				+ " only the object a snapshot was captured for can be queried", message);
	}

	@Test
	public void testPreprocessQueryWithTable() {
		SnapshotIndexDescription description = fromSnapshot(snapshot("syn123", TableType.table), emptyChangeNumbers);

		// call under test
		assertEquals("select * from syn123", description.preprocessQuery("select * from syn123"));
	}

	@Test
	public void testPreprocessQueryWithVirtualTable() {
		SnapshotIndexDescription description = virtualTableOverTable();

		// A virtual table has no index to query, so its defining SQL must be inlined as a common table
		// expression under its own name.
		// call under test
		assertEquals("WITH syn123 AS (select * from syn1) select * from syn123",
				description.preprocessQuery("select * from syn123"));
	}

	@Test
	public void testSupportQueryCacheWithTable() {
		SnapshotIndexDescription description = fromSnapshot(snapshot("syn123", TableType.table), emptyChangeNumbers);
		// call under test
		assertFalse(description.supportQueryCache());
	}

	@Test
	public void testSupportQueryCacheWithVirtualTable() {
		SnapshotIndexDescription description = virtualTableOverTable();
		// call under test
		assertTrue(description.supportQueryCache());
	}

	@Test
	public void testAddRowIdToSearchIndexWithView() {
		SnapshotIndexDescription description = fromSnapshot(snapshot("syn123", TableType.entityview),
				emptyChangeNumbers);
		// call under test
		assertTrue(description.addRowIdToSearchIndex());
	}

	@Test
	public void testAddRowIdToSearchIndexWithMaterializedView() {
		SnapshotIndexDescription description = materializedViewOverTable();
		// call under test
		assertFalse(description.addRowIdToSearchIndex());
	}

	@Test
	public void testGetLastTableChangeNumberWithView() {
		SnapshotIndexDescription description = fromSnapshot(snapshot("syn123", TableType.entityview),
				id -> Optional.of(42L));

		// call under test
		assertEquals(Optional.of(42L), description.getLastTableChangeNumber());
	}

	@Test
	public void testGetLastTableChangeNumberWithMaterializedView() {
		// A materialized view's own node contributes nothing to the hash; only its dependencies do.
		SnapshotIndexDescription description = fromSnapshot(snapshot("syn123", TableType.materializedview)
				.setDefiningSql("select * from syn1")
				.setDependencies(Collections.singletonList(
						new SourceDependency().setObjectId("syn1").setTableType(TableType.table.name()))),
				id -> Optional.of(42L));

		// call under test
		assertEquals(Optional.empty(), description.getLastTableChangeNumber());
	}

	@Test
	public void testGetLastTableChangeNumberWithVirtualTable() {
		// A virtual table is never materialized, so only its source contributes to the hash.
		SnapshotIndexDescription description = fromSnapshot(snapshot("syn123", TableType.virtualtable)
				.setDefiningSql("select * from syn1")
				.setDependencies(Collections.singletonList(
						new SourceDependency().setObjectId("syn1").setTableType(TableType.table.name()))),
				id -> Optional.of(42L));

		// call under test
		assertEquals(Optional.empty(), description.getLastTableChangeNumber());
	}

	@Test
	public void testGetTableHashMatchesLiveViewDescription() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		SnapshotIndexDescription description = fromSnapshot(snapshot("syn123", TableType.entityview),
				id -> Optional.of(42L));
		// The live view fed the same change number must produce the same query-cache hash.
		ViewIndexDescription live = new ViewIndexDescription(idAndVersion, TableType.entityview, 42L);

		// call under test
		assertEquals(live.getTableHash(), description.getTableHash());
	}

	@Test
	public void testGetTableHashIncludesDependencyChangeNumbers() {
		// The hash of a materialized view walks its dependencies' live change numbers, so two snapshots
		// that differ only in a dependency's current change number must hash differently.
		IndexDescriptionSnapshot snapshot = snapshot("syn123", TableType.materializedview)
				.setDefiningSql("select * from syn1")
				.setDependencies(Collections.singletonList(
						new SourceDependency().setObjectId("syn1").setTableType(TableType.table.name())));

		String first = fromSnapshot(snapshot, id -> Optional.of(5L)).getTableHash();
		String second = fromSnapshot(snapshot, id -> Optional.of(6L)).getTableHash();

		assertNotEquals(first, second);
	}

	@Test
	public void testGetCreateOrUpdateIndexSql() {
		SnapshotIndexDescription description = fromSnapshot(snapshot("syn123", TableType.table), emptyChangeNumbers);
		// A snapshot description only drives the query path and can never build an index.
		String message = assertThrows(UnsupportedOperationException.class, () -> {
			// call under test
			description.getCreateOrUpdateIndexSql();
		}).getMessage();
		assertEquals("Cannot create or update the index of a snapshot description", message);
	}

	@Test
	public void testGetColumnLineageWithSnapshotLineage() {
		List<ColumnLineageEntry> lineage = Arrays.asList(
				new ColumnLineageEntry().setOutputColumnId("11").setDerivationKind(DerivationKind.IDENTITY)
						.setInputs(Collections.singletonList(
								new SourceColumnReference().setSourceObjectId("syn1").setSourceColumnId("101"))),
				new ColumnLineageEntry().setOutputColumnId("12").setDerivationKind(DerivationKind.LITERAL)
						.setInputs(Collections.emptyList()));
		SnapshotIndexDescription description = SnapshotIndexDescription.fromSnapshot(
				new IndexAuthorizationSnapshot().setIndexDescription(snapshot("syn123", TableType.table))
						.setColumnLineage(lineage),
				emptyChangeNumbers);

		// call under test
		assertEquals(lineage, description.getColumnLineage());
	}

	@Test
	public void testGetColumnLineageWithDependencyNode() {
		QueryIndexDescription dependency = materializedViewOverTable().getDependencies().get(0);

		// A flattened dependency node carries no lineage of its own, so it must fail loudly rather
		// than answer "no quasi-identifier derived columns".
		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			dependency.getColumnLineage();
		}).getMessage();
		assertEquals("No column lineage is available for syn1 of type table", message);
	}

	@Test
	public void testGetColumnLineageWithNoLineage() {
		SnapshotIndexDescription description = fromSnapshot(snapshot("syn123", TableType.table), emptyChangeNumbers);

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			description.getColumnLineage();
		}).getMessage();
		assertEquals("No column lineage is available for syn123 of type table", message);
	}

	@Test
	public void testGetState() {
		SnapshotIndexDescription description = virtualTableOverTable();

		// call under test
		assertEquals(new IndexDescriptionState(IdAndVersion.parse("syn123"), TableType.virtualtable,
				"select * from syn1", null), description.getState());
	}

	private static IndexDescriptionSnapshot snapshot(String objectId, TableType tableType) {
		return new IndexDescriptionSnapshot().setObjectId(objectId).setTableType(tableType.name())
				.setBenefactors(Collections.emptyList()).setDependencies(Collections.emptyList());
	}

	private static SnapshotIndexDescription fromSnapshot(IndexDescriptionSnapshot snapshot,
			Function<IdAndVersion, Optional<Long>> changeNumberProvider) {
		return SnapshotIndexDescription.fromSnapshot(
				new IndexAuthorizationSnapshot().setIndexDescription(snapshot), changeNumberProvider);
	}

	private SnapshotIndexDescription materializedViewOverTable() {
		return fromSnapshot(snapshot("syn123", TableType.materializedview).setDefiningSql("select * from syn1")
				.setDependencies(Collections.singletonList(
						new SourceDependency().setObjectId("syn1").setTableType(TableType.table.name()))),
				emptyChangeNumbers);
	}

	private SnapshotIndexDescription virtualTableOverTable() {
		return fromSnapshot(snapshot("syn123", TableType.virtualtable).setDefiningSql("select * from syn1")
				.setDependencies(Collections.singletonList(
						new SourceDependency().setObjectId("syn1").setTableType(TableType.table.name()))),
				emptyChangeNumbers);
	}
}
