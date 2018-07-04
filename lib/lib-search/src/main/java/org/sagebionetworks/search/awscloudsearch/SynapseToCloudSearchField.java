package org.sagebionetworks.search.awscloudsearch;


import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_ACL;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_BOOST;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CONSORTIUM;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CREATED_BY;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CREATED_ON;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_DESCRIPTION;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_DISEASE;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_ETAG;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_ID;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_MODIFIED_BY;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_MODIFIED_ON;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_NAME;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_NODE_TYPE;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_NUM_SAMPLES;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_PARENT_ID;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_PLATFORM;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_REFERENCE;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_TISSUE;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_UPDATE_ACL;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.cloudsearchv2.model.IndexField;
import org.sagebionetworks.repo.model.search.query.SearchFieldName;
import org.sagebionetworks.util.ValidateArgument;

public enum SynapseToCloudSearchField {
	ID(SearchFieldName.Id, CLOUD_SEARCH_FIELD_ID),
	NAME(SearchFieldName.Name, CLOUD_SEARCH_FIELD_NAME),
	ENTITY_TYPE(SearchFieldName.EntityType, CLOUD_SEARCH_FIELD_NODE_TYPE),
	MODIFIED_BY(SearchFieldName.ModifiedBy, CLOUD_SEARCH_FIELD_MODIFIED_BY),
	MODIFIED_ON(SearchFieldName.ModifiedOn, CLOUD_SEARCH_FIELD_MODIFIED_ON),
	CREATED_BY(SearchFieldName.CreatedBy, CLOUD_SEARCH_FIELD_CREATED_BY),
	CREATED_ON(SearchFieldName.CreatedOn, CLOUD_SEARCH_FIELD_CREATED_ON),
	DESCRIPTION(SearchFieldName.Description, CLOUD_SEARCH_FIELD_DESCRIPTION),

	//indexes of annotations
	CONSORTIUM(SearchFieldName.ConsortiumAnnotation, CLOUD_SEARCH_FIELD_CONSORTIUM),
	DISEASE(SearchFieldName.DiseaseAnnotation, CLOUD_SEARCH_FIELD_DISEASE),
	NUM_SAMPLES(SearchFieldName.NumSamplesAnnotation, CLOUD_SEARCH_FIELD_NUM_SAMPLES),
	TISSUE(SearchFieldName.TissueAnnotation, CLOUD_SEARCH_FIELD_TISSUE),

	//The ones below are not exposed in our API currently (and probably never will be)
	ETAG(null, CLOUD_SEARCH_FIELD_ETAG),
	BOOST(null, CLOUD_SEARCH_FIELD_BOOST),
	PARENT_ID(null, CLOUD_SEARCH_FIELD_PARENT_ID),
	PLATFORM(null, CLOUD_SEARCH_FIELD_PLATFORM),
	REFERENCE(null, CLOUD_SEARCH_FIELD_REFERENCE),
	ACL(null, CLOUD_SEARCH_FIELD_ACL),
	UPDATE_ACL(null, CLOUD_SEARCH_FIELD_UPDATE_ACL);

	private final SearchFieldName synapseSearchFieldName;
	private final CloudSearchField indexField;

	SynapseToCloudSearchField(SearchFieldName synapseSearchFieldName, CloudSearchField indexField){
		this.synapseSearchFieldName = synapseSearchFieldName;
		this.indexField = indexField;
	}

	public CloudSearchField getIndexField() {
		return indexField;
	}

	/**
	 * Returns the CloudSearchField corresponding ot the SearchFieldName
	 * @param synapseSearchFieldName the SearchFieldName used to find its corresponding CloudSearchField
	 * @return CloudSearchField corresponding to the SearchFieldName or null if no match is found.
	 */
	public static CloudSearchField cloudSearchFieldFor(SearchFieldName synapseSearchFieldName){
		ValidateArgument.required(synapseSearchFieldName, "synapseSearchFieldName");

		for (SynapseToCloudSearchField synapseToCloudSearchField : values()){
			if(synapseSearchFieldName == synapseToCloudSearchField.synapseSearchFieldName){
				return synapseToCloudSearchField.indexField;
			}
		}
		return null;
	}

	/**
	 * Returns a List of all IndexFields needed for initialization of the Cloud Search Domain.
	 * @return a List of all IndexFields needed for initialization of the Cloud Search Domain,
	 */
	public static List<IndexField> loadSearchDomainSchema() {
		List<IndexField> indexFields = new ArrayList<>();
		for(SynapseToCloudSearchField fieldEnum : values()){
			CloudSearchField cloudSearchIndexField = fieldEnum.getIndexField();
			if(cloudSearchIndexField instanceof SynapseCreatedCloudSearchField){
				indexFields.add( ((SynapseCreatedCloudSearchField) cloudSearchIndexField).getIndexField() );
			}
		}
		return indexFields;
	}


}
