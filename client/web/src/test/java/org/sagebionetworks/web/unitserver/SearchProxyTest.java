package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.web.client.SearchService;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.TableResults;

import com.gdevelop.gwt.syncrpc.SyncProxy;

public class SearchProxyTest {
	
	@Ignore
	@Test
	public void testProxy(){
		String url = "http://localhost:8888/Portal/";
		SearchService proxy = (SearchService) SyncProxy.newProxyInstance(SearchService.class, url, "search");
		assertNotNull(proxy);
		SearchParameters params = new SearchParameters();
		params.setFromType("dataset");
		TableResults results = proxy.executeSearch(params);
		assertNotNull(results);
		assertEquals(100, results.getTotalNumberResults());
	}

}
