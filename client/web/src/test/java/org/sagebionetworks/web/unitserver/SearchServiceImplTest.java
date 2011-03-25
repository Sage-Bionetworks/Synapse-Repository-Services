package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.ws.rs.core.UriBuilder;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.web.server.ColumnConfigProvider;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.server.RestTemplateProviderImpl;
import org.sagebionetworks.web.server.ServerConstants;
import org.sagebionetworks.web.server.servlet.SearchServiceImpl;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.TableResults;
import org.sagebionetworks.web.util.LocalStubLauncher;
import org.sagebionetworks.web.util.ServerPropertiesUtils;
import org.springframework.web.client.RestTemplate;

import com.sun.grizzly.http.SelectorThread;
import com.sun.istack.logging.Logger;

public class SearchServiceImplTest {
	
	public static Logger logger = Logger.getLogger(SearchServiceImplTest.class);
	
	private static SelectorThread selector = null;
	
	private static String serviceHost = "localhost";
	private static int servicePort = 9998;
	private static URL serviceUrl = null;
	private static RestTemplateProvider provider = null;
	
	private static SearchServiceImpl service;
	private static ColumnConfigProvider columnConfigProvider;
	private static String defultDatasetCols;
	private static String columnConfigFile;
	
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
		
		
		// Create a version of the service with all of the data stubbed.
		Properties props = ServerPropertiesUtils.loadProperties();
		
		defultDatasetCols = props.getProperty(ServerConstants.KEY_DEFAULT_DATASET_COLS);
		assertNotNull(defultDatasetCols);
		columnConfigFile = props.getProperty(ServerConstants.KEY_COLUMN_CONFIG_XML_FILE);
		
		// Create the service
		service = new SearchServiceImpl();
		
		// Create the column config from the classpath
		columnConfigProvider = new ColumnConfigProvider(columnConfigFile);
		columnConfigProvider.setDefaultDatasetColumns(defultDatasetCols);
		service.setColunConfigProvider(columnConfigProvider);
		service.setRestTemplate(provider);
		service.setRootUrl(serviceUrl.toString());
	}
	
	@AfterClass
	public static void afterClass(){
		// Shut down the grizzly container at the end of this suite.
		if(selector != null){
			selector.stopEndpoint();
		}
	}
	
	/**
	 * Clear all of the data in the stub service.
	 */
	private static void clearStubData(){
		RestTemplate template = provider.getTemplate();
		String url = serviceUrl+"repo/v1/query/clear/all";
		String results = template.getForObject(url, String.class, new TreeMap());
		logger.info(results);
	}
	
	/**
	 * Clear all of the data in the stub service.
	 */
	private static void generateRandomData(int number){
		RestTemplate template = provider.getTemplate();
		String url = serviceUrl+"repo/v1/query/populate/random?number={numberKey}";
		Map<String, String> map = new TreeMap<String,String>();
		map.put("numberKey", Integer.toString(number));
		String results = template.getForObject(url, String.class, map);
		logger.info(results);
	}
	
	@Test
	public void testDatasetDefaults(){
		String[] split = defultDatasetCols.split(",");
		List<String> datasetDefautls = service.getDefaultColumnIds(ObjectType.dataset.name());
		assertNotNull(datasetDefautls);
		// It should contain all of the splits
		for(int i=0; i<split.length; i++){
			assertTrue(datasetDefautls.contains(split[i].trim()));
		}
	}
	
	@Test
	public void testPreprocessSelect(){
		// If we pass a null or empty list then we should get the defaults
		List<String> processed = service.getVisibleColumns(ObjectType.dataset.name(), null);
		assertNotNull(processed);
		// Should match the defaults
		List<String> defaults = service.getDefaultColumnIds(ObjectType.dataset.name());
		assertEquals(defaults, processed);
		// Same for an empty list
		processed = service.getVisibleColumns(ObjectType.dataset.name(), new ArrayList<String>());
		assertEquals(defaults, processed);
		
		// Now use non-null non-empty
		List<String> select = new ArrayList<String>();
		select.add("someId");
		processed = service.getVisibleColumns(ObjectType.dataset.name(), select);
		assertEquals(select, processed);
		
	}
	
	
	@Test
	public void testDefaultDatasetColumns() throws IOException{
		// Create a new 
		// Now make sure we can get the list out
		List<String> list = service.getDefaultColumnIds(ObjectType.dataset.name());
		assertNotNull(list);
		assertTrue(list.contains("dataset.NameLink"));
		assertTrue(list.contains("dataset.layerTypeIcons"));
		assertTrue(list.contains("dataset.status"));
		assertTrue(list.contains("dataset.Number_of_Samples"));
		
		assertNotNull(columnConfigProvider);
		// Make sure all of the columns can be found
		for(int i=0; i<list.size(); i++){
			String key = list.get(i);
			HeaderData col = columnConfigProvider.get(key);
			assertNotNull("Could not find the column information for a default dataset column: "+key, col);
			// Make sure the key matches
			assertEquals(key, col.getId());
		}
	}
	
	
	@Test
	public void testGetColumnsForResults(){
		// Null should not be allowed.
		try{
			service.getColumnsForResults(null);
			fail("Null is not supported");
		}catch(IllegalArgumentException e){
			// expected
		}
		// You cannot ask for an unknown coumns
		List<String> select = new ArrayList<String>();
		select.add("bogusId");
		try{
			service.getColumnsForResults(select);
			fail("Unknown ids are not supported");
		}catch(IllegalArgumentException e){
			// expected
		}
		// Create a select list composed of all known columns
		select = new ArrayList<String>();
		Iterator<String> it = columnConfigProvider.getKeyIterator();
		while(it.hasNext()){
			select.add(it.next());
		}
		assertTrue(select.size() > 0);
		// Get all of the columns
		List<HeaderData> results = service.getColumnsForResults(select);
		assertNotNull(results);
		assertEquals(select.size(), results.size());
	}
	
	@Test
	public void testQueryWithNoData(){
		clearStubData();
		SearchParameters query = new SearchParameters();
		query.setFromType(ObjectType.dataset.name());
		TableResults results = service.executeSearch(query);
		assertNotNull(results);
		// There should be no rows to start
		assertEquals(0, results.getTotalNumberResults());
		assertNotNull(results.getRows());
	}
	
	@Test
	public void testQueryPagination(){
		// Populate the stub with random data
		clearStubData();
		int totalRows = 50;
		generateRandomData(totalRows);
		
		SearchParameters query = new SearchParameters();
		int limit = 15;
		query.setLimit(limit);
		query.setOffset(3);
		query.setFromType(ObjectType.dataset.name());
		TableResults results = service.executeSearch(query);
		assertNotNull(results);
		// There should be no rows to start
		assertEquals(totalRows, results.getTotalNumberResults());
		List<Map<String, Object>> rows = results.getRows();
		assertNotNull(rows);
		assertEquals(limit, rows.size());
	}

	


}
