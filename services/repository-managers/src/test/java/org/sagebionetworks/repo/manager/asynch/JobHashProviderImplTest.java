package org.sagebionetworks.repo.manager.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class JobHashProviderImplTest {
	
	TableManagerSupport mockTableManagerSupport;
	JobHashProvider provider;
	TableStatus tableStatus;
	
	@Before
	public void before() throws NotFoundException, IOException{
		mockTableManagerSupport = Mockito.mock(TableManagerSupport.class);
		provider = new JobHashProviderImpl();
		ReflectionTestUtils.setField(provider, "tableManagerSupport", mockTableManagerSupport);
		
		tableStatus = new TableStatus();
		tableStatus.setLastTableChangeEtag("someEtag");
		tableStatus.setResetToken("someResetToken");
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(anyString())).thenReturn(tableStatus);
		when(mockTableManagerSupport.getTableType(anyString())).thenReturn(ObjectType.TABLE);
	}

	@Test
	public void testHash(){
		DownloadFromTableRequest body = new DownloadFromTableRequest();
		body.setEntityId("syn123");
		body.setSql("select * from syn123");
		String hash = provider.getJobHash(body);
		assertEquals("6e1004998c65dfe299ea947cd55a1bbe", hash);
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
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(body1.getEntityId())).thenReturn(tableStatus);
		// call under test
		String etag = provider.getJobHash(body1);
		assertEquals("a677a454999341a94f51f26b3ddd4d74", etag);
	}

	@Test
	public void testGetRequestObjectEtagTableWithRows() throws NotFoundException, IOException{
		DownloadFromTableRequest body1 = new DownloadFromTableRequest();
		body1.setEntityId("syn123");
		body1.setSql("select * from syn123");
		// call under test
		String hash = provider.getJobHash(body1);
		assertEquals("6e1004998c65dfe299ea947cd55a1bbe", hash);
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
		assertEquals("6ec21be4052a97470dddc248e01633f1", hash);
	}

	@Test
	public void testGetRequestObjectEtagQueryNextPageToken() throws NotFoundException, IOException{
		QueryNextPageToken body1 = TableQueryUtils.createNextPageToken("SELECT * FROM SYN123", null, 100L, 10L, true, null);
		// call under test
		String hash = provider.getJobHash(body1);
		assertEquals("a27d83c50b1666f2ed22b11f67598fad", hash);
	}
	
}
