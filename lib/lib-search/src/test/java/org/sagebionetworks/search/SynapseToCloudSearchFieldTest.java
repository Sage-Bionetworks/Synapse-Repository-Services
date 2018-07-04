package org.sagebionetworks.search;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.amazonaws.services.cloudsearchv2.model.IndexField;
import org.sagebionetworks.search.awscloudsearch.SynapseToCloudSearchField;

public class SynapseToCloudSearchFieldTest {
	//TODO: add more tests
	
	@Test
	public void testLoadSearchDomainSchema() throws IOException{
		List<IndexField> list = SynapseToCloudSearchField.loadSearchDomainSchema();
		assertNotNull(list);
		// We currently have 18 index fields
		assertEquals(18, list.size());
	}

	@Test
	public void testCloudSearchFieldFor(){

	}
}
