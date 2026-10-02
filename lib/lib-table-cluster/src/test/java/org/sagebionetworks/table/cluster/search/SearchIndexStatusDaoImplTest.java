package org.sagebionetworks.table.cluster.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.search.table.SearchIndexState;
import org.sagebionetworks.repo.model.search.table.SearchIndexStatus;
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
