package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.sagebionetworks.repo.model.table.IndexDescriptionSnapshot;
import org.sagebionetworks.repo.model.table.SourceDependency;
import org.sagebionetworks.table.query.model.SqlContext;

public class SnapshotIndexDescriptionTest {

	// A change-number provider that knows nothing; every object contributes nothing to the hash.
	private final Function<IdAndVersion, Optional<Long>> emptyChangeNumbers = id -> Optional.empty();

	@Test
	public void testFromSnapshotWithView() {
		IndexDescriptionSnapshot snapshot = new IndexDescriptionSnapshot().setObjectId("syn123")
				.setVersionNumber(null).setTableType(TableType.entityview.name())
				.setBenefactors(Collections.singletonList(
						new BenefactorColumn().setBenefactorColumnName(ROW_BENEFACTOR).setBenefactorType(ObjectType.ENTITY.name())))
				.setDependencies(Collections.emptyList());

		// call under test
		SnapshotIndexDescription description = SnapshotIndexDescription.fromSnapshot(snapshot, emptyChangeNumbers);

		assertEquals(IdAndVersion.parse("syn123"), description.getIdAndVersion());
		assertEquals(TableType.entityview, description.getTableType());
		assertEquals(Collections.singletonList(new BenefactorDescription(ROW_BENEFACTOR, ObjectType.ENTITY)),
				description.getBenefactors());
		assertEquals(Collections.emptyList(), description.getDependencies());
	}

	@Test
	public void testFromSnapshotWithVersionPinned() {
		IndexDescriptionSnapshot snapshot = new IndexDescriptionSnapshot().setObjectId("syn123").setVersionNumber(4L)
				.setTableType(TableType.entityview.name()).setBenefactors(Collections.emptyList())
				.setDependencies(Collections.emptyList());

		// call under test
		SnapshotIndexDescription description = SnapshotIndexDescription.fromSnapshot(snapshot, emptyChangeNumbers);

		assertEquals(IdAndVersion.parse("syn123.4"), description.getIdAndVersion());
	}

	@Test
	public void testFromSnapshotWithSubmissionViewBenefactor() {
		IndexDescriptionSnapshot snapshot = new IndexDescriptionSnapshot().setObjectId("syn123")
				.setTableType(TableType.submissionview.name())
				.setBenefactors(Collections.singletonList(new BenefactorColumn().setBenefactorColumnName(ROW_BENEFACTOR)
						.setBenefactorType(ObjectType.EVALUATION.name())))
				.setDependencies(Collections.emptyList());

		// call under test
		SnapshotIndexDescription description = SnapshotIndexDescription.fromSnapshot(snapshot, emptyChangeNumbers);

		assertEquals(Collections.singletonList(new BenefactorDescription(ROW_BENEFACTOR, ObjectType.EVALUATION)),
				description.getBenefactors());
	}

	@Test
	public void testFromSnapshotWithNullBenefactorsAndDependencies() {
		IndexDescriptionSnapshot snapshot = new IndexDescriptionSnapshot().setObjectId("syn123")
				.setTableType(TableType.table.name()).setBenefactors(null).setDependencies(null);

		// call under test
		SnapshotIndexDescription description = SnapshotIndexDescription.fromSnapshot(snapshot, emptyChangeNumbers);

		assertEquals(Collections.emptyList(), description.getBenefactors());
		assertEquals(Collections.emptyList(), description.getDependencies());
	}

	@Test
	public void testFromSnapshotWithDependencies() {
		IndexDescriptionSnapshot snapshot = new IndexDescriptionSnapshot().setObjectId("syn123")
				.setTableType(TableType.materializedview.name()).setBenefactors(Collections.emptyList())
				.setDependencies(Arrays.asList(
						new SourceDependency().setObjectId("syn1").setVersionNumber(null).setTableType(TableType.entityview.name()),
						new SourceDependency().setObjectId("syn2").setVersionNumber(7L).setTableType(TableType.table.name())));

		// call under test
		SnapshotIndexDescription description = SnapshotIndexDescription.fromSnapshot(snapshot, emptyChangeNumbers);

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
	public void testGetColumnNamesToAddToSelectWithViewWithEtag() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		SnapshotIndexDescription description = viewDescription(idAndVersion, TableType.entityview);

		// call under test
		List<ColumnToAdd> result = description.getColumnNamesToAddToSelect(SqlContext.query, true, false);

		assertEquals(Arrays.asList(new ColumnToAdd(idAndVersion, ROW_ID), new ColumnToAdd(idAndVersion, ROW_VERSION),
				new ColumnToAdd(idAndVersion, ROW_ETAG), new ColumnToAdd(idAndVersion, ROW_BENEFACTOR)), result);
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithViewWithoutEtag() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		SnapshotIndexDescription description = viewDescription(idAndVersion, TableType.entityview);

		// call under test
		List<ColumnToAdd> result = description.getColumnNamesToAddToSelect(SqlContext.query, false, false);

		assertEquals(Arrays.asList(new ColumnToAdd(idAndVersion, ROW_ID), new ColumnToAdd(idAndVersion, ROW_VERSION),
				new ColumnToAdd(idAndVersion, ROW_BENEFACTOR)), result);
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithNonView() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		SnapshotIndexDescription description = tableDescription(idAndVersion, TableType.materializedview);

		// call under test
		List<ColumnToAdd> result = description.getColumnNamesToAddToSelect(SqlContext.query, true, false);

		assertEquals(Arrays.asList(new ColumnToAdd(idAndVersion, ROW_ID), new ColumnToAdd(idAndVersion, ROW_VERSION)),
				result);
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithAggregate() {
		SnapshotIndexDescription description = viewDescription(IdAndVersion.parse("syn123"), TableType.entityview);

		// call under test
		List<ColumnToAdd> result = description.getColumnNamesToAddToSelect(SqlContext.query, true, true);

		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithBuildContext() {
		SnapshotIndexDescription description = viewDescription(IdAndVersion.parse("syn123"), TableType.entityview);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			description.getColumnNamesToAddToSelect(SqlContext.build, true, false);
		}).getMessage();
		assertEquals("Only 'query' is supported for a snapshot index description", message);
	}

	@Test
	public void testAddRowIdToSearchIndexWithView() {
		SnapshotIndexDescription description = viewDescription(IdAndVersion.parse("syn123"), TableType.entityview);
		// call under test
		assertTrue(description.addRowIdToSearchIndex());
	}

	@Test
	public void testAddRowIdToSearchIndexWithNonView() {
		SnapshotIndexDescription description = tableDescription(IdAndVersion.parse("syn123"), TableType.materializedview);
		// call under test
		assertFalse(description.addRowIdToSearchIndex());
	}

	@Test
	public void testSupportQueryCache() {
		SnapshotIndexDescription description = tableDescription(IdAndVersion.parse("syn123"), TableType.table);
		// call under test
		assertFalse(description.supportQueryCache());
	}

	@Test
	public void testPreprocessQueryIsIdentity() {
		SnapshotIndexDescription description = tableDescription(IdAndVersion.parse("syn123"), TableType.table);
		// call under test
		assertEquals("select * from syn123", description.preprocessQuery("select * from syn123"));
	}

	@Test
	public void testGetLastTableChangeNumber() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		Function<IdAndVersion, Optional<Long>> provider = id -> Optional.of(42L);
		SnapshotIndexDescription description = new SnapshotIndexDescription(idAndVersion, TableType.entityview,
				Collections.emptyList(), Collections.emptyList(), provider);

		// call under test
		assertEquals(Optional.of(42L), description.getLastTableChangeNumber());
	}

	@Test
	public void testGetTableHashMatchesLiveViewDescription() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		Function<IdAndVersion, Optional<Long>> provider = id -> Optional.of(42L);
		SnapshotIndexDescription description = new SnapshotIndexDescription(idAndVersion, TableType.entityview,
				Collections.singletonList(new BenefactorDescription(ROW_BENEFACTOR, ObjectType.ENTITY)),
				Collections.emptyList(), provider);
		// The live view fed the same change number must produce the same query-cache hash.
		ViewIndexDescription live = new ViewIndexDescription(idAndVersion, TableType.entityview, 42L);

		// call under test
		assertEquals(live.getTableHash(), description.getTableHash());
	}

	@Test
	public void testGetTableHashIncludesDependencyChangeNumbers() {
		// The hash of a materialized view walks its dependencies' live change numbers, so two snapshots
		// that differ only in a dependency's current change number must hash differently.
		IndexDescriptionSnapshot snapshot = new IndexDescriptionSnapshot().setObjectId("syn123")
				.setTableType(TableType.materializedview.name()).setBenefactors(Collections.emptyList())
				.setDependencies(Collections.singletonList(
						new SourceDependency().setObjectId("syn1").setTableType(TableType.entityview.name())));

		Function<IdAndVersion, Optional<Long>> firstProvider = id -> IdAndVersion.parse("syn1").equals(id)
				? Optional.of(5L) : Optional.empty();
		Function<IdAndVersion, Optional<Long>> secondProvider = id -> IdAndVersion.parse("syn1").equals(id)
				? Optional.of(6L) : Optional.empty();

		String first = SnapshotIndexDescription.fromSnapshot(snapshot, firstProvider).getTableHash();
		String second = SnapshotIndexDescription.fromSnapshot(snapshot, secondProvider).getTableHash();

		assertFalse(first.equals(second));
	}

	@Test
	public void testGetCreateOrUpdateIndexSqlThrows() {
		SnapshotIndexDescription description = tableDescription(IdAndVersion.parse("syn123"), TableType.table);
		// A snapshot description only drives the query path and can never build an index.
		String message = assertThrows(UnsupportedOperationException.class, () -> {
			// call under test
			description.getCreateOrUpdateIndexSql();
		}).getMessage();
		assertEquals("Cannot create or update the index of a snapshot description", message);
	}

	@Test
	public void testConstructorWithNullIdAndVersion() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new SnapshotIndexDescription(null, TableType.table, Collections.emptyList(), Collections.emptyList(),
					emptyChangeNumbers);
		}).getMessage();
		assertEquals("idAndVersion is required.", message);
	}

	@Test
	public void testConstructorWithNullChangeNumberProvider() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new SnapshotIndexDescription(IdAndVersion.parse("syn123"), TableType.table, Collections.emptyList(),
					Collections.emptyList(), null);
		}).getMessage();
		assertEquals("changeNumberProvider is required.", message);
	}

	@Test
	public void testFromSnapshotWithNullSnapshot() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			SnapshotIndexDescription.fromSnapshot(null, emptyChangeNumbers);
		}).getMessage();
		assertEquals("snapshot is required.", message);
	}

	private SnapshotIndexDescription viewDescription(IdAndVersion idAndVersion, TableType tableType) {
		return new SnapshotIndexDescription(idAndVersion, tableType,
				Collections.singletonList(new BenefactorDescription(ROW_BENEFACTOR, ObjectType.ENTITY)),
				Collections.emptyList(), emptyChangeNumbers);
	}

	private SnapshotIndexDescription tableDescription(IdAndVersion idAndVersion, TableType tableType) {
		return new SnapshotIndexDescription(idAndVersion, tableType, Collections.emptyList(), Collections.emptyList(),
				emptyChangeNumbers);
	}
}
