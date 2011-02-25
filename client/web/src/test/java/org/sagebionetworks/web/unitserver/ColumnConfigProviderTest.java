package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.web.server.ColumnConfig;
import org.sagebionetworks.web.server.ColumnConfigProvider;
import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.unitclient.presenter.SampleHeaderData;

public class ColumnConfigProviderTest {
	
	// Loaded form the "ColumnConfigurationV2.xml" file
	ColumnConfigProvider provider;
	// Setup with sample data with cyclic dependencies.
	ColumnConfigProvider providerWithCycles;
	@Before
	public void setup(){
		// This provide is setup with config xml
		provider = new ColumnConfigProvider("ColumnConfigurationV2.xml");
		
		// Now the cyclic dependencies
		// Create some cyclic dependencies
		// This is our cycle
		// A -> B -> C -> A  and D has no dependencies
		SampleHeaderData a = new SampleHeaderData("a", "A", "A desc");
		SampleHeaderData b = new SampleHeaderData("b", "B", "B desc");
		SampleHeaderData c = new SampleHeaderData("c", "C", "C desc");
		SampleHeaderData d = new SampleHeaderData("d", "D", "D desc");
		
		// Set the dependencies
		// A -> B
		List<String> dependencies = new ArrayList<String>();
		dependencies.add(b.getId());
		a.setDependencyList(dependencies);
		
		// B -> C
		dependencies = new ArrayList<String>();
		dependencies.add(c.getId());
		b.setDependencyList(dependencies);
		
		// C -> A
		dependencies = new ArrayList<String>();
		dependencies.add(a.getId());
		c.setDependencyList(dependencies);
		
	
		List<HeaderData> flatList = new ArrayList<HeaderData>();
		flatList.add(a);
		flatList.add(b);
		flatList.add(c);
		flatList.add(d);
		
		ColumnConfig config = new ColumnConfig();
		config.setColumns(flatList);
		providerWithCycles = new ColumnConfigProvider(config);
	}
	
	@Test
	public void testConfigXml(){
		// Test that we can load the configuration file used at runtime
		assertNotNull(provider);
		// Make sure we can get the name column\
		String key = "name";
		HeaderData header = provider.get(key);
		assertNotNull(header);
		assertTrue(header instanceof ColumnInfo);
		ColumnInfo nameInfo = (ColumnInfo)header;
		assertEquals(key, nameInfo.getId());
		// Make sure the rest of the fields are not null
		assertNotNull(nameInfo.getDescription());
		assertNotNull(nameInfo.getDisplayName());
		assertNotNull(nameInfo.getType());
	}
	
	@Test
	public void testGetDependencies(){
		// Currently a datasets name link depends on two other columns, and one of those columns also has
		// dependencies.
		List<String> dependancies = provider.getColumnDependancies("datasetNameLink");
		assertNotNull(dependancies);
		assertTrue(dependancies.contains("name"));
		assertTrue(dependancies.contains("datasetUrl"));
		assertTrue(dependancies.contains("id"));
	}
	
	@Test
	public void testGetDependenciesWithCycles(){

		// Since we have a cycle they should all depend on the others
		// Test A
		List<String>dependencies = providerWithCycles.getColumnDependancies("a");
		assertNotNull(dependencies);
		assertEquals(3, dependencies.size());
		assertTrue(dependencies.contains("b"));
		assertTrue(dependencies.contains("c"));
		
		// Test B
		dependencies = providerWithCycles.getColumnDependancies("b");
		assertNotNull(dependencies);
		assertEquals(3, dependencies.size());
		assertTrue(dependencies.contains("a"));
		assertTrue(dependencies.contains("c"));
		
		// Test C
		dependencies = providerWithCycles.getColumnDependancies("c");
		assertNotNull(dependencies);
		assertEquals(3, dependencies.size());
		assertTrue(dependencies.contains("a"));
		assertTrue(dependencies.contains("b"));

	}
	
	@Test
	public void testAddAllDependancies(){
		// Now request D & C with their dependencies
		List<String> input = new ArrayList<String>();
		input.add("d");
		input.add("c");
		
		// This is what we expect as a result
		List<String> expectedOut = new ArrayList<String>();
		expectedOut.add("d");
		expectedOut.add("c");
		expectedOut.add("a");
		expectedOut.add("b");
		List<String> results = providerWithCycles.addAllDependancies(input);
		assertNotNull(results);
		// Does it match what we expected
		assertEquals(expectedOut, results);
		
	}

}
