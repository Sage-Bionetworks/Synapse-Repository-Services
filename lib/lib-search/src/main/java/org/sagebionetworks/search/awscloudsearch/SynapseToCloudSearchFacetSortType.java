package org.sagebionetworks.search.awscloudsearch;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.sagebionetworks.repo.model.search.query.SearchFacetSort;

/**
 * An enum representing the types of facet result sorting available in CloudSearch.
 * Additionally provides a mapping from Synapse's facet result sorting into CloudSearch's facet result sorting
 */
public enum SynapseToCloudSearchFacetSortType {
	BUCKET(SearchFacetSort.ALPHA),
	COUNT(SearchFacetSort.COUNT);

	private SearchFacetSort synapseSearchSortType;

	private static final Map<SearchFacetSort, SynapseToCloudSearchFacetSortType> SYNAPSE_SORT_TO_CLOUDSEARCH_SORT;
	static{ //initialize SYNAPSE_SORT_TO_CLOUDSEARCH_SORT
		Map<SearchFacetSort, SynapseToCloudSearchFacetSortType> tempMap = new EnumMap<>(SearchFacetSort.class);
		for(SynapseToCloudSearchFacetSortType cloudSearchSortType : SynapseToCloudSearchFacetSortType.values()){
			tempMap.put(cloudSearchSortType.synapseSearchSortType, cloudSearchSortType);
		}
		SYNAPSE_SORT_TO_CLOUDSEARCH_SORT = Collections.unmodifiableMap(tempMap);
	}

	SynapseToCloudSearchFacetSortType(SearchFacetSort synapseSortType){
		this.synapseSearchSortType = synapseSortType;
	}

	public static SynapseToCloudSearchFacetSortType getCloudSearchSortTypeFor(SearchFacetSort synapseSearchSortType){
		return SYNAPSE_SORT_TO_CLOUDSEARCH_SORT.get(synapseSearchSortType);
	}
}
