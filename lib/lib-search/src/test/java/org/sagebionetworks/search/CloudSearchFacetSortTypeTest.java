package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.search.query.SearchFacetSort;


public class CloudSearchFacetSortTypeTest {

	@Test
	public void testEnumMapping(){
		assertEquals("A one-to-one mapping must exist between CloudSearchFacetSortType and SearchFacetSort. " +
						"Please add a new enum entry in CloudSearchFacetSortType.",
				SearchFacetSort.values().length, CloudSearchFacetSortType.values().length);

	}

	@Test
	public void testGetCloudSearchSortTypeFor(){
		assertEquals(CloudSearchFacetSortType.COUNT, CloudSearchFacetSortType.getCloudSearchSortTypeFor(SearchFacetSort.COUNT));
		assertEquals(CloudSearchFacetSortType.BUCKET, CloudSearchFacetSortType.getCloudSearchSortTypeFor(SearchFacetSort.ALPHA));
	}
}
