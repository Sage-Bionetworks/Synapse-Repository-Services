package org.sagebionetworks.search;


import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_ACL;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_BOOST;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_CONSORTIUM;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_CREATED_BY;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_CREATED_ON;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_DESCRIPTION;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_DISEASE;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_ETAG;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_MODIFIED_BY;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_MODIFIED_ON;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_NAME;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_NODE_TYPE;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_NUM_SAMPLES;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_PARENT_ID;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_PLATFORM;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_REFERENCE;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_TISSUE;
import static org.sagebionetworks.search.CloudSearchIndexFields.INDEX_FIELD_UPDATE_ACL;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.amazonaws.services.cloudsearchv2.model.IndexField;
import com.amazonaws.services.cloudsearchv2.model.IndexFieldType;
import org.sagebionetworks.repo.model.search.query.SearchFieldName;
import org.sagebionetworks.util.ValidateArgument;

public enum SynapseToCloudSearchField {
	/***
	 * TODO: This class should be the "Truth" on the configuration for search index. The SearchDomainSetupImpl class should be refactored to use this enum to initialize the search index.
	 */
	NAME(SearchFieldName.Name, INDEX_FIELD_NAME),
	TISSUE(SearchFieldName.TissueAnnotation, INDEX_FIELD_TISSUE),
	ENTITY_TYPE(SearchFieldName.EntityType, INDEX_FIELD_NODE_TYPE),
	DISEASE(SearchFieldName.DiseaseAnnotation, INDEX_FIELD_DISEASE),
	MODIFIED_BY(SearchFieldName.ModifiedBy, INDEX_FIELD_MODIFIED_BY),
	CREATED_BY(SearchFieldName.CreatedBy, INDEX_FIELD_CREATED_BY),
	NUM_SAMPLES(SearchFieldName.NumSamplesAnnotation, INDEX_FIELD_NUM_SAMPLES),
	CREATED_ON(SearchFieldName.CreatedOn, INDEX_FIELD_CREATED_ON),
	MODIFIED_ON(SearchFieldName.ModifiedOn, INDEX_FIELD_MODIFIED_ON),
	DESCRIPTION(SearchFieldName.Description, INDEX_FIELD_DESCRIPTION),
	CONSORTIUM(SearchFieldName.ConsortiumAnnotation, INDEX_FIELD_CONSORTIUM),
	ID(SearchFieldName.Id, null),//Id is an implicit TODO: maybe use a wrapper around index fields

	//The ones below are not exposed in our API currently
	ETAG(null, INDEX_FIELD_ETAG),
	BOOST(null, INDEX_FIELD_BOOST),
	PARENT_ID(null, INDEX_FIELD_PARENT_ID),
	PLATFORM(null, INDEX_FIELD_PLATFORM),
	REFERENCE(null, INDEX_FIELD_REFERENCE),
	ACL(null, INDEX_FIELD_ACL),
	UPDATE_ACL(null, INDEX_FIELD_UPDATE_ACL);

	private final SearchFieldName synapseSearchFieldName;
	private final IndexField indexField;

	SynapseToCloudSearchField(SearchFieldName synapseSearchFieldName, IndexField indexField){
		this.synapseSearchFieldName = synapseSearchFieldName;
		this.indexField = indexField;
	}

	public IndexField getIndexField() {
		return indexField.clone();
	}

	public static List<IndexField> loadSearchDomainSchema() {
		return Arrays.stream(values())
				.map(SynapseToCloudSearchField::getIndexField)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}


}
