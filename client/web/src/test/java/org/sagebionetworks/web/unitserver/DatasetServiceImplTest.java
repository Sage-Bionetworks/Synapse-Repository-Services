package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.server.RestTemplateProviderImpl;
import org.sagebionetworks.web.server.servlet.DatasetServiceImpl;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.PaginatedDatasets;
import org.sagebionetworks.web.util.LocalDatasetServiceStub;
import org.sagebionetworks.web.util.LocalStubLauncher;
import org.springframework.web.client.RestTemplate;

import com.sun.grizzly.http.SelectorThread;
import com.sun.istack.logging.Logger;

/**
 * This is a unit test of the DatasetServiceImpl service.
 * It depends on a local stub implementation of the platform API
 * to be deployed locally.
 * 
 * @author jmhill
 *
 */
public class DatasetServiceImplTest {
	
	public static Logger logger = Logger.getLogger(DatasetServiceImplTest.class);
	
	/**
	 * This is our handle to the local grizzly container.
	 * It can be used to communicate with the container or 
	 * shut it down.
	 */
	private static SelectorThread selector = null;
	
	private static String serviceHost = "localhost";
	private static int servicePort = 9998;
	private static URL serviceUrl = null;
	
	// This is our service.
	private static DatasetServiceImpl service = null;
	private static RestTemplateProvider provider = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception{
		// Start the local stub implementation of the the platform
		// api.  This stub services runs in a local grizzly/jersey 
		// container.
		
		// First setup the url
		serviceUrl = UriBuilder.fromUri("http://"+serviceHost+"/").port(servicePort).build().toURL();
		// Now start the container
		selector = LocalStubLauncher.startServer(serviceHost, servicePort);
		
		// Create the RestProvider
		int timeout = 1000*60*2; // 2 minute timeout
		int maxTotalConnections = 1; // Only need one for this test.
		provider = new RestTemplateProviderImpl(timeout, maxTotalConnections);
		// Create the service
		service = new DatasetServiceImpl();
		// Inject the required values
		service.setRestTemplate(provider);
		service.setRootUrl(serviceUrl.toString());
	}
	
	/**
	 * Clear all of the data in the stub service.
	 */
	private static void clearStubData(){
		RestTemplate template = provider.getTemplate();
		String url = serviceUrl+"repo/v1/dataset/clear/all";
		String results = template.getForObject(url, String.class, new HashMap(0));
		logger.info(results);
	}
	
	/**
	 * Clear all of the data in the stub service.
	 */
	private static void generateRandomData(int number){
		RestTemplate template = provider.getTemplate();
		String url = serviceUrl+"repo/v1/dataset/populate/random?number={numberKey}";
		Map<String, String> map = new HashMap<String,String>(1);
		map.put("numberKey", Integer.toString(number));
		String results = template.getForObject(url, String.class, map);
		logger.info(results);
	}
	
	@AfterClass
	public static void afterClass(){
		// Shut down the grizzly container at the end of this suite.
		if(selector != null){
			selector.stopEndpoint();
		}
	}
	
	@After
	public void tearDown(){
		// After each test clean out all data
		clearStubData();
	}
	
	@Test
	public void testValidate(){
		// Create an instance that is not setup correctly
		DatasetServiceImpl dummy = new DatasetServiceImpl();
		try{
			dummy.validateService();
			fail("The dummy was not initialized so it should have failed validation");
		}catch(IllegalStateException e){
			//expected;
		}
		// Set the template
		dummy.setRestTemplate(provider);
		try{
			dummy.validateService();
			fail("The dummy was not initialized so it should have failed validation");
		}catch(IllegalStateException e){
			//expected;
		}
		// After setting the url it should pass validation.
		dummy.setRootUrl("dummy url");
	}
	
	@Test
	public void testGetAllDatasets(){
		// Make sure we are staring with no data
		clearStubData();
		// There should be no data starting out.
		PaginatedDatasets results = service.getAllDatasets(1, 5, null, true);
		assertNotNull(results);
		assertEquals(0, results.getTotalNumberOfResults());
		// The list should be empty but not null
		List<Dataset> datasets = results.getResults();
		assertNotNull(datasets);
		assertEquals(0, datasets.size());
		
		// Now generate some random datasets
		int totalNumberDatasets = 50;
		generateRandomData(50);
		results = service.getAllDatasets(1, 5, null, true);
		assertNotNull(results);
		assertEquals(totalNumberDatasets, results.getTotalNumberOfResults());
		// We should have some datasets this time.
		datasets = results.getResults();
		assertNotNull(datasets);
		assertEquals(5, datasets.size());
		// Get the first dataset
		Dataset first = datasets.get(0);
		assertNotNull(first);
		// It should have non-null fields
		assertNotNull(first.getId());
		assertNotNull(first.getName());
		assertNotNull(first.getCreationDate());
		assertNotNull(first.getCreator());
		assertNotNull(first.getDescription());
		assertNotNull(first.getLayerPreviews());
		System.out.println(first.getLayerPreviews());
		assertNotNull(first.getReleaseDate());
		assertNotNull(first.getStatus());
		assertNotNull(first.getVersion());
		
	}
	
	@Test
	public void testPaging(){
		// Make sure we are staring with no data
		clearStubData();
		// Start with 50
		generateRandomData(50);
		// Get the first page
		PaginatedDatasets results = service.getAllDatasets(0, 10, null, true);
		assertNotNull(results);
		List<Dataset> datasets = results.getResults();
		assertNotNull(datasets);
		Dataset first = datasets.get(0);
		assertNotNull(first);
		// Now get the next page
		results = service.getAllDatasets(10, 10, null, true);
		datasets = results.getResults();
		assertNotNull(datasets);
		Dataset firstPage2 = datasets.get(0);
		assertNotNull(firstPage2);
		// They should not be the same dataset
		assertTrue(!first.equals(firstPage2));
		// Now get past the total
		results = service.getAllDatasets(48, 10, null, true);
		datasets = results.getResults();
		assertNotNull(datasets);
		// There should only be two items the last page.
		assertEquals(3, datasets.size());
	}
	
	public void testGetDataset(){
		// Make sure we are staring with no data
		clearStubData();
		// Start with 50
		generateRandomData(50);
		// Search for bad data
		// Null and empty Ids should return null
		Dataset result = service.getDataset(null);
		assertNull(result);
		result = service.getDataset("");
		assertNull(result);
		result = service.getDataset("badId");
		assertNull(result);
		// Pick some datsets in the middle of the set.
		PaginatedDatasets results = service.getAllDatasets(25, 15, null, true);
		assertNotNull(results);
		// We should be able to find all datasets on this list
		List<Dataset> subList = results.getResults();
		assertEquals(15, subList.size());
		for(Dataset dataset: subList){
			Dataset found = service.getDataset(dataset.getId());
			assertNotNull(found);
			// The should be equal
			assertTrue(dataset.equals(found));
		}
		// Make sure we can find a datast even with white space around the id.
		Dataset last = subList.get(0);
		assertNotNull(last);
		Dataset found = service.getDataset("\n"+last.getId()+" ");
		assertNotNull(found);
		assertTrue(last.equals(found));
		
	}

}
