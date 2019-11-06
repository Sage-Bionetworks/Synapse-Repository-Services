package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.search.FacetTypeNames;

import com.amazonaws.services.cloudsearchv2.model.IndexFieldType;

public class IndexFieldToSynapseFacetTypeTest {

	@Test
	public void testAllValues(){
		assertEquals(FacetTypeNames.LITERAL, IndexFieldToSynapseFacetType.getSynapseFacetType(IndexFieldType.Literal));
		assertEquals(FacetTypeNames.CONTINUOUS, IndexFieldToSynapseFacetType.getSynapseFacetType(IndexFieldType.Int));
		assertEquals(FacetTypeNames.DATE, IndexFieldToSynapseFacetType.getSynapseFacetType(IndexFieldType.Date));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnexpectedIndexField(){
		IndexFieldToSynapseFacetType.getSynapseFacetType(IndexFieldType.DoubleArray);
	}
}
