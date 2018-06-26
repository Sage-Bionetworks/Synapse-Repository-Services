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
import static org.sagebionetworks.search.CloudSearchIndexFields.*;

import com.amazonaws.services.cloudsearchv2.model.IndexField;
import org.sagebionetworks.repo.model.search.query.SearchFieldName;

public enum CloudSearchFields {
	/***
	 * TODO: This class should be the "Truth" on the configuration for search index. The SearchDomainSetupImpl class should be refactored to use this enum to initialize the search index.
	 */
	NAME(SearchFieldName.Name, INDEX_FIELD_NAME),
	TISSUE(SearchFieldName.TissueAnnotation, INDEX_FIELD_TISSUE),
	ENTITY_TYPE(SearchFieldName.EntityType, INDEX_FIELD_NODE_TYPE),
	DISEASE(SearchFieldName.DiseaseAnnotation, INDEX_FIELD_DISEASE),
	MODIFIED_BY(SearchFieldName.ModifiedBy, INDEX_FIELD_MODIFIED_BY),
	CREATED_BY(SearchFieldName.CreatedBy),
	NUM_SAMPLES(SearchFieldName.NumSamplesAnnotation),
	CREATED_ON(SearchFieldName.CreatedOn),
	MODIFIED_ON(SearchFieldName.ModifiedOn),
	DESCRIPTION(SearchFieldName.Description),
	CONSORTIUM(SearchFieldName.ConsortiumAnnotation),
	ID(SearchFieldName.Id,);

	final SearchFieldName synapseSearchFieldName;
	final IndexField indexField;

	CloudSearchFields(SearchFieldName synapseSearchFieldName, IndexField indexField){
		this.synapseSearchFieldName = synapseSearchFieldName;

	}


}
