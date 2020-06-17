package org.sagebionetworks.repo.manager.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class JobHashProviderImplTest {
	
	@Mock
	TableManagerSupport mockTableManagerSupport;
	@InjectMocks
	JobHashProviderImpl provider;
	

	TableStatus tableStatus;
	
	@Before
	public void before() throws NotFoundException, IOException{
		tableStatus = new TableStatus();
		tableStatus.setLastTableChangeEtag("someEtag");
		tableStatus.setResetToken("someResetToken");
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(any(IdAndVersion.class))).thenReturn(tableStatus);
		when(mockTableManagerSupport.getTableType(any(IdAndVersion.class))).thenReturn(ObjectType.TABLE);
	}

	@Test
	public void testHash(){
		DownloadFromTableRequest body = new DownloadFromTableRequest();
		body.setEntityId("syn123");
		body.setSql("select * from syn123");
		String hash = provider.getJobHash(body);
		assertEquals("104e5a592b453d31a58da6f9e4ec998a", hash);
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
		IdAndVersion idAndVersion = IdAndVersion.parse(body1.getEntityId());
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(idAndVersion)).thenReturn(tableStatus);
		// call under test
		String etag = provider.getJobHash(body1);
		assertEquals("172bcd947ddd904155e4cc35e06a410d", etag);
	}

	@Test
	public void testGetRequestObjectEtagTableWithRows() throws NotFoundException, IOException{
		DownloadFromTableRequest body1 = new DownloadFromTableRequest();
		body1.setEntityId("syn123");
		body1.setSql("select * from syn123");
		// call under test
		String hash = provider.getJobHash(body1);
		assertEquals("104e5a592b453d31a58da6f9e4ec998a", hash);
	}

	@Test
	public void testGetRequestObjectEtagQueryBundleRequest() throws NotFoundException, IOException{
		QueryBundleRequest body1 = new QueryBundleRequest();
		body1.setEntityId("syn123");
		Query query = new Query();
		query.setSql("select * from syn123");
		body1.setQuery(query);
		// call under test
		String hash = provider.getJobHash(body1);
		assertEquals("783a9c542fff9a43542046eed61a15df", hash);
	}

	@Test
	public void testGetRequestObjectEtagQueryNextPageToken() throws NotFoundException, IOException{
		QueryNextPageToken body1 = TableQueryUtils.createNextPageToken("SELECT * FROM SYN123", null, 100L, 10L,  null);
		// call under test
		String hash = provider.getJobHash(body1);
		assertEquals("8acd1d8b465c43c34196a6f3026c08ba", hash);
	}
	
}
