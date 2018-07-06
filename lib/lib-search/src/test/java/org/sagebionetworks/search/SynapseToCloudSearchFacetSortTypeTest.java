package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.search.query.SearchFacetSort;
import org.sagebionetworks.search.awscloudsearch.SynapseToCloudSearchFacetSortType;


public class SynapseToCloudSearchFacetSortTypeTest {

	@Test
	public void testEnumMapping(){
		assertEquals("A one-to-one mapping must exist between SynapseToCloudSearchFacetSortType and SearchFacetSort. " +
						"Please add a new enum entry in SynapseToCloudSearchFacetSortType.",
				SearchFacetSort.values().length, SynapseToCloudSearchFacetSortType.values().length);

	}

	@Test
	public void testGetCloudSearchSortTypeFor(){
		assertEquals(SynapseToCloudSearchFacetSortType.count, SynapseToCloudSearchFacetSortType.getCloudSearchSortTypeFor(SearchFacetSort.COUNT));
		assertEquals(SynapseToCloudSearchFacetSortType.bucket, SynapseToCloudSearchFacetSortType.getCloudSearchSortTypeFor(SearchFacetSort.ALPHA));
	}
}
