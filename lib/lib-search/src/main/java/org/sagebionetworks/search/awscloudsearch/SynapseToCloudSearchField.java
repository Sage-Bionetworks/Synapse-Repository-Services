package org.sagebionetworks.search.awscloudsearch;


import static org.sagebionetworks.search.awscloudsearch.CloudSearchIndexFieldConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.amazonaws.services.cloudsearchv2.model.IndexField;
import org.sagebionetworks.repo.model.search.query.SearchFieldName;

public enum SynapseToCloudSearchField {
	/***
	 * TODO: This class should be the "Truth" on the configuration for search index. The SearchDomainSetupImpl class should be refactored to use this enum to initialize the search index.
	 */
	ID(SearchFieldName.Id, new IdCloudSearchField()), //Id is an implicit field
	NAME(SearchFieldName.Name, new SynapseCreatedCloudSearchField(INDEX_FIELD_NAME)),
	TISSUE(SearchFieldName.TissueAnnotation, new SynapseCreatedCloudSearchField(INDEX_FIELD_TISSUE)),
	ENTITY_TYPE(SearchFieldName.EntityType, new SynapseCreatedCloudSearchField(INDEX_FIELD_NODE_TYPE)),
	DISEASE(SearchFieldName.DiseaseAnnotation, new SynapseCreatedCloudSearchField(INDEX_FIELD_DISEASE)),
	MODIFIED_BY(SearchFieldName.ModifiedBy, new SynapseCreatedCloudSearchField(INDEX_FIELD_MODIFIED_BY)),
	CREATED_BY(SearchFieldName.CreatedBy, new SynapseCreatedCloudSearchField(INDEX_FIELD_CREATED_BY)),
	NUM_SAMPLES(SearchFieldName.NumSamplesAnnotation, new SynapseCreatedCloudSearchField(INDEX_FIELD_NUM_SAMPLES)),
	CREATED_ON(SearchFieldName.CreatedOn, new SynapseCreatedCloudSearchField(INDEX_FIELD_CREATED_ON)),
	MODIFIED_ON(SearchFieldName.ModifiedOn, new SynapseCreatedCloudSearchField(INDEX_FIELD_MODIFIED_ON)),
	DESCRIPTION(SearchFieldName.Description, new SynapseCreatedCloudSearchField(INDEX_FIELD_DESCRIPTION)),
	CONSORTIUM(SearchFieldName.ConsortiumAnnotation, new SynapseCreatedCloudSearchField(INDEX_FIELD_CONSORTIUM)),


	//The ones below are not exposed in our API currently
	ETAG(null, new SynapseCreatedCloudSearchField(INDEX_FIELD_ETAG)),
	BOOST(null, new SynapseCreatedCloudSearchField(INDEX_FIELD_BOOST)),
	PARENT_ID(null, new SynapseCreatedCloudSearchField(INDEX_FIELD_PARENT_ID)),
	PLATFORM(null, new SynapseCreatedCloudSearchField(INDEX_FIELD_PLATFORM)),
	REFERENCE(null, new SynapseCreatedCloudSearchField(INDEX_FIELD_REFERENCE)),
	ACL(null, new SynapseCreatedCloudSearchField(INDEX_FIELD_ACL)),
	UPDATE_ACL(null, new SynapseCreatedCloudSearchField(INDEX_FIELD_UPDATE_ACL));

	private final SearchFieldName synapseSearchFieldName;
	private final CloudSearchField indexField;

	SynapseToCloudSearchField(SearchFieldName synapseSearchFieldName, CloudSearchField indexField){
		this.synapseSearchFieldName = synapseSearchFieldName;
		this.indexField = indexField;
	}

	public CloudSearchField getIndexField() {
		return indexField;
	}

	public static CloudSearchField cloudSearchFieldFor(SearchFieldName synapseSearchFieldName){
		for (SynapseToCloudSearchField synapseToCloudSearchField : values()){
			if(synapseSearchFieldName == synapseToCloudSearchField.synapseSearchFieldName){
				return synapseToCloudSearchField.indexField;
			}
		}
		return null;
	}

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
