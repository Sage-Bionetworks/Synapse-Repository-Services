package org.sagebionetworks.search;

import org.sagebionetworks.repo.model.search.FacetTypeNames;

import com.amazonaws.services.cloudsearchv2.model.IndexFieldType;

public enum IndexFieldToSynapseFacetType {
	LITERAL (IndexFieldType.Literal, FacetTypeNames.LITERAL),
	DATE (IndexFieldType.Date, FacetTypeNames.DATE),
	CONTINUOUS(IndexFieldType.Int, FacetTypeNames.CONTINUOUS);


	private IndexFieldType indexFieldType;
	private FacetTypeNames facetTypeName;

	private IndexFieldToSynapseFacetType(IndexFieldType indexFieldType, FacetTypeNames facetTypeName){
		this.indexFieldType = indexFieldType;
		this.facetTypeName = facetTypeName;
	}

	public static FacetTypeNames getSynapseFacetType(IndexFieldType indexFieldType){
		for(IndexFieldToSynapseFacetType entry : values()){
			if(indexFieldType == entry.indexFieldType){
				return entry.facetTypeName;
			}
		}
		throw new IllegalArgumentException("Unknown indexFieldType " + indexFieldType.name());
	}
}
