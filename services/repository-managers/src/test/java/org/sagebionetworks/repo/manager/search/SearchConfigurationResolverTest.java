package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dbo.search.SearchConfigurationDao;
import org.sagebionetworks.repo.model.search.table.SearchConfigBinding;
import org.sagebionetworks.repo.model.search.table.SearchConfiguration;

@ExtendWith(MockitoExtension.class)
public class SearchConfigurationResolverTest {

	@Mock
	private SearchConfigurationDao mockSearchConfigurationDao;

	@Mock
	private NodeDAO mockNodeDAO;

	private SearchConfigurationResolver resolver;

	@BeforeEach
	public void before() {
		resolver = new SearchConfigurationResolver(mockSearchConfigurationDao, mockNodeDAO);
	}

	@Test
	public void testResolveWithExplicitId() {
		SearchConfiguration config = new SearchConfiguration();
		when(mockSearchConfigurationDao.get("config-1")).thenReturn(Optional.of(config));

		// call under test
		Optional<SearchConfiguration> result = resolver.resolve("config-1", "syn456");

		assertEquals(Optional.of(config), result);
		verify(mockSearchConfigurationDao).get("config-1");
		verifyNoMoreInteractions(mockNodeDAO);
	}

	@Test
	public void testResolveWithBindingFallback() {
		SearchConfigBinding binding = new SearchConfigBinding();
		binding.setSearchConfigurationId("config-2");

		when(mockNodeDAO.getEntityIdOfFirstBoundSearchConfig(456L)).thenReturn(Optional.of(456L));
		when(mockSearchConfigurationDao.getSearchConfigBindingForObject(456L, "entity"))
			.thenReturn(Optional.of(binding));

		SearchConfiguration config = new SearchConfiguration();
		when(mockSearchConfigurationDao.get("config-2")).thenReturn(Optional.of(config));

		// call under test
		Optional<SearchConfiguration> result = resolver.resolve(null, "syn456");

		assertEquals(Optional.of(config), result);
		verify(mockSearchConfigurationDao).get("config-2");
	}

	@Test
	public void testResolveWithNoConfigReturnsEmpty() {
		when(mockNodeDAO.getEntityIdOfFirstBoundSearchConfig(456L)).thenReturn(Optional.empty());

		// call under test
		Optional<SearchConfiguration> result = resolver.resolve(null, "syn456");

		assertTrue(result.isEmpty());
	}

	@Test
	public void testResolveWithExplicitIdNotFound() {
		when(mockSearchConfigurationDao.get("config-missing")).thenReturn(Optional.empty());

		// call under test
		Optional<SearchConfiguration> result = resolver.resolve("config-missing", "syn456");

		assertTrue(result.isEmpty());
		verify(mockSearchConfigurationDao).get("config-missing");
	}

	@Test
	public void testResolveWithNullParentId() {
		// call under test
		Optional<SearchConfiguration> result = resolver.resolve(null, null);

		assertTrue(result.isEmpty());
		verifyNoMoreInteractions(mockNodeDAO);
	}

	@Test
	public void testResolveWithEmptyExplicitId() {
		when(mockNodeDAO.getEntityIdOfFirstBoundSearchConfig(456L)).thenReturn(Optional.empty());

		// call under test
		Optional<SearchConfiguration> result = resolver.resolve("", "syn456");

		assertTrue(result.isEmpty());
		verify(mockNodeDAO).getEntityIdOfFirstBoundSearchConfig(456L);
	}

	@Test
	public void testResolveWithBindingButConfigDeleted() {
		SearchConfigBinding binding = new SearchConfigBinding();
		binding.setSearchConfigurationId("config-deleted");

		when(mockNodeDAO.getEntityIdOfFirstBoundSearchConfig(456L)).thenReturn(Optional.of(456L));
		when(mockSearchConfigurationDao.getSearchConfigBindingForObject(456L, "entity"))
			.thenReturn(Optional.of(binding));
		when(mockSearchConfigurationDao.get("config-deleted")).thenReturn(Optional.empty());

		// call under test
		Optional<SearchConfiguration> result = resolver.resolve(null, "syn456");

		assertTrue(result.isEmpty());
	}

	@Test
	public void testResolveWithEmptyParentId() {
		// call under test
		Optional<SearchConfiguration> result = resolver.resolve(null, "");

		assertTrue(result.isEmpty());
		verifyNoMoreInteractions(mockNodeDAO);
	}
}
