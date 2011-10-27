package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.web.server.ColumnConfig;
import org.sagebionetworks.web.server.ColumnConfigProvider;
import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.ColumnsForType;
import org.sagebionetworks.web.shared.FilterEnumeration;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;
import org.sagebionetworks.web.unitclient.presenter.SampleHeaderData;

public class ColumnConfigProviderTest {
	
	// Loaded form the "ColumnConfigurationV2.xml" file
	ColumnConfigProvider provider;
	// Setup with sample data with cyclic dependencies.
	ColumnConfigProvider providerWithCycles;
	
	SampleHeaderData a;
	SampleHeaderData b;
	SampleHeaderData c;
	SampleHeaderData d;
	
	@Before
	public void setup(){
		// This provide is setup with config xml
		provider = new ColumnConfigProvider("ColumnConfigurationV2.xml");
		// Load the filter enumerations
		provider.setFilterEnumerations("FilterEnumerations.xml");
		
		// Now the cyclic dependencies
		// Create some cyclic dependencies
		// This is our cycle
		// A -> B -> C -> A  and D has no dependencies
		a = new SampleHeaderData("dataset.a", "A", "A desc");
		b = new SampleHeaderData("dataset.b", "B", "B desc");
		c = new SampleHeaderData("dataset.c", "C", "C desc");
		d = new SampleHeaderData("dataset.d", "D", "D desc");
		
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
		String key = "dataset.name";
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
	public void testGetDependenciesWithCycles(){

		// Since we have a cycle they should all depend on the others
		// Test A
		List<String>dependencies = providerWithCycles.getColumnDependancies(a.getId());
		assertNotNull(dependencies);
		assertEquals(3, dependencies.size());
		assertTrue(dependencies.contains(b.getId()));
		assertTrue(dependencies.contains(c.getId()));
		
		// Test B
		dependencies = providerWithCycles.getColumnDependancies(b.getId());
		assertNotNull(dependencies);
		assertEquals(3, dependencies.size());
		assertTrue(dependencies.contains(a.getId()));
		assertTrue(dependencies.contains(c.getId()));
		
		// Test C
		dependencies = providerWithCycles.getColumnDependancies(c.getId());
		assertNotNull(dependencies);
		assertEquals(3, dependencies.size());
		assertTrue(dependencies.contains(a.getId()));
		assertTrue(dependencies.contains(b.getId()));

	}
	
	@Test
	public void testAddAllDependancies(){
		// Now request D & C with their dependencies
		List<String> input = new ArrayList<String>();
		input.add(d.getId());
		input.add(c.getId());
		
		// This is what we expect as a result
		List<String> expectedOut = new ArrayList<String>();
		expectedOut.add(d.getId());
		expectedOut.add(c.getId());
		expectedOut.add(a.getId());
		expectedOut.add(b.getId());
		List<String> results = providerWithCycles.addAllDependancies(input);
		assertNotNull(results);
		// Does it match what we expected
		assertEquals(expectedOut, results);
		
	}
	
	@Test
	public void testColumnsForType(){
		// The cache should start off empty.
		Map<String, ColumnsForType> cache = providerWithCycles.getColumnsForTypeCache();
		assertNotNull(cache);
		assertEquals(0, cache.size());
		// Set it up with the default columns.
		providerWithCycles.setDefaultDatasetColumns(b.getId()+","+a.getId());
		// Add the additional columns with one duplicate from the defaults
		providerWithCycles.setAdditionalDatasetColumns(a.getId()+","+c.getId());
		// Now get the columns for datasts
		ColumnsForType cft = providerWithCycles.getColumnsForType(ObjectType.dataset.name());
		assertNotNull(cft);
		assertNotNull(cft.getType());
		assertNotNull(cft.getDefaultColumns());
		assertNotNull(cft.getAdditionalColumns());
		
		// The defaults should contain b, then a.
		assertEquals(2, (cft.getDefaultColumns().size()));
		assertEquals(b.getId(),cft.getDefaultColumns().get(0).getId());
		assertEquals(a.getId(),cft.getDefaultColumns().get(1).getId());
		// The additional should just contain c
		assertEquals(1, (cft.getAdditionalColumns().size()));
		assertEquals(c.getId(),cft.getAdditionalColumns().get(0).getId());
		
		// Now make sure the cache was initialized
		cache = providerWithCycles.getColumnsForTypeCache();
		assertNotNull(cache);
		assertEquals(1, cache.size());
		assertEquals(cft, cache.get(ObjectType.dataset.name()));
	}
	
	@Test
	public void testGetFilterEnumerations(){
		List<FilterEnumeration> list = provider.getFilterEnumerations();
		assertNotNull(list);
		assertTrue(list.size() > 0);
	}

}
