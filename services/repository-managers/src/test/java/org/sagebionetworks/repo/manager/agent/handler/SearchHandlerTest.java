package org.sagebionetworks.repo.manager.agent.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.service.SearchService;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

@ExtendWith(MockitoExtension.class)
public class SearchHandlerTest {
	
	private static final long USER_ID = 123;
	private static final String ACTION_GROUP = "org_sage_zero";
    private static final String FUNCTION = "org_sage_zero_search";
	
	@Mock
	private SearchService mockSearchService;
	
	@InjectMocks
	private SearchHandler handler;

	private SearchResults searchResults;
	
	@BeforeEach
	private void before() {
		searchResults = new SearchResults()
			.setHits(List.of(new Hit().setDescription("search result description")));
	}
	
	@Test
	public void testGetActionGroup() {
		assertEquals(ACTION_GROUP, handler.getActionGroup());
	}
	
	@Test
	public void testGetFunction() {
		assertEquals(FUNCTION, handler.getFunction());
	}
	
	@Test
	public void testSearchHandler() throws Exception {
		String searchTerm = "test search";
		
		when(mockSearchService.proxySearch(USER_ID, new SearchQuery().setQueryTerm(List.of(searchTerm))))
			.thenReturn(searchResults);
		
		ReturnControlEvent returnControlEvent = new ReturnControlEvent(USER_ID, ACTION_GROUP, FUNCTION, List.of(new Parameter("term", "string", searchTerm)));
		
		// Call under test
		assertEquals(EntityFactory.createJSONStringForEntity(searchResults), handler.handleEvent(returnControlEvent));
		
	}
	
	@Test
	public void testSearchHandlerWithTruncatedDescription() throws Exception {
		String searchTerm = "test search";
		
		searchResults = new SearchResults()
			.setHits(List.of(new Hit().setDescription("A".repeat(SearchHandler.MAX_NUM_CHARS + 1))));
		
		when(mockSearchService.proxySearch(USER_ID, new SearchQuery().setQueryTerm(List.of(searchTerm))))
			.thenReturn(searchResults);
		
		ReturnControlEvent returnControlEvent = new ReturnControlEvent(USER_ID, ACTION_GROUP, FUNCTION, List.of(new Parameter("term", "string", searchTerm)));
		
		SearchResults expectedResults = new SearchResults()
			.setHits(List.of(new Hit().setDescription("A".repeat(SearchHandler.MAX_NUM_CHARS) + " --truncated--")));
		
		// Call under test
		assertEquals(EntityFactory.createJSONStringForEntity(expectedResults), handler.handleEvent(returnControlEvent));
		
	}
	
	@Test
	public void testSearchHandlerWithoutTerm() throws Exception {
		
		ReturnControlEvent returnControlEvent = new ReturnControlEvent(USER_ID, ACTION_GROUP, FUNCTION, Collections.emptyList());
		
		assertEquals("Parameter 'term' of type string is required", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			handler.handleEvent(returnControlEvent);
		}).getMessage());
		
	}

}
