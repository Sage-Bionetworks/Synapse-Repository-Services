package org.sagebionetworks.repo.manager.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.web.NotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class SearchDocumentDriverImplUnitTest {
	
	@Mock
	NodeDAO mockNodeDao;
	
	@InjectMocks
	SearchDocumentDriverImpl driver;

	@Test
	public void testGetEntityEtag() {
		String entityId = "syn123";
		String etag = "theEtag";
		when(mockNodeDao.isNodeAvailable(entityId)).thenReturn(true);
		when(mockNodeDao.peekCurrentEtag(entityId)).thenReturn(etag);
		// call under test
		Optional<String> etagOptional = driver.getEntityEtag(entityId);
		assertNotNull(etagOptional);
		assertEquals(etag, etagOptional.get());
	}
	
	@Test
	public void testGetEntityEtagNotAvailable() {
		String entityId = "syn123";
		String etag = "theEtag";
		when(mockNodeDao.isNodeAvailable(entityId)).thenReturn(false);
		when(mockNodeDao.peekCurrentEtag(entityId)).thenReturn(etag);
		// call under test
		Optional<String> etagOptional = driver.getEntityEtag(entityId);
		assertNotNull(etagOptional);
		assertFalse(etagOptional.isPresent());
		verify(mockNodeDao, never()).peekCurrentEtag(anyString());
	}
	
	@Test
	public void testGetEntityEtagNotFound() {
		String entityId = "syn123";
		when(mockNodeDao.isNodeAvailable(entityId)).thenReturn(true);
		when(mockNodeDao.peekCurrentEtag(entityId)).thenThrow(new NotFoundException("not found"));
		// call under test
		Optional<String> etagOptional = driver.getEntityEtag(entityId);
		assertNotNull(etagOptional);
		assertFalse(etagOptional.isPresent());
	}
}
