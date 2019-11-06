package org.sagebionetworks.search.awscloudsearch;


import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_ACL;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CONSORTIUM;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CREATED_BY;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_CREATED_ON;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_DESCRIPTION;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_DIAGNOSIS;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_ETAG;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_ID;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_MODIFIED_BY;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_MODIFIED_ON;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_NAME;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_NODE_TYPE;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_ORGAN;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_PARENT_ID;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_REFERENCE;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_TISSUE;
import static org.sagebionetworks.search.awscloudsearch.CloudSearchFieldConstants.CLOUD_SEARCH_FIELD_UPDATE_ACL;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.search.query.SearchFieldName;
import org.sagebionetworks.util.ValidateArgument;

import com.amazonaws.services.cloudsearchv2.model.IndexField;

/**
 * This class tracks all of the fields used in CloudSearch and provides mapping from fields listed in Synapse's API
 * to their actual CloudSearch fields.
 */
public enum SynapseToCloudSearchField {
	//indexes of annotations
	CONSORTIUM(SearchFieldName.Consortium, CLOUD_SEARCH_FIELD_CONSORTIUM),
	DIAGNOSIS(SearchFieldName.Diagnosis, CLOUD_SEARCH_FIELD_DIAGNOSIS),
	ORGAN(SearchFieldName.Organ, CLOUD_SEARCH_FIELD_ORGAN),
	TISSUE(SearchFieldName.Tissue, CLOUD_SEARCH_FIELD_TISSUE),

	ID(SearchFieldName.Id, CLOUD_SEARCH_FIELD_ID),
	NAME(SearchFieldName.Name, CLOUD_SEARCH_FIELD_NAME),
	ENTITY_TYPE(SearchFieldName.EntityType, CLOUD_SEARCH_FIELD_NODE_TYPE),
	MODIFIED_BY(SearchFieldName.ModifiedBy, CLOUD_SEARCH_FIELD_MODIFIED_BY),
	MODIFIED_ON(SearchFieldName.ModifiedOn, CLOUD_SEARCH_FIELD_MODIFIED_ON),
	CREATED_BY(SearchFieldName.CreatedBy, CLOUD_SEARCH_FIELD_CREATED_BY),
	CREATED_ON(SearchFieldName.CreatedOn, CLOUD_SEARCH_FIELD_CREATED_ON),
	DESCRIPTION(SearchFieldName.Description, CLOUD_SEARCH_FIELD_DESCRIPTION),

	//The ones below are not exposed in our API currently (and probably never will be)
	ETAG(null, CLOUD_SEARCH_FIELD_ETAG),
	PARENT_ID(null, CLOUD_SEARCH_FIELD_PARENT_ID),
	REFERENCE(null, CLOUD_SEARCH_FIELD_REFERENCE),
	ACL(null, CLOUD_SEARCH_FIELD_ACL),
	UPDATE_ACL(null, CLOUD_SEARCH_FIELD_UPDATE_ACL);

	private final SearchFieldName synapseSearchFieldName;
	private final CloudSearchField cloudSearchField;

	SynapseToCloudSearchField(SearchFieldName synapseSearchFieldName, CloudSearchField cloudSearchField){
		this.synapseSearchFieldName = synapseSearchFieldName;
		this.cloudSearchField = cloudSearchField;
	}

	/**
	 * Returns the CloudSearchField corresponding to the SearchFieldName
	 * @param synapseSearchFieldName the SearchFieldName used to find its corresponding CloudSearchField
	 * @return CloudSearchField corresponding to the SearchFieldName or null if no match is found.
	 */
	public static CloudSearchField cloudSearchFieldFor(SearchFieldName synapseSearchFieldName){
		ValidateArgument.required(synapseSearchFieldName, "synapseSearchFieldName");

		for (SynapseToCloudSearchField synapseToCloudSearchField : values()){
			if(synapseSearchFieldName == synapseToCloudSearchField.synapseSearchFieldName){
				return synapseToCloudSearchField.cloudSearchField;
			}
		}
		throw new IllegalArgumentException("Unknown SearchField");
	}

	public static CloudSearchField cloudSearchFieldFor(String cloudSearchFieldName){
		ValidateArgument.required(cloudSearchFieldName, "cloudSearchFieldName");

		for (SynapseToCloudSearchField synapseToCloudSearchField : values()){
			if(cloudSearchFieldName.equals(synapseToCloudSearchField.cloudSearchField.getFieldName())){
				return synapseToCloudSearchField.cloudSearchField;
			}
		}
		throw new IllegalArgumentException("Unknown SearchField");
	}

	/**
	 * Returns a List of all IndexFields needed for initialization of the Cloud Search Domain.
	 * @return a List of all IndexFields needed for initialization of the Cloud Search Domain,
	 */
	public static List<IndexField> loadSearchDomainSchema() {
		List<IndexField> indexFields = new ArrayList<>();
		for(SynapseToCloudSearchField fieldEnum : values()){
			CloudSearchField cloudSearchIndexField = fieldEnum.cloudSearchField;
			if(cloudSearchIndexField instanceof SynapseCreatedCloudSearchField){
				indexFields.add( ((SynapseCreatedCloudSearchField) cloudSearchIndexField).getIndexField() );
			}
		}
		return indexFields;
	}




}
