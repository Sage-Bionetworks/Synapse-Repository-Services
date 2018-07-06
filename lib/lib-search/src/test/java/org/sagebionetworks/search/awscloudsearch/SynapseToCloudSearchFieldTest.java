package org.sagebionetworks.search.awscloudsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazonaws.services.cloudsearchv2.model.IndexField;
import com.amazonaws.services.cloudsearchv2.model.IndexFieldType;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.sagebionetworks.repo.model.search.query.SearchFieldName;

public class SynapseToCloudSearchFieldTest {

	@Test
	public void testLoadSearchDomainSchema() throws IOException{
		List<IndexField> list = SynapseToCloudSearchField.loadSearchDomainSchema();
		assertNotNull(list);

		Set<String> expectedFieldNames = Sets.newHashSet("modified_on", "disease", "consortium",
				"description", "tissue", "acl", "created_by", "platform", "reference", "node_type", "created_on",
				"update_acl", "parent_id", "name", "modified_by", "etag", "boost", "num_samples");

		Set<String> actualfieldNames = list.stream().map(IndexField::getIndexFieldName).collect(Collectors.toSet());
		// We currently have 18 index fields
		assertEquals(18, list.size());
		assertEquals(18, actualfieldNames.size());
		assertEquals(expectedFieldNames, actualfieldNames);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCloudSearchFieldFor_nullSearchFieldName(){
		SynapseToCloudSearchField.cloudSearchFieldFor(null);
	}

	@Test
	public void testCloudSearchFieldFor_allResults(){
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.Id), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_ID);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.Name), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_NAME);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.EntityType), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_NODE_TYPE);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.ModifiedBy), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_MODIFIED_BY);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.ModifiedOn), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_MODIFIED_ON);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.CreatedBy), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CREATED_BY);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.CreatedOn), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CREATED_ON);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.Description), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_DESCRIPTION);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.Consortium), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CONSORTIUM);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.Disease), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_DISEASE);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.NumSamples), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_NUM_SAMPLES);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.Tissue), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_TISSUE);
	}
}
