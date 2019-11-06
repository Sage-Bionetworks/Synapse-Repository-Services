package org.sagebionetworks.search.awscloudsearch;

import org.sagebionetworks.repo.model.search.query.SearchFacetSort;

/**
 * An enum representing the types of facet result sorting available in CloudSearch.
 * Additionally provides a mapping from Synapse's facet result sorting into CloudSearch's facet result sorting
 */
public enum SynapseToCloudSearchFacetSortType {
	bucket(SearchFacetSort.ALPHA),
	count(SearchFacetSort.COUNT);

	private SearchFacetSort synapseSearchSortType;

	SynapseToCloudSearchFacetSortType(SearchFacetSort synapseSortType){
		this.synapseSearchSortType = synapseSortType;
	}

	public static SynapseToCloudSearchFacetSortType getCloudSearchSortTypeFor(SearchFacetSort synapseSearchSortType) {
		for (SynapseToCloudSearchFacetSortType cloudSearchSortType : SynapseToCloudSearchFacetSortType.values()) {
			if( synapseSearchSortType == cloudSearchSortType.synapseSearchSortType){
				return cloudSearchSortType;
			}
		}
		throw new IllegalArgumentException("unknown SearchFacetSort");
	}
}
