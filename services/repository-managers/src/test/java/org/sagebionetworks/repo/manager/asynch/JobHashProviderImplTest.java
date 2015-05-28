package org.sagebionetworks.repo.manager.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class JobHashProviderImplTest {
	
	TableRowManager mockTableRowManager;
	JobHashProvider provider;
	TableStatus tableStatus;
	
	@Before
	public void before() throws NotFoundException, IOException{
		mockTableRowManager = Mockito.mock(TableRowManager.class);
		provider = new JobHashProviderImpl();
		ReflectionTestUtils.setField(provider, "tableRowManager", mockTableRowManager);
		
		tableStatus = new TableStatus();
		tableStatus.setLastTableChangeEtag("someEtag");
		tableStatus.setResetToken("someResetToken");
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(anyString())).thenReturn(tableStatus);
	}
	
	@Test
	public void testHash(){
		DownloadFromTableRequest body = new DownloadFromTableRequest();
		body.setEntityId("syn123");
		body.setSql("select * from syn123");
		String hash = provider.getJobHash(body);
		assertEquals("EE5aWStFPTGljab55OyZig==", hash);
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
	public void testGetRequestObjectEtagTableNoRows() throws NotFoundException, IOException{
		DownloadFromTableRequest body1 = new DownloadFromTableRequest();
		body1.setEntityId("syn123");
		body1.setSql("select * from syn123");
		
		// an empty table will have a null lastTableChangeEtag
		tableStatus.setLastTableChangeEtag(null);
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(body1.getEntityId())).thenReturn(tableStatus);
		// call under test
		String etag = provider.getJobHash(body1);
		assertEquals("FyvNlH3dkEFV5Mw14GpBDQ==", etag);
	}
	
	@Test
	public void testGetRequestObjectEtagTableWithRows() throws NotFoundException, IOException{
		DownloadFromTableRequest body1 = new DownloadFromTableRequest();
		body1.setEntityId("syn123");
		body1.setSql("select * from syn123");
		// call under test
		String hash = provider.getJobHash(body1);
		assertEquals("EE5aWStFPTGljab55OyZig==", hash);
	}
	
	@Test
	public void testGetRequestObjectEtagQueryBundleRequest() throws NotFoundException, IOException{
		QueryBundleRequest body1 = new QueryBundleRequest();
		body1.setEntityId("syn123");
		// call under test
		String hash = provider.getJobHash(body1);
		assertEquals("fd8O/19tdy+e22+2prHyfQ==", hash);
	}
	
	@Test
	public void testGetRequestObjectEtagQueryNextPageToken() throws NotFoundException, IOException{
		QueryNextPageToken body1 = new QueryNextPageToken();
		body1.setEntityId("syn123");
		// call under test
		String hash = provider.getJobHash(body1);
		assertEquals("ews2Ed37Q7e5aMDIJ4OhtA==", hash);
	}
	
}
