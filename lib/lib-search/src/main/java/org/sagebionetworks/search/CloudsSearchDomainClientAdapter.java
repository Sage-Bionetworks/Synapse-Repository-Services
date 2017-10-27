package org.sagebionetworks.search;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import com.amazonaws.services.cloudsearchdomain.model.Bucket;
import com.amazonaws.services.cloudsearchdomain.model.BucketInfo;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsResult;
import org.sagebionetworks.repo.model.search.AwesomeSearchFactory;
import org.sagebionetworks.repo.model.search.Facet;
import org.sagebionetworks.repo.model.search.FacetConstraint;
import org.sagebionetworks.repo.model.search.FacetTypeNames;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.apache.commons.lang.math.NumberUtils; //TODO: are there different version of apache commons for different parts of the codebase? I can't use the lang3 library here but I can in SearchServiceImpl

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CloudsSearchDomainClientAdapter {
	private boolean initialized = false; //TODO: use autowire? or use the client builder? decide later?
	private AmazonCloudSearchDomainClient client;


	public CloudsSearchDomainClientAdapter(){//TODO: maybe need to change constructor?
		this.initialized = false;
	}

	public void sendDocuments(String documents){
		byte[] documentBytes = documents.getBytes();
		UploadDocumentsRequest request = new UploadDocumentsRequest()
										.withContentType("application/json")
										.withDocuments(new ByteArrayInputStream(documentBytes))
										.withContentLength((long) documentBytes.length);
		UploadDocumentsResult result = client.uploadDocuments(request);
	}

	public void setEndpoint(String endpoint){
		client.setEndpoint(endpoint);
		initialized = true;
	}

	public boolean isInitialized(){
		return initialized;
	}

	public SearchResults search(SearchRequest request){
		return convertToSynapseSearchResult(client.search(request));
	}

	public SearchResults convertToSynapseSearchResult(SearchResult cloudSearchResult){
		SearchResults synapseSearchResults = new SearchResults();

		//Handle Translating of facets
		Map<String, BucketInfo> facetMap = cloudSearchResult.getFacets();
		if(facetMap != null) {
			List<Facet> facetList = new ArrayList<>();

			for (Map.Entry<String, BucketInfo> facetInfo : facetMap.entrySet()) {//iterate over each facet

				String facetName = facetInfo.getKey();
				//TODO: REFACTOR AwesomeSearchFactory
				FacetTypeNames facetType = AwesomeSearchFactory.FACET_TYPES.get(facetName);
				if (facetType == null) {
					throw new IllegalArgumentException(
							"facet "
									+ facetName
									+ " is not properly configured, add it to the facet type map");
				}

				Facet synapseFacet = new Facet();
				synapseFacet.setName(facetName);
				synapseFacet.setType(facetType);
				//Note: min and max are never set since the frontend never makes use of them and so the results won't ever have them.

				BucketInfo bucketInfo = facetInfo.getValue();
				List<FacetConstraint> facetConstraints = new ArrayList<>();
				for (Bucket bucket: bucketInfo.getBuckets()){
					FacetConstraint facetConstraint = new FacetConstraint();
					facetConstraint.setValue(bucket.getValue());
					facetConstraint.setCount(bucket.getCount());
				}
				synapseFacet.setConstraints(facetConstraints);

				facetList.add(synapseFacet);
			}
			synapseSearchResults.setFacets(facetList);
		}

		Hits hits = cloudSearchResult.getHits();

		synapseSearchResults.setFound(hits.getFound());
		synapseSearchResults.setStart(hits.getStart());

		//class names clashing feelsbadman
		List<org.sagebionetworks.repo.model.search.Hit> hitList = new ArrayList<>();
		for(com.amazonaws.services.cloudsearchdomain.model.Hit cloudSearchHit : hits.getHit()){
			org.sagebionetworks.repo.model.search.Hit synapseHit = new org.sagebionetworks.repo.model.search.Hit();
			Map<String, List<String>> fieldsMap = cloudSearchHit.getFields();
			//TODO: test to make sure the values are correct

			synapseHit.setCreated_by(getFirstListValueFromMap(fieldsMap, "created_by"));
			synapseHit.setCreated_on(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, "created_on")));
			synapseHit.setDescription(getFirstListValueFromMap(fieldsMap, "description"));
			synapseHit.setDisease(getFirstListValueFromMap(fieldsMap, "disease"));
			synapseHit.setEtag(getFirstListValueFromMap(fieldsMap, "etag"));
			synapseHit.setId(getFirstListValueFromMap(fieldsMap, "id"));
			synapseHit.setModified_by(getFirstListValueFromMap(fieldsMap, "modified_by"));
			synapseHit.setModified_on(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, "modified_on")));
			synapseHit.setName(getFirstListValueFromMap(fieldsMap, "name"));
			synapseHit.setNode_type(getFirstListValueFromMap(fieldsMap, "node_type"));
			synapseHit.setNum_samples(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, "num_samples")));
			synapseHit.setTissue(getFirstListValueFromMap(fieldsMap, "tissue"));
			//synapseHit.setPath() also exists but there does not appear to be a path field in the cloudsearch anymore.

			hitList.add(synapseHit);
		}
		synapseSearchResults.setHits(hitList);
		return synapseSearchResults;
	}


	private String getFirstListValueFromMap(Map<String, List<String>> map, String key){
		//TODO: are we on Java 8 yet? switch to lambda function?
		List<String> value = map.get(key);
		return value == null || value.isEmpty() ? null : value.get(0);
	}
}
