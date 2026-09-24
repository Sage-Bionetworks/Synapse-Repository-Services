package org.sagebionetworks.table.cluster.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.search.table.SearchIndexState;
import org.sagebionetworks.repo.model.search.table.SearchIndexStatus;
import org.sagebionetworks.repo.model.table.BenefactorColumn;
import org.sagebionetworks.repo.model.table.ColumnLineageEntry;
import org.sagebionetworks.repo.model.table.DerivationKind;
import org.sagebionetworks.repo.model.table.IndexAuthorizationSnapshot;
import org.sagebionetworks.repo.model.table.IndexDescriptionSnapshot;
import org.sagebionetworks.repo.model.table.SourceColumnReference;
import org.sagebionetworks.repo.model.table.SourceDependency;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:table-cluster-spb.xml" })
public class SearchIndexStatusDaoImplTest {

	@Autowired
	private ConnectionFactory connectionFactory;

	private SearchIndexStatusDao dao;

	@BeforeEach
	void setUp() {
		dao = connectionFactory.getSearchIndexStatusDao();
		dao.truncateAll();
	}

	@Test
	void testCreateAndGetState() {
		// call under test
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(SearchIndexState.CREATING));

		Optional<SearchIndexState> state = dao.getState(42L);
		assertTrue(state.isPresent());
		assertEquals(SearchIndexState.CREATING, state.get());
	}

	@Test
	void testUpsertUpdatesExisting() {
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(SearchIndexState.CREATING));

		// call under test
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(SearchIndexState.ACTIVE));

		assertEquals(SearchIndexState.ACTIVE, dao.getState(42L).orElseThrow());
	}

	@Test
	void testUpsertWithErrorMessage() {
		// call under test
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(SearchIndexState.FAILED)
				.setErrorMessage("Something broke"));

		assertEquals(SearchIndexState.FAILED, dao.getState(42L).orElseThrow());
	}

	@Test
	void testExistsReturnsFalseForMissing() {
		// call under test
		assertFalse(dao.exists(999L));
	}

	@Test
	void testExistsReturnsTrueAfterCreate() {
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(SearchIndexState.CREATING));

		// call under test
		assertTrue(dao.exists(42L));
	}

	@Test
	void testDeleteRemovesRow() {
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(SearchIndexState.ACTIVE));

		// call under test
		dao.delete(42L);

		assertFalse(dao.exists(42L));
	}

	@Test
	void testGetStateMissingReturnsEmpty() {
		// call under test
		assertTrue(dao.getState(999L).isEmpty());
	}

	@Test
	void testGetStatusWithFailedState() {
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(SearchIndexState.FAILED)
				.setErrorMessage("Something broke"));

		// call under test
		Optional<SearchIndexStatus> result = dao.getStatus(42L);

		assertTrue(result.isPresent());
		SearchIndexStatus status = result.get();
		assertEquals("syn42", status.getSearchIndexId());
		assertEquals(SearchIndexState.FAILED, status.getState());
		assertEquals("Something broke", status.getErrorMessage());
		assertNotNull(status.getChangedOn());
	}

	@Test
	void testGetStatusMissingReturnsEmpty() {
		// call under test
		assertTrue(dao.getStatus(999L).isEmpty());
	}

	@Test
	void testSaveAndGetSnapshot() {
		// The status row exists before a snapshot is captured (as it does at the real capture site).
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(SearchIndexState.CREATING));
		IndexAuthorizationSnapshot snapshot = newSnapshot();

		// call under test
		dao.saveSnapshot(42L, snapshot);

		assertEquals(Optional.of(snapshot), dao.getSnapshot(42L));
	}

	@Test
	void testGetSnapshotWithMissingRow() {
		// call under test
		assertEquals(Optional.empty(), dao.getSnapshot(999L));
	}

	@Test
	void testGetSnapshotWithNullId() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.getSnapshot(null);
		}).getMessage();
		assertEquals("searchIndexId is required.", message);
	}

	@Test
	void testGetSnapshotWhenRowExistsButNoSnapshotCaptured() {
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(SearchIndexState.CREATING));

		// call under test
		assertEquals(Optional.empty(), dao.getSnapshot(42L));
	}

	@Test
	void testSaveSnapshotOverwritesPrevious() {
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(SearchIndexState.CREATING));
		dao.saveSnapshot(42L, newSnapshot());
		IndexAuthorizationSnapshot rebuilt = newSnapshot().setVersionNumber(9L);

		// call under test
		dao.saveSnapshot(42L, rebuilt);

		assertEquals(Optional.of(rebuilt), dao.getSnapshot(42L));
	}

	@Test
	void testSaveSnapshotPreservesLifecycleState() {
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(SearchIndexState.ACTIVE));

		// call under test
		dao.saveSnapshot(42L, newSnapshot());

		// saveSnapshot only touches SNAPSHOT_JSON, so the lifecycle state written by createOrUpdate survives.
		assertEquals(SearchIndexState.ACTIVE, dao.getState(42L).orElseThrow());
		assertEquals(Optional.of(newSnapshot()), dao.getSnapshot(42L));
	}

	@Test
	void testCreateOrUpdatePreservesSnapshot() {
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(SearchIndexState.CREATING));
		dao.saveSnapshot(42L, newSnapshot());

		// call under test
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(SearchIndexState.FAILED)
				.setErrorMessage("rebuild failed"));

		// createOrUpdate's upsert only touches STATE/ERROR_MESSAGE/CHANGED_ON, so the captured snapshot survives.
		assertEquals(SearchIndexState.FAILED, dao.getState(42L).orElseThrow());
		assertEquals(Optional.of(newSnapshot()), dao.getSnapshot(42L));
	}

	@Test
	void testTruncateAllRemovesSnapshot() {
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(SearchIndexState.CREATING));
		dao.saveSnapshot(42L, newSnapshot());

		// call under test
		dao.truncateAll();

		assertEquals(Optional.empty(), dao.getSnapshot(42L));
	}

	/**
	 * A realistic multi-entry snapshot: a SearchIndex authorized through a materialized-view source with a
	 * benefactor column and two transitive dependencies, and a column lineage mixing an identity column and
	 * an aggregate. Non-trivial so the JSON round-trip would surface a serialization bug.
	 */
	private IndexAuthorizationSnapshot newSnapshot() {
		return new IndexAuthorizationSnapshot()
				.setObjectId("syn42")
				.setVersionNumber(3L)
				.setIndexDescription(new IndexDescriptionSnapshot()
						.setObjectId("syn789")
						.setVersionNumber(null)
						.setTableType("materializedview")
						.setBenefactors(Arrays.asList(new BenefactorColumn()
								.setBenefactorColumnName("_benefactor_0")
								.setBenefactorType("ENTITY")))
						.setDependencies(Arrays.asList(
								new SourceDependency().setObjectId("syn100").setVersionNumber(null).setTableType("table"),
								new SourceDependency().setObjectId("syn200").setVersionNumber(5L).setTableType("entityview"))))
				.setColumnLineage(Arrays.asList(
						new ColumnLineageEntry()
								.setOutputColumnId("111")
								.setDerivationKind(DerivationKind.IDENTITY)
								.setInputs(Arrays.asList(new SourceColumnReference()
										.setSourceObjectId("syn100").setSourceVersionNumber(null).setSourceColumnId("10"))),
						new ColumnLineageEntry()
								.setOutputColumnId("222")
								.setDerivationKind(DerivationKind.AGGREGATE)
								.setSetFunctionType("MAX")
								.setInputs(Arrays.asList(new SourceColumnReference()
										.setSourceObjectId("syn200").setSourceVersionNumber(5L).setSourceColumnId("11")))));
	}

	@ParameterizedTest
	@EnumSource(SearchIndexState.class)
	void testCreateOrUpdateWithEachState(SearchIndexState state) {
		String expectedError = state == SearchIndexState.FAILED ? "example failure" : null;

		// call under test
		dao.createOrUpdate(new SearchIndexStatus()
				.setSearchIndexId("syn42")
				.setState(state)
				.setErrorMessage(expectedError));

		SearchIndexStatus result = dao.getStatus(42L).orElseThrow();
		assertEquals("syn42", result.getSearchIndexId());
		assertEquals(state, result.getState());
		assertNotNull(result.getChangedOn());
		if (state == SearchIndexState.FAILED) {
			assertEquals("example failure", result.getErrorMessage());
		} else {
			assertNull(result.getErrorMessage());
		}
	}
}
