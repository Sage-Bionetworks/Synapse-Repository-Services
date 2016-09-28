package org.sagebionetworks.repo.model.search;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.sagebionetworks.schema.adapter.AdapterFactory;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Factory methods to convert AWESOMESEARCH <-> Synapse entities
 * 
 * @author deflaux
 * 
 */
public class AwesomeSearchFactory {

	/**
	 * Literal fields that are facets cannot also be returned in search results,
	 * therefore we add a copy to the search index and add this suffix to that
	 * field
	 */
	public static final String RESULT_FIELD_SUFFIX = "_r";

	/**
	 * The types of all facets one might ask for in search results from the
	 * repository service.
	 */
	public static final Map<String, FacetTypeNames> FACET_TYPES;

	private static final String PATH_FIELD = "path";

	AdapterFactory factory = null;

	static {
		Map<String, FacetTypeNames> facetTypes = new HashMap<String, FacetTypeNames>();
		facetTypes.put("node_type", FacetTypeNames.LITERAL);
		facetTypes.put("disease", FacetTypeNames.LITERAL);
		facetTypes.put("tissue", FacetTypeNames.LITERAL);
		facetTypes.put("species", FacetTypeNames.LITERAL);
		facetTypes.put("platform", FacetTypeNames.LITERAL);
		facetTypes.put("created_by", FacetTypeNames.LITERAL);
		facetTypes.put("modified_by", FacetTypeNames.LITERAL);
		facetTypes.put("reference", FacetTypeNames.LITERAL);
		facetTypes.put("acl", FacetTypeNames.LITERAL);
		facetTypes.put("created_on", FacetTypeNames.DATE);
		facetTypes.put("modified_on", FacetTypeNames.DATE);
		facetTypes.put("num_samples", FacetTypeNames.CONTINUOUS);
		FACET_TYPES = Collections.unmodifiableMap(facetTypes);
	}

	/**
	 * Create a new search factory using an AdapterFactory.
	 * 
	 * @param factory
	 */
	public AwesomeSearchFactory(AdapterFactory factory) {
		this.factory = factory;
	}

	/**
	 * Convert from AwesomeSearch serialized JSON to the SearchResults
	 * JSONEntity
	 * 
	 * TODO later when Schema2Pojo supports maps, make a JSONEntity for the
	 * AwesomeSearchResults object
	 * 
	 * @param awesomeSearchResponse
	 * @return the SearchResults JSONEntity
	 * @throws JSONObjectAdapterException
	 */
	public SearchResults fromAwesomeSearchResults(String awesomeSearchResponse) throws JSONObjectAdapterException {

		// First let the factory parse the JSON for you.
		// The adapter works much like a JSONObject
		JSONObjectAdapter awesomeSearchResults = factory
				.createNew(awesomeSearchResponse);

		// This is where were will put the results
		JSONObjectAdapter searchResults = factory.createNew();
		JSONArrayAdapter hits = factory.createNewArray();
		searchResults.put("hits", hits);
		JSONArrayAdapter facets = factory.createNewArray();
		searchResults.put("facets", facets);

		// Now do the translation
		searchResults.put("found", awesomeSearchResults
				.getJSONObject("hits").getLong("found"));
		searchResults.put("start", awesomeSearchResults
				.getJSONObject("hits").getLong("start"));

		JSONArrayAdapter awesomeSearchHits = awesomeSearchResults
				.getJSONObject("hits").getJSONArray("hit");
		for (int i = 0; i < awesomeSearchHits.length(); i++) {
			JSONObjectAdapter hit = factory.createNew();
			JSONObjectAdapter awesomeSearchHit = awesomeSearchHits
			.getJSONObject(i);
			hit.put("id", awesomeSearchHit.getString("id"));

			// Copy over results fields, if they were requested and therefore
			// present in the AwesomeSearch response
			if (awesomeSearchHits.getJSONObject(i).has("fields")) {
				JSONObjectAdapter awesomeSearchHitData = awesomeSearchHit.getJSONObject("fields");
				Iterator<String> dataNames = awesomeSearchHitData.keys();
				while (dataNames.hasNext()) {
					String dataName = dataNames.next();
					String dataValue = awesomeSearchHitData.getString(dataName);
					if(dataName.endsWith(RESULT_FIELD_SUFFIX)) {
						dataName = dataName.substring(0, dataName.length() - RESULT_FIELD_SUFFIX.length());
					}
					if(dataName.equals(PATH_FIELD)) {					
						hit.put(dataName, factory.createNew(dataValue));
					} else {
						hit.put(dataName, dataValue);
					}
				}
			}
			hits.put(hits.length(), hit);
		}

		if (awesomeSearchResults.has("facets")) {

			// Copy over facets, if they were requested and therefore present in
			// the AwesomeSearch response
			JSONObjectAdapter awesomeSearchFacets = awesomeSearchResults
					.getJSONObject("facets");

			Iterator<String> facetNames = awesomeSearchFacets.keys();
			while (facetNames.hasNext()) {
				JSONObjectAdapter facet = factory.createNew();
				String facetName = facetNames.next();
				JSONObjectAdapter awesomeSearchFacet = awesomeSearchFacets
						.getJSONObject(facetName);
				FacetTypeNames facetType = FACET_TYPES.get(facetName);
				if (null == facetType) {
					throw new IllegalArgumentException(
							"facet "
									+ facetName
									+ " is not properly configured, add it to the facet type map");
				}

				facet.put("name", facetName);
				facet.put("type", facetType.name());
				facets.put(facets.length(), facet);

				if (FacetTypeNames.DATE == facetType
						|| FacetTypeNames.CONTINUOUS == facetType) {
					// Dev Note: don't do optLong here because zero for a min
					// and
					// max might not make sense for most facets
					if (awesomeSearchFacet.has("min")
							&& awesomeSearchFacet.has("max")) {
						facet.put("min", awesomeSearchFacet
								.getLong("min"));
						facet.put("max", awesomeSearchFacet
								.getLong("max"));
					}
				}

				if (!awesomeSearchFacet.has("buckets"))
					continue;
				
				JSONArrayAdapter awesomeSearchConstraints = awesomeSearchFacet
						.getJSONArray("buckets"); //in 2013 search api, constraints are called buckets

				JSONArrayAdapter constraints = factory.createNewArray();
				facet.put("constraints", constraints);
				
				for (int i = 0; i < awesomeSearchConstraints.length(); i++) {
					JSONObjectAdapter awesomeSearchConstraint = awesomeSearchConstraints
							.getJSONObject(i);
					JSONObjectAdapter constraint = factory.createNew();
					constraint.put("value", awesomeSearchConstraint
							.getString("value"));
					constraint.put("count", awesomeSearchConstraint
							.getLong("count"));
					constraints.put(constraints.length(), constraint);
				}
			}
		}
		
		// Use the result to create the actual results
		SearchResults results = new SearchResults();
		results.initializeFromJSONObject(searchResults);
		return results;

	}
}
