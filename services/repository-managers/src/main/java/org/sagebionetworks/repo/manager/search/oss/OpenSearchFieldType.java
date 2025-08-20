package org.sagebionetworks.repo.manager.search.oss;

import org.sagebionetworks.repo.model.search.FacetTypeNames;
import org.sagebionetworks.repo.model.search.query.SearchFieldName;
import org.sagebionetworks.util.ValidateArgument;

public enum OpenSearchFieldType {

    CONSORTIUM(SearchFieldName.Consortium, FacetTypeNames.LITERAL),
    DIAGNOSIS(SearchFieldName.Diagnosis, FacetTypeNames.LITERAL),
    ORGAN(SearchFieldName.Organ, FacetTypeNames.LITERAL),
    TISSUE(SearchFieldName.Tissue, FacetTypeNames.LITERAL),
    ID(SearchFieldName.Id, FacetTypeNames.LITERAL),
    ENTITY_TYPE(SearchFieldName.EntityType, FacetTypeNames.LITERAL),
    MODIFIED_BY(SearchFieldName.ModifiedBy, FacetTypeNames.LITERAL),
    MODIFIED_ON(SearchFieldName.ModifiedOn, FacetTypeNames.CONTINUOUS),
    CREATED_BY(SearchFieldName.CreatedBy, FacetTypeNames.LITERAL),
    CREATED_ON(SearchFieldName.CreatedOn, FacetTypeNames.CONTINUOUS);

    private final SearchFieldName searchFieldName;
    private final FacetTypeNames facetType;

    OpenSearchFieldType(SearchFieldName searchFieldName, FacetTypeNames facetType) {
        this.searchFieldName = searchFieldName;
        this.facetType = facetType;
    }

    public static FacetTypeNames fieldType(String synapseSearchFieldName) {
        ValidateArgument.required(synapseSearchFieldName, "synapseSearchFieldName");

        for (OpenSearchFieldType type : values()) {
            if (synapseSearchFieldName.equals(type.searchFieldName.name())) {
                return type.facetType;
            }
        }
        throw new IllegalArgumentException("OpenSearchFieldType is not supported for " + synapseSearchFieldName);
    }
}