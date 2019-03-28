package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.search.SearchManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test for SearchServiceImpl
 * @author John
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SearchServiceImplTest {
	@Mock
	private SearchManager mockSearchManager;
	@Mock
	private UserManager mockUserManager;

	long userId;
	private SearchServiceImpl service;
	private UserInfo userInfo;

	@Before
	public void before(){
		service= new SearchServiceImpl();
		ReflectionTestUtils.setField(service, "searchManager", mockSearchManager);
		ReflectionTestUtils.setField(service, "userManager", mockUserManager);
		userId = 990L;
		userInfo = new UserInfo(false, userId);
	}

	@Test
	public void testProxySearch() throws Exception{
		SearchQuery searchQuery = new SearchQuery();
		SearchResults searchResults = new SearchResults();
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		when(mockSearchManager.proxySearch(userInfo, searchQuery)).thenReturn(searchResults);

		//method under test
		SearchResults callResults = service.proxySearch(userId, searchQuery);

		verify(mockUserManager, times(1)).getUserInfo(userId);
		verify(mockSearchManager, times(1)).proxySearch(userInfo, searchQuery);
		assertEquals(searchResults, callResults);
	}


}
