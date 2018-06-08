package org.sagebionetworks.search;

import org.sagebionetworks.repo.model.search.query.SearchFacetSort;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * An enum representing the types of facet result sorting available in CloudSearch.
 * Additionally provides a mapping from Synapse's facet result sorting into CloudSearch's facet result sorting
 */
public enum CloudSearchFacetSortType {
	BUCKET(SearchFacetSort.ALPHA),
	COUNT(SearchFacetSort.COUNT);

	private SearchFacetSort synapseSearchSortType;

	private static final Map<SearchFacetSort, CloudSearchFacetSortType> SYNAPSE_SORT_TO_CLOUDSEARCH_SORT;
	static{ //initialize SYNAPSE_SORT_TO_CLOUDSEARCH_SORT
		Map<SearchFacetSort, CloudSearchFacetSortType> tempMap = new EnumMap<>(SearchFacetSort.class);
		for(CloudSearchFacetSortType cloudSearchSortType : CloudSearchFacetSortType.values()){
			tempMap.put(cloudSearchSortType.synapseSearchSortType, cloudSearchSortType);
		}
		SYNAPSE_SORT_TO_CLOUDSEARCH_SORT = Collections.unmodifiableMap(tempMap);
	}

	CloudSearchFacetSortType(SearchFacetSort synapseSortType){
		this.synapseSearchSortType = synapseSortType;
	}

	public static CloudSearchFacetSortType getCloudSearchSortTypeFor(SearchFacetSort synapseSearchSortType){
		return SYNAPSE_SORT_TO_CLOUDSEARCH_SORT.get(synapseSearchSortType);
	}
}
