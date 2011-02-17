package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.web.server.ColumnConfigProvider;
import org.sagebionetworks.web.server.ServerConstants;
import org.sagebionetworks.web.server.servlet.SearchServiceImpl;
import org.sagebionetworks.web.shared.HeaderData;

public class SearchServiceImplTest {
	
	private static SearchServiceImpl service;
	private static ColumnConfigProvider columnConfigProvider;
	/**
	 * Setup the service once
	 * @throws IOException
	 */
	@BeforeClass
	public static void setup() throws IOException{
		// Create a version of the service with all of the data stubbed.
		Properties props = loadProperties();
		
		String defultDatasetCols = props.getProperty(ServerConstants.KEY_DEFAULT_DATASET_COLS);
		assertNotNull(defultDatasetCols);
		String columnConfigFile = props.getProperty(ServerConstants.KEY_COLUMN_CONFIG_XML_FILE);
		
		// Create the service
		service = new SearchServiceImpl();
		service.setDefaultDatasetColumns(defultDatasetCols);
		
		// Create the column config from the classpath
		columnConfigProvider = new ColumnConfigProvider(columnConfigFile);
		service.setColunConfigProvider(columnConfigProvider);
	}
	
	@Test
	public void testColumnConfig(){
		
	}
	
	
	@Test
	public void testDefaultDatasetColumns() throws IOException{
		// Create a new 
		// Now make sure we can get the list out
		List<String> list = service.getDefaultDatasetColumnIds();
		assertNotNull(list);
		assertTrue(list.contains("datasetNameLink"));
		assertTrue(list.contains("status"));
		assertTrue(list.contains("Number of Samples"));
		
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

	
	/**
	 * Helper to load the server properties off the classpath.
	 * @return
	 * @throws IOException
	 */
	private static Properties loadProperties() throws IOException{
		String propsFileName = "ServerConstants.properties";
		InputStream in = SearchServiceImpl.class.getClassLoader().getResourceAsStream(propsFileName);
		assertNotNull("Failed to load:"+propsFileName+" from the classpath", in);
		Properties props = new Properties();
		try{
			props.load(in);		
		}finally{
			if(in != null) in.close();
		}
		return props;
	}

}
