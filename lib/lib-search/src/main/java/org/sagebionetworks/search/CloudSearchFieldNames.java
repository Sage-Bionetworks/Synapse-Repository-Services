package org.sagebionetworks.search;


import static org.sagebionetworks.search.SearchConstants.FIELD_CONSORTIUM;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_BY;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_DESCRIPTION;
import static org.sagebionetworks.search.SearchConstants.FIELD_DISEASE;
import static org.sagebionetworks.search.SearchConstants.FIELD_ID;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_BY;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_NAME;
import static org.sagebionetworks.search.SearchConstants.FIELD_NODE_TYPE;
import static org.sagebionetworks.search.SearchConstants.FIELD_NUM_SAMPLES;
import static org.sagebionetworks.search.SearchConstants.FIELD_TISSUE;

import org.sagebionetworks.repo.model.search.query.SearchFieldName;

public enum CloudSearchFieldNames {
	//TODO: is this dupliating what tracks whether a field is facetable?
	// TODO: should we have config file that defines the search indicies instaed of code?
	NAME(SearchFieldName.Name, FIELD_NAME, false),
	TISSUE(SearchFieldName.TissueAnnotation, FIELD_TISSUE, true),
	ENTITY_TYPE(SearchFieldName.EntityType, FIELD_NODE_TYPE, true),
	DISEASE(SearchFieldName.DiseaseAnnotation, FIELD_DISEASE, true),
	MODIFIED_BY(SearchFieldName.ModifiedBy, FIELD_MODIFIED_BY, true),
	CREATED_BY(SearchFieldName.CreatedBy, FIELD_CREATED_BY, true),
	NUM_SAMPLES(SearchFieldName.NumSamplesAnnotation, FIELD_NUM_SAMPLES, true),
	CREATED_ON(SearchFieldName.CreatedOn, FIELD_CREATED_ON, true),
	MODIFIED_ON(SearchFieldName.ModifiedOn, FIELD_MODIFIED_ON, true),
	DESCRIPTION(SearchFieldName.Description, FIELD_DESCRIPTION, false),
	CONSORTIUM(SearchFieldName.ConsortiumAnnotation, FIELD_CONSORTIUM, true),
	ID(SearchFieldName.Id, FIELD_ID, false);

	final SearchFieldName synapseSearchFieldName;
	final String cloudSearchIndexName;
	final boolean isFaceted;

	CloudSearchFieldNames(SearchFieldName synapseSearchFieldName, String cloudSearchIndexName, boolean isFaceted){
		this.synapseSearchFieldName = synapseSearchFieldName;
		this.cloudSearchIndexName = cloudSearchIndexName;
		this.isFaceted = isFaceted;
	}
}
