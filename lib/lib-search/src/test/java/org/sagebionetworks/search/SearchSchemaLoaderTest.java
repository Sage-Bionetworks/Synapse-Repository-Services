package org.sagebionetworks.search;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.amazonaws.services.cloudsearchv2.model.IndexField;

@Ignore// Turned off until the dao is turned on.
public class SearchSchemaLoaderTest {
	
	@Test
	public void testLoadSearchDomainSchema() throws IOException{
		List<IndexField> list = SearchSchemaLoader.loadSearchDomainSchema();
		assertNotNull(list);
		// We currently have 18 index fields
		assertEquals(18, list.size());
	}

}
