package org.sagebionetworks.search;

import static org.sagebionetworks.search.SearchConstants.FIELD_ACL;
import static org.sagebionetworks.search.SearchConstants.FIELD_CONSORTIUM;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_BY;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_DESCRIPTION;
import static org.sagebionetworks.search.SearchConstants.FIELD_DISEASE;
import static org.sagebionetworks.search.SearchConstants.FIELD_ETAG;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_BY;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_NAME;
import static org.sagebionetworks.search.SearchConstants.FIELD_NODE_TYPE;
import static org.sagebionetworks.search.SearchConstants.FIELD_NUM_SAMPLES;
import static org.sagebionetworks.search.SearchConstants.FIELD_PLATFORM;
import static org.sagebionetworks.search.SearchConstants.FIELD_REFERENCE;
import static org.sagebionetworks.search.SearchConstants.FIELD_TISSUE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import com.amazonaws.services.cloudsearchdomain.model.Bucket;
import com.amazonaws.services.cloudsearchdomain.model.BucketInfo;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.QueryParser;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import org.apache.commons.lang.math.NumberUtils;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.search.Facet;
import org.sagebionetworks.repo.model.search.FacetConstraint;
import org.sagebionetworks.repo.model.search.FacetTypeNames;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyRange;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchFacetOption;
import org.sagebionetworks.repo.model.search.query.SearchFacetSort;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.search.awscloudsearch.CloudSearchField;
import org.sagebionetworks.search.awscloudsearch.SynapseToCloudSearchFacetSortType;
import org.sagebionetworks.search.awscloudsearch.SynapseToCloudSearchField;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.util.CollectionUtils;

public class SearchUtil{
	public static final Map<String, FacetTypeNames> FACET_TYPES;

	//regex provided by https://docs.aws.amazon.com/cloudsearch/latest/developerguide/preparing-data.html
	private	static final Pattern UNSUPPORTED_UNICODE_REGEX_PATTERN = Pattern.compile("[^\\u0009\\u000a\\u000d\\u0020-\\uD7FF\\uE000-\\uFFFD]");

	static {
		Map<String, FacetTypeNames> facetTypes = new HashMap<String, FacetTypeNames>();
		facetTypes.put(FIELD_NODE_TYPE, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_DISEASE, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_TISSUE, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_PLATFORM, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_CREATED_BY, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_MODIFIED_BY, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_REFERENCE, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_ACL, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_CREATED_ON, FacetTypeNames.DATE);
		facetTypes.put(FIELD_MODIFIED_ON, FacetTypeNames.DATE);
		facetTypes.put(FIELD_NUM_SAMPLES, FacetTypeNames.CONTINUOUS);
		facetTypes.put(FIELD_CONSORTIUM, FacetTypeNames.LITERAL);
		FACET_TYPES = Collections.unmodifiableMap(facetTypes);
	}

	/**
	 * Returns a String of the input with Unicode characters not supported by the Search Service stripped out.
	 * @param charSequence input to be stripped of unsupported Unicode characters
	 * @return String of the input charSequence with unsupported Unicode characters stripped out
	 */
	static String stripUnsupportedUnicodeCharacters(CharSequence charSequence){
		return UNSUPPORTED_UNICODE_REGEX_PATTERN.matcher(charSequence).replaceAll("");
	}

	public static SearchRequest generateSearchRequest(SearchQuery searchQuery){
		ValidateArgument.required(searchQuery, "searchQuery");

		List<String> q = searchQuery.getQueryTerm();
		List<KeyValue> bq = searchQuery.getBooleanQuery();
		List<KeyRange> rangeQueries =  searchQuery.getRangeQuery();

		SearchRequest searchRequest = new SearchRequest();

		// clean up empty q
		if(q != null && q.size() == 1 && "".equals(q.get(0))) {
			q = null;
		}

		// test for minimum search requirements
		if ((q == null || q.isEmpty()) && (bq == null || bq.isEmpty())) {
			throw new IllegalArgumentException(
					"Either one queryTerm or one booleanQuery must be defined");
		}

		if (q != null && !q.isEmpty()) {
			searchRequest.setQueryParser(QueryParser.Simple);
			searchRequest.setQuery(String.join(" ", q));
		}else{
			searchRequest.setQueryParser(QueryParser.Structured);
			searchRequest.setQuery("matchall"); //if no query is given. default to matching all queries
		}

		List<String> filterQueryTerms = new ArrayList<String>();

		// boolean query into structured query terms
		if (bq != null) {
			for (KeyValue pair : bq) {
				// this regex is pretty lame to have. need to work continuous into KeyValue model
				String value = pair.getValue();

				//add quotes around value. i.e. value -> 'value'
				value = "'" + escapeQuotedValue(pair.getValue()) + "'";
				String term = pair.getKey() + ":" + value;
				if(pair.getNot() != null && pair.getNot()) {
					term = "(not " + term + ")";
				}
				filterQueryTerms.add(term);
			}
		}

		if (rangeQueries != null && !rangeQueries.isEmpty()) {
			filterQueryTerms.addAll(createRangeFilterQueries(rangeQueries));
		}
		searchRequest.setFilterQuery(filterQueryTerms.isEmpty() ? null : "(and " + String.join(" ", filterQueryTerms) + ")");

		// Translate from provided FacetOption to AWS CloudSearch options
		if (!CollectionUtils.isEmpty(searchQuery.getFacetOptions())){
			searchRequest.setFacet(createCloudSearchFacetJSON(searchQuery.getFacetOptions()).toString());
		}

		// return-fields
		if (searchQuery.getReturnFields() != null
				&& searchQuery.getReturnFields().size() > 0)
			searchRequest.setReturn(String.join(",", searchQuery.getReturnFields()));

		// size
		if (searchQuery.getSize() != null)
			searchRequest.setSize(searchQuery.getSize());

		// start
		if (searchQuery.getStart() != null)
			searchRequest.setStart(searchQuery.getStart());

		return searchRequest;
	}

	/**
	 * For each KeyRange in the List, create a String representing its CloudSearch structured query.
	 * The order of the returned List<String> match the order of the input List<Keyrange>
	 *
	 * For example:
	 * given [KeyRange(key=myKey, min=5, max=45)] return ["(range field=myKey [5,45])"]
	 *
	 * @param rangeQueries List of KeyRanges for which the range queries will be constructed
	 * @return List<String> containing the Cloudsearch range structured query for each of the KeyRanges
	 */
	static List<String> createRangeFilterQueries(List<KeyRange> rangeQueries) {
		List<String> filterQueryTerms = new ArrayList<>(rangeQueries.size());

		for (KeyRange keyRange : rangeQueries) {
			String key = keyRange.getKey();
			String min = keyRange.getMin();
			String max = keyRange.getMax();

			if (min == null && max == null) {
				throw new IllegalArgumentException("at least one of min or max for key=" + key + "must be not null");
			}

			StringBuilder rangeStringBuilder = new StringBuilder();
			//left bound
			if (min == null) {
				rangeStringBuilder.append("{");
			} else {
				rangeStringBuilder.append("[" + min);
			}

			//right bound
			rangeStringBuilder.append(",");
			if (max == null) {
				rangeStringBuilder.append("}");
			} else {
				rangeStringBuilder.append(max + "]");
			}

			filterQueryTerms.add("(range field=" + keyRange.getKey() + " " + rangeStringBuilder.toString() + ")");
		}
		return filterQueryTerms;
	}

	/**
	 * Translates a list of SearchFacetOptions into a valid JSON for CloudSearch's SearchRequest facet field.
	 * @param searchFacetOptions list of SearchFacetOption
	 * @return a JSON representation of the SearchFacetOption that is compatible with CloudSearch
	 */
	static JSONObject createCloudSearchFacetJSON(List<SearchFacetOption> searchFacetOptions){
		JSONObject facetJSON = new JSONObject();

		for(SearchFacetOption facetOption : searchFacetOptions){
			addOptionsJSONForFacet(facetJSON, facetOption);
		}
		return facetJSON;
	}

	/**
	 * Helper method to createCloudSearchFacetJSON() which creates the JSON for each individual SearchFacetOption
	 * @param facetOption A facetOption
	 * @return translation of the the facetOption's sortType and maxCount into CloudSearch's facet option JSON.
	 */
	static void addOptionsJSONForFacet(JSONObject facetJSON, SearchFacetOption facetOption) {
		CloudSearchField field = SynapseToCloudSearchField.cloudSearchFieldFor(facetOption.getName());
		if(!field.isFaceted()){
			throw new IllegalArgumentException("The field:\"" + facetOption.getName() +"\" can not be faceted");
		}

		JSONObject facetOptionJSON = new JSONObject();

		SearchFacetSort sortType = facetOption.getSortType();
		Long maxCount = facetOption.getMaxResultCount();

		if(sortType != null) {
			facetOptionJSON.put("sort", SynapseToCloudSearchFacetSortType.getCloudSearchSortTypeFor(sortType).name());
		}

		if(maxCount != null){
			facetOptionJSON.put("size", maxCount);
		}
		facetJSON.putOnce(field.getFieldName() , facetOptionJSON);
	}


	public static SearchResults convertToSynapseSearchResult(SearchResult cloudSearchResult){
		SearchResults synapseSearchResults = new SearchResults();

		//Handle Translating of facets
		List<Facet> facetList = new ArrayList<>();
		Map<String, BucketInfo> facetMap = cloudSearchResult.getFacets();
		if(facetMap != null) {
			for (Map.Entry<String, BucketInfo> facetInfo : facetMap.entrySet()) {//iterate over each facet
				facetList.add(convertToSynapseSearchFacet(facetInfo.getKey(), facetInfo.getValue()));
			}
		}
		synapseSearchResults.setFacets(facetList);

		//Handle translation of Hits
		//class names are clashing feelsbadman
		List<org.sagebionetworks.repo.model.search.Hit> hitList = new ArrayList<>();
		Hits hits = cloudSearchResult.getHits();
		if (hits != null) {
			synapseSearchResults.setFound(hits.getFound());
			synapseSearchResults.setStart(hits.getStart());

			for (com.amazonaws.services.cloudsearchdomain.model.Hit cloudSearchHit : hits.getHit()) {
				hitList.add(convertToSynapseHit(cloudSearchHit));
			}
		}
		synapseSearchResults.setHits(hitList);

		return synapseSearchResults;
	}

	static Facet convertToSynapseSearchFacet(String facetName, BucketInfo bucketInfo) {
		FacetTypeNames facetType = FACET_TYPES.get(facetName);
		if (facetType == null) {
			throw new IllegalArgumentException(
					"facet "
							+ facetName
							+ " is not properly configured, add it to the facet type map");
		}

		Facet synapseFacet = new Facet();
		synapseFacet.setName(facetName);
		synapseFacet.setType(facetType);
		// Note: min and max are never set since the frontend never makes use of them and so the results won't ever have them.
		// A IllegalArgumentException would have been throw when converting from Synapse's SearchQuery to Amazon's SearchRequest

		List<FacetConstraint> facetConstraints = new ArrayList<>();
		for (Bucket bucket: bucketInfo.getBuckets()){
			FacetConstraint facetConstraint = new FacetConstraint();
			facetConstraint.setValue(bucket.getValue());
			facetConstraint.setCount(bucket.getCount());
			facetConstraints.add(facetConstraint);
		}
		synapseFacet.setConstraints(facetConstraints);
		return synapseFacet;
	}

	static org.sagebionetworks.repo.model.search.Hit convertToSynapseHit(com.amazonaws.services.cloudsearchdomain.model.Hit cloudSearchHit){
		Map<String, List<String>> fieldsMap = cloudSearchHit.getFields();

		org.sagebionetworks.repo.model.search.Hit synapseHit = new org.sagebionetworks.repo.model.search.Hit();
		synapseHit.setCreated_by(getFirstListValueFromMap(fieldsMap, FIELD_CREATED_BY));
		synapseHit.setCreated_on(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, FIELD_CREATED_ON)));
		synapseHit.setDescription(getFirstListValueFromMap(fieldsMap, FIELD_DESCRIPTION));
		synapseHit.setDisease(getFirstListValueFromMap(fieldsMap, FIELD_DISEASE));
		synapseHit.setEtag(getFirstListValueFromMap(fieldsMap, FIELD_ETAG));
		synapseHit.setModified_by(getFirstListValueFromMap(fieldsMap, FIELD_MODIFIED_BY));
		synapseHit.setModified_on(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, FIELD_MODIFIED_ON)));
		synapseHit.setName(getFirstListValueFromMap(fieldsMap, FIELD_NAME));
		synapseHit.setNode_type(getFirstListValueFromMap(fieldsMap, FIELD_NODE_TYPE));
		synapseHit.setNum_samples(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, FIELD_NUM_SAMPLES)));
		synapseHit.setTissue(getFirstListValueFromMap(fieldsMap, FIELD_TISSUE));
		synapseHit.setConsortium(getFirstListValueFromMap(fieldsMap, FIELD_CONSORTIUM));
		//synapseHit.setPath() also exists but there does not appear to be a path field in the cloudsearch anymore.
		synapseHit.setId(cloudSearchHit.getId());
		return synapseHit;
	}


	static String getFirstListValueFromMap(Map<String, List<String>> map, String key){
		List<String> list = map.get(key);
		return (list == null || list.isEmpty()) ? null : list.get(0);
	}

	/*
	 * Private Methods
	 */

	private static String escapeQuotedValue(String value) {
		value = value.replaceAll("\\\\", "\\\\\\\\"); // replace \ -> \\
		value = value.replaceAll("'", "\\\\'"); // replace ' -> \'
		return value;
	}


	/**
	 * Formulate the boolean query to enforce an access controll list for a
	 * particular user
	 *
	 * @param userInfo
	 * @return the acl boolean query
	 * @throws DatastoreException
	 */
	public static String formulateAuthorizationFilter(UserInfo userInfo)
			throws DatastoreException {
		ValidateArgument.required(userInfo, "userInfo");
		Set<Long> userGroups = userInfo.getGroups();
		ValidateArgument.required(userGroups, "userInfo.getGroups()");
		ValidateArgument.requirement(!userGroups.isEmpty(), "no groups for user " + userInfo);

		// Make our boolean query
		StringJoiner authorizationFilterJoiner = new StringJoiner(" ","(or ", ")");
		for (Long group : userGroups) {
			authorizationFilterJoiner.add(FIELD_ACL + ":'" + group + "'");
		}
		return authorizationFilterJoiner.toString();
	}

	public static void addAuthorizationFilter(SearchRequest searchRequest, UserInfo userInfo){
		String authFilter = formulateAuthorizationFilter(userInfo);
		String existingFitler = searchRequest.getFilterQuery();
		if(existingFitler != null){
			searchRequest.setFilterQuery("(and " + authFilter + " " + existingFitler + ")");
		}else{
			searchRequest.setFilterQuery(authFilter);
		}
	}

	/**
	 * Remove any character that is not compatible with cloud search.
	 * @param documents
	 * @return JSON String of cleaned document
	 */
	static String convertSearchDocumentsToJSONString(List<Document> documents) {
		ValidateArgument.required(documents, "documents");
		// CloudSearch will not accept an empty document batch
		ValidateArgument.requirement(!documents.isEmpty(), "documents can not be empty");

		StringJoiner stringJoiner = new StringJoiner(", ", "[", "]");

		for (Document document : documents) {
			if (DocumentTypeNames.add == document.getType()) {
				prepareDocument(document);
			}
			try {
				stringJoiner.add(EntityFactory.createJSONStringForEntity(document));
			} catch (JSONObjectAdapterException e) {
				throw new RuntimeException(e);
			}
		}
		// Some descriptions have control characters in them for some reason, in any case, just get rid
		// of all control characters in the search document
		return stripUnsupportedUnicodeCharacters(stringJoiner.toString());
	}

	/**
	 * Prepare the document to be sent.
	 * @param document
	 */
	static void prepareDocument(Document document) {
		if(document.getFields() == null){
			document.setFields(new DocumentFields());
		}
	}
}
