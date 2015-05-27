package org.sagebionetworks.repo.manager.asynch;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.springframework.test.util.ReflectionTestUtils;

public class JobHashProviderImplTest {
	
	TableRowTruthDAO mockTableDao;
	JobHashProvider provider;
	
	@Before
	public void before(){
		mockTableDao = Mockito.mock(TableRowTruthDAO.class);
		provider = new JobHashProviderImpl();
		ReflectionTestUtils.setField(provider, "tableRowTruthDao", mockTableDao);
	}
	
	@Test
	public void testHash(){
		DownloadFromTableRequest body = new DownloadFromTableRequest();
		body.setEntityId("syn123");
		body.setSql("select * from syn123");
		String hash = provider.getJobHash(body);
		assertEquals("sd1zQvpC67saUigIElscOg==", hash);
	}
	
	@Test
	public void testHashNotEquals(){
		DownloadFromTableRequest body1 = new DownloadFromTableRequest();
		body1.setEntityId("syn123");
		body1.setSql("select * from syn123");
		String hash1 = provider.getJobHash(body1);
		
		DownloadFromTableRequest body2 = new DownloadFromTableRequest();
		body2.setEntityId("syn123");
		body2.setSql("select * from syn123 limit 1");
		String hash2 = provider.getJobHash(body2);
		assertFalse(hash1.equals(hash2));
	}
	
	/**
	 * For now make the hash case sensitive.
	 */
	@Test
	public void testHashCaseSensitive(){
		DownloadFromTableRequest body1 = new DownloadFromTableRequest();
		body1.setEntityId("syn123");
		body1.setSql("select * from syn123");
		String hash1 = provider.getJobHash(body1);
		
		DownloadFromTableRequest body2 = new DownloadFromTableRequest();
		body2.setEntityId("syn123");
		body2.setSql(body1.getSql().toUpperCase());
		String hash2 = provider.getJobHash(body2);
		assertFalse(hash1.equals(hash2));
	}

	@Test
	public void testGetRequestObjectEtagTableNoRows(){
		DownloadFromTableRequest body1 = new DownloadFromTableRequest();
		body1.setEntityId("syn123");
		body1.setSql("select * from syn123");
		// null for empty tables.
		when(mockTableDao.getLastTableRowChange(body1.getEntityId())).thenReturn(null);
		// call under test
		String etag = provider.getRequestObjectEtag(body1);
		assertEquals(null, etag);
	}
	
	@Test
	public void testGetRequestObjectEtagTableWithRows(){
		DownloadFromTableRequest body1 = new DownloadFromTableRequest();
		body1.setEntityId("syn123");
		body1.setSql("select * from syn123");
		// Not null
		TableRowChange lastChange = new TableRowChange();
		lastChange.setEtag("theEtag");
		when(mockTableDao.getLastTableRowChange(body1.getEntityId())).thenReturn(lastChange);
		// call under test
		String etag = provider.getRequestObjectEtag(body1);
		assertEquals(lastChange.getEtag(), etag);
	}
	
	@Test
	public void testGetRequestObjectEtagQueryBundleRequest(){
		QueryBundleRequest body1 = new QueryBundleRequest();
		body1.setEntityId("syn123");
		// Not null
		TableRowChange lastChange = new TableRowChange();
		lastChange.setEtag("theEtag");
		when(mockTableDao.getLastTableRowChange(body1.getEntityId())).thenReturn(lastChange);
		// call under test
		String etag = provider.getRequestObjectEtag(body1);
		assertEquals(lastChange.getEtag(), etag);
	}
	
	@Test
	public void testGetRequestObjectEtagQueryNextPageToken(){
		QueryNextPageToken body1 = new QueryNextPageToken();
		body1.setEntityId("syn123");
		// Not null
		TableRowChange lastChange = new TableRowChange();
		lastChange.setEtag("theEtag");
		when(mockTableDao.getLastTableRowChange(body1.getEntityId())).thenReturn(lastChange);
		// call under test
		String etag = provider.getRequestObjectEtag(body1);
		assertEquals(lastChange.getEtag(), etag);
	}
	
	@Test
	public void testGetRequestObjectEtagUploadToTableRequest(){
		UploadToTableRequest body1 = new UploadToTableRequest();
		body1.setTableId("syn123");
		// Not null
		TableRowChange lastChange = new TableRowChange();
		lastChange.setEtag("theEtag");
		when(mockTableDao.getLastTableRowChange(body1.getTableId())).thenReturn(lastChange);
		// call under test
		String etag = provider.getRequestObjectEtag(body1);
		assertEquals(null, etag);
		verify(mockTableDao, never()).getLastTableRowChange(anyString());
	}
}
