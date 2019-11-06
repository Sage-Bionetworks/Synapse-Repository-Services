package org.sagebionetworks.search.awscloudsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.sagebionetworks.search.SearchConstants.FIELD_CONSORTIUM;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_BY;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_DESCRIPTION;
import static org.sagebionetworks.search.SearchConstants.FIELD_DIAGNOSIS;
import static org.sagebionetworks.search.SearchConstants.FIELD_ID;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_BY;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_NAME;
import static org.sagebionetworks.search.SearchConstants.FIELD_NODE_TYPE;
import static org.sagebionetworks.search.SearchConstants.FIELD_ORGAN;
import static org.sagebionetworks.search.SearchConstants.FIELD_TISSUE;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.sagebionetworks.repo.model.search.query.SearchFieldName;

import com.amazonaws.services.cloudsearchv2.model.IndexField;
import com.google.common.collect.Sets;

public class SynapseToCloudSearchFieldTest {

	@Test
	public void testLoadSearchDomainSchema() throws IOException{
		List<IndexField> list = SynapseToCloudSearchField.loadSearchDomainSchema();
		assertNotNull(list);

		Set<String> expectedFieldNames = Sets.newHashSet("modified_on", "diagnosis", "consortium",
				"description", "tissue", "acl", "created_by", "reference", "node_type", "created_on",
				"update_acl", "parent_id", "name", "modified_by", "etag", "organ");

		Set<String> actualfieldNames = list.stream().map(IndexField::getIndexFieldName).collect(Collectors.toSet());
		// We currently have 16 index fields
		assertEquals(16, list.size());
		assertEquals(16, actualfieldNames.size());
		assertEquals(expectedFieldNames, actualfieldNames);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCloudSearchFieldFor_nullSearchFieldName(){
		SynapseToCloudSearchField.cloudSearchFieldFor((SearchFieldName) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCloudSearchFieldFor_nullString(){
		SynapseToCloudSearchField.cloudSearchFieldFor((String) null);
	}

	@Test
	public void testCloudSearchFieldForSearchFieldName_allResults(){
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.Id), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_ID);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.Name), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_NAME);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.EntityType), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_NODE_TYPE);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.ModifiedBy), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_MODIFIED_BY);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.ModifiedOn), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_MODIFIED_ON);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.CreatedBy), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CREATED_BY);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.CreatedOn), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CREATED_ON);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.Description), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_DESCRIPTION);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.Consortium), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CONSORTIUM);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.Diagnosis), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_DIAGNOSIS);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.Organ), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_ORGAN);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(SearchFieldName.Tissue), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_TISSUE);
	}

	@Test
	public void testCloudSearchFieldForString_allResults(){
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(FIELD_ID), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_ID);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(FIELD_NAME), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_NAME);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(FIELD_NODE_TYPE), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_NODE_TYPE);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(FIELD_MODIFIED_BY), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_MODIFIED_BY);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(FIELD_MODIFIED_ON), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_MODIFIED_ON);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(FIELD_CREATED_BY), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CREATED_BY);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(FIELD_CREATED_ON), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CREATED_ON);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(FIELD_DESCRIPTION), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_DESCRIPTION);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(FIELD_CONSORTIUM), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CONSORTIUM);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(FIELD_DIAGNOSIS), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_DIAGNOSIS);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(FIELD_ORGAN), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_ORGAN);
		assertEquals(SynapseToCloudSearchField.cloudSearchFieldFor(FIELD_TISSUE), CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_TISSUE);
	}
}
