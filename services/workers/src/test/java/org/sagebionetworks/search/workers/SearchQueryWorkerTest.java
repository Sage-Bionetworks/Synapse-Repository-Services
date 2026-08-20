package org.sagebionetworks.search.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.search.SearchIndexQueryManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;

import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.dsl.MatchAllQuery;
import org.sagebionetworks.repo.model.search.dsl.Query;
import org.sagebionetworks.repo.model.search.table.SearchIndexQuery;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class SearchQueryWorkerTest {

	@Mock
	private SearchIndexQueryManager mockSearchIndexQueryManager;

	@InjectMocks
	private SearchQueryWorker worker;

	@Mock
	private AsyncJobProgressCallback mockJobCallback;

	private UserInfo user;
	private SearchIndexQuery request;
	private String searchIndexId;
	private String jobId;

	@BeforeEach
	public void before() {
		user = new UserInfo(false, 123L, AuthorizationConstants.DEFAULT_REALM_ID);
		searchIndexId = "syn456";
		request = new SearchIndexQuery();
		request.setSearchIndexId(searchIndexId);
		request.setSearchQuery(new SearchQuery().setQuery(new Query().setMatch_all(new MatchAllQuery())));
		jobId = "job-1";
	}

	@Test
	public void testGetRequestType() {
		assertEquals(SearchIndexQuery.class, worker.getRequestType());
	}

	@Test
	public void testGetResponseType() {
		assertEquals(SearchQueryResults.class, worker.getResponseType());
	}

	@Test
	public void testRunWithValidRequest() throws Exception {
		SearchQueryResults expected = new SearchQueryResults();
		when(mockSearchIndexQueryManager.search(user, request)).thenReturn(expected);

		// Call under test
		SearchQueryResults result = worker.run(jobId, user, request, mockJobCallback);

		assertEquals(expected, result);
		verify(mockSearchIndexQueryManager).search(user, request);
	}

	@Test
	public void testRunWithStillBuildingStatus() throws Exception {
		when(mockSearchIndexQueryManager.search(user, request))
			.thenThrow(new IllegalStateException("Index is still building"));

		// Call under test
		assertThrows(RecoverableMessageException.class, () -> {
			worker.run(jobId, user, request, mockJobCallback);
		});
	}

	@Test
	public void testRunWithOtherIllegalState() throws Exception {
		IllegalStateException cause = new IllegalStateException("Some other error");
		when(mockSearchIndexQueryManager.search(user, request)).thenThrow(cause);

		// Call under test
		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {
			worker.run(jobId, user, request, mockJobCallback);
		});

		assertEquals(cause, result);
	}

	@Test
	public void testRunWithFailedIndexStatus() throws Exception {
		// checkIndexStatus throws IllegalArgumentException for FAILED state
		when(mockSearchIndexQueryManager.search(user, request))
			.thenThrow(new IllegalArgumentException("Search index build failed: some error. Delete or update the SearchIndex to trigger a rebuild."));

		// Call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			worker.run(jobId, user, request, mockJobCallback);
		});
		assertTrue(ex.getMessage().contains("build failed"));
	}

	@Test
	public void testRunWithDeletingIndexStatus() throws Exception {
		when(mockSearchIndexQueryManager.search(user, request))
			.thenThrow(new IllegalArgumentException("Search index is being deleted."));

		// Call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			worker.run(jobId, user, request, mockJobCallback);
		});
		assertTrue(ex.getMessage().contains("being deleted"));
	}

	@Test
	public void testRunWithUnexpectedException() throws Exception {
		when(mockSearchIndexQueryManager.search(user, request))
			.thenThrow(new RuntimeException("unexpected error"));

		// Call under test
		assertThrows(RuntimeException.class, () -> {
			worker.run(jobId, user, request, mockJobCallback);
		});
	}
}
