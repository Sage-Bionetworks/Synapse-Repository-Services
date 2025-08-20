package org.sagebionetworks.repo.manager.search.oss;

import org.sagebionetworks.repo.model.search.query.SearchFieldName;
import org.sagebionetworks.search.SearchConstants;
import org.sagebionetworks.util.ValidateArgument;

public enum SynapseToOpenSearchAggregationField {
    CONSORTIUM(SearchFieldName.Consortium, SearchConstants.FIELD_CONSORTIUM),
    DIAGNOSIS(SearchFieldName.Diagnosis, SearchConstants.FIELD_DIAGNOSIS),
    ORGAN(SearchFieldName.Organ, SearchConstants.FIELD_ORGAN),
    TISSUE(SearchFieldName.Tissue, SearchConstants.FIELD_TISSUE),

    ID(SearchFieldName.Id, SearchConstants.FIELD_ID),
    ENTITY_TYPE(SearchFieldName.EntityType, SearchConstants.FIELD_NODE_TYPE),
    MODIFIED_BY(SearchFieldName.ModifiedBy, SearchConstants.FIELD_MODIFIED_BY),
    MODIFIED_ON(SearchFieldName.ModifiedOn, SearchConstants.FIELD_MODIFIED_ON),
    CREATED_BY(SearchFieldName.CreatedBy, SearchConstants.FIELD_CREATED_BY),
    CREATED_ON(SearchFieldName.CreatedOn, SearchConstants.FIELD_CREATED_ON);

    //In Opensearch, text fields can not be used in aggregations and sorting, so this enum does not include Name and Description.

    private final SearchFieldName synapseSearchField;
    private final String name;

    SynapseToOpenSearchAggregationField(SearchFieldName synapseSearchField, String name) {
        this.synapseSearchField = synapseSearchField;
        this.name = name;
    }

    public static String openSearchFieldFor(SearchFieldName synapseSearchFieldName) {
        ValidateArgument.required(synapseSearchFieldName, "synapseSearchFieldName");

        for (SynapseToOpenSearchAggregationField synapseToOpenSearchAggregationField : values()) {
            if(synapseSearchFieldName == synapseToOpenSearchAggregationField.synapseSearchField){
                return synapseToOpenSearchAggregationField.name;
            }
        }
        throw new IllegalArgumentException("OpenSearch Aggregation is not supported for "+ synapseSearchFieldName);
    }

    public static String synapseFieldFor(String searchConstant) {
        ValidateArgument.required(searchConstant, "searchConstant");

        for (SynapseToOpenSearchAggregationField synapseToOpenSearchAggregationField : values()) {
            if(searchConstant.equals(synapseToOpenSearchAggregationField.name)){
                return synapseToOpenSearchAggregationField.synapseSearchField.name();
            }
        }

        throw new IllegalArgumentException("Synapse field is not supported for search Constant " + searchConstant);
    }
}
