package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.cloudsearch.model.AccessPoliciesStatus;
import com.amazonaws.services.cloudsearch.model.DomainStatus;
import com.amazonaws.services.cloudsearch.model.IndexField;
import com.amazonaws.services.cloudsearch.model.IndexFieldStatus;
import com.amazonaws.services.cloudsearch.model.OptionState;

/**
 * This is an integration test for the SearchDaoImpl
 * 
 * @author jmhill
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:search-dao.spb.xml" })
public class SearchDaoImplAutowireTest {
	
	public static final long MAX_WAIT_TIME = 60*1000; // one minute
	
	@Autowired
	SearchDao searchDao;
	@Autowired
	SearchDomainSetup searchDomainSetup;
	
	@Test
	public void testSetup(){
		assertNotNull(searchDao);
		// Validate that the search index is ready to go.
		DomainStatus status = searchDomainSetup.getDomainStatus();
		assertNotNull(status);
		// The domain should be ready
		assertTrue("Search domain has not been created", status.isCreated());
		assertFalse("Search domain is processing", status.isProcessing());
		assertFalse("Search domain requies indexing", status.getRequiresIndexDocuments());
		assertFalse("Search domain has been deleted", status.isDeleted());
		assertNotNull(status.getDocService());
		assertNotNull(status.getSearchService());
		assertNotNull(status.getDocService().getArn());
		assertNotNull(status.getSearchService().getArn());
		assertNotNull(status.getDocService().getEndpoint());
		assertNotNull(status.getSearchService().getEndpoint());
	}
	
	@Test
	public void testIndexFields(){
		// This is our expected list of fields.
		List<IndexField> expected = SearchSchemaLoader.loadSearchDomainSchema();
		// Do we have the expected index fields?
		List<IndexFieldStatus> currentFields = searchDomainSetup.getIndexFieldStatus();
		assertNotNull(currentFields);
		assertEquals(expected.size(), currentFields.size());
		// Find each and validate it.
		for(IndexField field: expected){
			// find the status for this field
			IndexFieldStatus status = findByName(currentFields, field.getIndexFieldName());
			assertNotNull("Faild to find an index field with the name: "+field.getIndexFieldName(), status);
			// Is it active?
			assertEquals(OptionState.Active, OptionState.valueOf(status.getStatus().getState()));
			// Does it match the expected
			assertEquals(field, status.getOptions());
		}
	}

	@Test
	public void testAccessPolicies(){
		AccessPoliciesStatus status = searchDomainSetup.getAccessPoliciesStatus();
		assertNotNull(status);
		// Is it active?
		assertEquals(OptionState.Active, OptionState.valueOf(status.getStatus().getState()));
		// get the json
		assertNotNull(status.getOptions());
		// Is there an access policy for both the search arn and document arn?
		DomainStatus domainStatus = searchDomainSetup.getDomainStatus();
		assertNotNull(status);
		assertTrue(status.getOptions().indexOf(domainStatus.getDocService().getArn()) > 0);
		assertTrue(status.getOptions().indexOf(domainStatus.getSearchService().getArn()) > 0);
	}
	
	@Test
	public void testCRUD() throws ClientProtocolException, IOException, HttpClientHelperException, InterruptedException{
		// create a document, search for it, then delete it.
		String id = "abc";
		String etag = "4567";
		String parentId = "xyz";
		String name = "This is my name";
		searchDao.deleteDocument(id);
		// Now create the document
		Document document = new Document();
		document.setId(id);
		DocumentFields fields = new DocumentFields();
		document.setFields(fields);

		// Node fields
		document.setId(id);
		fields.setId(id); // this is redundant because document id
		// is returned in search results, but its cleaner to have this also show
		// up in the "data" section of AwesomeSearch results
		fields.setEtag(etag);
		fields.setParent_id(parentId);
		fields.setName(name);
		fields.setDescription("AABBCC one two thre");
		// Create this document
		searchDao.createOrUpdateSearchDocument(document);
		// Wait for the create
		waitForSearchCreateOrUpdate(id, etag);
		// Now make sure we can search for it
		SearchResults results = searchDao.executeSearch("q=AABBCC");
		assertNotNull(results);
		assertNotNull(results.getHits());
		assertEquals(1, results.getHits().size());
		Hit hit = results.getHits().get(0);
		assertEquals(id, hit.getId());
		
		// Make sure the document exists
		assertTrue(searchDao.doesDocumentExist(id, etag));
		assertFalse(searchDao.doesDocumentExist(id, "oldEtag"));
		
		// now update the document and try the search again.
		fields.setDescription("JJKKRRDD seven eight nine");
		etag += "0";
		fields.setEtag(etag);
		searchDao.createOrUpdateSearchDocument(document);
		// Wait for the create
		waitForSearchCreateOrUpdate(id, etag);
		// We should not be able to find it with the old string
		results = searchDao.executeSearch("q=AABBCC");
		assertNotNull(results);
		assertNotNull(results.getHits());
		assertEquals(0, results.getHits().size());
		// We should be able to find it with the updated value.
		results = searchDao.executeSearch("q=JJKKRRDD");
		assertNotNull(results);
		assertNotNull(results.getHits());
		assertEquals(1, results.getHits().size());
		hit = results.getHits().get(0);
		assertEquals(id, hit.getId());
		// Delete the document.
		searchDao.deleteDocument(id);
		waitForSearchDelete(id, etag);
		// We should not be able to find this document
		results = searchDao.executeSearch("bq=id:'"+id+"'");
		assertNotNull(results);
		assertNotNull(results.getHits());
		assertEquals(0, results.getHits().size());
		// It should not exists
		assertFalse(searchDao.doesDocumentExist(id, etag));
	}
	
	/**
	 * Helper to wait for a search document to be created or update.
	 * @param id
	 * @param etag
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 * @throws InterruptedException
	 */
	private void waitForSearchCreateOrUpdate(String id, String etag) throws ClientProtocolException, IOException, HttpClientHelperException, InterruptedException{
		long start = System.currentTimeMillis();
		while(!searchDao.doesDocumentExist(id, etag)){
			System.out.println(String.format("Waiting for Search document create, id: %1$s etag: %2$s", id, etag));
			Thread.sleep(2000);
			long elapse = System.currentTimeMillis()-start;
			assertTrue(String.format("Timed out waiting for Search document create, id: %1$s etag: %2$s", id, etag),elapse < MAX_WAIT_TIME);
		}
	}
	
	/**
	 * Helper to wait for a search document to be created or update.
	 * @param id
	 * @param etag
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 * @throws InterruptedException
	 */
	private void waitForSearchDelete(String id, String etag) throws ClientProtocolException, IOException, HttpClientHelperException, InterruptedException{
		long start = System.currentTimeMillis();
		while(searchDao.doesDocumentExist(id, etag)){
			System.out.println(String.format("Waiting for Search document delete, id: %1$s etag: %2$s", id, etag));
			Thread.sleep(2000);
			long elapse = System.currentTimeMillis()-start;
			assertTrue(String.format("Timed out waiting for Search document delete, id: %1$s etag: %2$s", id, etag),elapse < MAX_WAIT_TIME);
		}
	}
	
	/**
	 * Find a field by name.
	 * @param currentFields
	 * @param name
	 * @return
	 */
	private static IndexFieldStatus findByName(List<IndexFieldStatus> currentFields, String name){
		if(currentFields == null) throw new IllegalArgumentException("currentFields cannot be null");
		if(name == null) throw new IllegalArgumentException("Name cannot be null");
		for(IndexFieldStatus status: currentFields){
			if(status.getOptions().getIndexFieldName().equals(name)) return status;
		}
		// not found
		return null;
	}
}
